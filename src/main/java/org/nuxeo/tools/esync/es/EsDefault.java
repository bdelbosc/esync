/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.tools.esync.es;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

import org.nuxeo.tools.esync.config.ESyncConfig;
import org.nuxeo.tools.esync.db.Document;

@Singleton
public class EsDefault implements Es {
    private static final Logger log = LoggerFactory.getLogger(EsDefault.class);

    private static final String DOC_TYPE = "doc";

    private static final String ACL_FIELD = "ecm:acl";

    private static final String PATH_FIELD = "ecm:path";

    private static final String CHILDREN_FIELD = "ecm:path.children";

    private ESyncConfig config;

    // client is thread safe and shared between checkers
    private static TransportClient client;

    private final static AtomicInteger clients = new AtomicInteger(0);

    private final static MetricRegistry registry = SharedMetricRegistries.getOrCreate("main");

    private final static Timer aclTimer = registry.timer("esync.es.acl");

    private final static Timer cardinalityTimer = registry.timer("esync.es.cardinality");

    private final static Timer typeCardinalityTimer = registry.timer("esync.es.type.cardinality");

    private final Timer documentIdsForTypeTimed = registry.timer("esync.es.type.documentIdsForType");

    @Override
    public void initialize(ESyncConfig config) {
        this.config = config;
        open();
    }

    private void open() {
        synchronized (EsDefault.class) {
            if (clients.incrementAndGet() == 1) {
                log.debug("Connecting to ES cluster");
                ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder().put("cluster.name",
                        config.clusterName()).put("client.transport.sniff", false);
                Settings settings = builder.build();
                log.debug("Using settings: " + settings.toDelimitedString(','));
                TransportClient tClient = new TransportClient(settings);
                String[] addresses = config.addressList().split(",");
                for (String item : addresses) {
                    String[] address = item.split(":");
                    log.debug("Add transport address: " + item);
                    try {
                        InetAddress inet = InetAddress.getByName(address[0]);
                        tClient.addTransportAddress(new InetSocketTransportAddress(inet, Integer.parseInt(address[1])));
                    } catch (UnknownHostException e) {
                        log.error("Unable to resolve host " + address[0], e);
                    }
                }
                client = tClient;
            }
        }
    }

    @Override
    public void close() {
        if (clients.decrementAndGet() == 0) {
            if (client != null) {
                log.debug("Closing es connection");
                client.close();
                client = null;
            }
        }
    }

    @Override
    public Document getDocument(String id) throws NoSuchElementException {
        GetRequestBuilder getRequest = getClient().prepareGet(config.esIndex(), DOC_TYPE, id).setFields(ACL_FIELD,
                PATH_FIELD);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Get path of doc: curl -XGET 'http://localhost:9200/%s/%s/%s?fields=%s'",
                    config.esIndex(), DOC_TYPE, id, ACL_FIELD));
        }
        GetResponse ret = getRequest.execute().actionGet();
        if (!ret.isExists()) {
            throw new NoSuchElementException(id + " not found in ES");
        }
        Set<String> acl;
        try {
            Object[] aclArray = ret.getField(ACL_FIELD).getValues().toArray();
            String[] aclStringArray = Arrays.copyOf(aclArray, aclArray.length, String[].class);
            acl = new HashSet<>(Arrays.asList(aclStringArray));
        } catch (NullPointerException e) {
            acl = Document.NO_ACL;
        }
        String path = ret.getField(PATH_FIELD).getValue().toString();
        return new Document(id, acl, path);
    }

    @Override
    public List<Document> getDocsWithInvalidAcl(Set<String> acl, String path, List<String> excludePaths) {
        final Timer.Context context = aclTimer.time();
        try {
            return getDocsWithInvalidAclTimed(acl, path, excludePaths);
        } finally {
            context.stop();
        }
    }

    private List<Document> getDocsWithInvalidAclTimed(Set<String> acl, String path, List<String> excludePaths) {
        AndFilterBuilder filter = FilterBuilders.andFilter();
        // Looking for a different ACL
        if (Document.NO_ACL == acl) {
            filter.add(FilterBuilders.notFilter(FilterBuilders.missingFilter(ACL_FIELD).nullValue(true)));
        } else {
            filter.add(FilterBuilders.notFilter(FilterBuilders.termFilter(ACL_FIELD, acl)));
        }
        // Starts with path
        filter.add(FilterBuilders.termFilter(CHILDREN_FIELD, path));
        if (!excludePaths.isEmpty()) {
            // Excluding paths
            OrFilterBuilder excludeClause = FilterBuilders.orFilter();
            for (String excludePath : excludePaths) {
                excludeClause.add(FilterBuilders.termFilter(CHILDREN_FIELD, excludePath));
            }
            filter.add(FilterBuilders.notFilter(excludeClause));
        }
        SearchRequestBuilder request = getClient().prepareSearch(config.esIndex()).setTypes(DOC_TYPE).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setSize(config.maxResults()).addFields(ACL_FIELD, PATH_FIELD).setQuery(
                QueryBuilders.constantScoreQuery(filter));
        logSearchRequest(request);
        SearchResponse response = request.execute().actionGet();
        logSearchResponse(response);
        long hits = response.getHits().getTotalHits();
        ArrayList<Document> ret = new ArrayList<>((int) hits);
        if (hits > 0) {
            log.info(String.format("%d docs with potential invalid ACL found on ES", hits));
            for (SearchHit hit : response.getHits()) {
                String aclDoc[];
                Set<String> aclSet;
                try {
                    Object[] aclArray = hit.field(ACL_FIELD).getValues().toArray();
                    aclDoc = Arrays.copyOf(aclArray, aclArray.length, String[].class);
                    aclSet = new HashSet<>(Arrays.asList(aclDoc));
                } catch (NullPointerException e) {
                    aclSet = Document.NO_ACL;
                }
                Document esDoc = new Document(hit.getId(), aclSet, hit.field(PATH_FIELD).getValue().toString());
                ret.add(esDoc);
            }
        }
        return ret;
    }

    @Override
    public long getCardinality() {
        final Timer.Context context = cardinalityTimer.time();
        try {
            return getCardinalityTimed();
        } finally {
            context.stop();
        }
    }

    @Override
    public long getProxyCardinality() {
        CountRequestBuilder request = getClient().prepareCount(config.esIndex()).setTypes(DOC_TYPE).setQuery(
                QueryBuilders.constantScoreQuery(FilterBuilders.termFilter("ecm:isProxy", "true")));
        logSearchRequest(request);
        CountResponse response = request.execute().actionGet();
        logSearchResponse(response);
        return response.getCount();
    }

    @Override
    public long getVersionCardinality() {
        CountRequestBuilder request = getClient().prepareCount(config.esIndex()).setTypes(DOC_TYPE).setQuery(
                QueryBuilders.constantScoreQuery(FilterBuilders.termFilter("ecm:isVersion", "true")));
        logSearchRequest(request);
        CountResponse response = request.execute().actionGet();
        logSearchResponse(response);
        return response.getCount();
    }

    @Override
    public long getOrphanCardinality() {
        CountRequestBuilder request = getClient().prepareCount(config.esIndex()).setTypes(DOC_TYPE).setQuery(
                QueryBuilders.constantScoreQuery(FilterBuilders.missingFilter("ecm:parentId").nullValue(true)));
        logSearchRequest(request);
        CountResponse response = request.execute().actionGet();
        logSearchResponse(response);
        return response.getCount();
    }

    @Override
    public Map<String, Long> getTypeCardinality() {
        final Timer.Context context = typeCardinalityTimer.time();
        try {
            return getTypeCardinalityTimed();
        } finally {
            context.stop();
        }
    }

    private Map<String, Long> getTypeCardinalityTimed() {
        LinkedHashMap<String, Long> ret = new LinkedHashMap<>();
        SearchRequestBuilder request = getClient().prepareSearch(config.esIndex()).setSearchType(SearchType.COUNT).setQuery(
                QueryBuilders.constantScoreQuery(FilterBuilders.notFilter(FilterBuilders.termFilter("ecm:isProxy",
                        "true")))).addAggregation(
                AggregationBuilders.terms("primaryType").field("ecm:primaryType").size(0));
        logSearchRequest(request);
        SearchResponse response = request.execute().actionGet();
        logSearchResponse(response);
        Terms terms = response.getAggregations().get("primaryType");
        for (Terms.Bucket term : terms.getBuckets()) {
            ret.put(term.getKey(), term.getDocCount());
        }
        return ret;

    }

    private long getCardinalityTimed() {
        CountRequestBuilder request = getClient().prepareCount(config.esIndex()).setTypes(DOC_TYPE).setQuery(
                QueryBuilders.constantScoreQuery(FilterBuilders.notFilter(FilterBuilders.andFilter(
                        FilterBuilders.termFilter("ecm:isProxy", "true"),
                        FilterBuilders.termFilter("ecm:isVersion", "true")))));
        logSearchRequest(request);
        CountResponse response = request.execute().actionGet();
        logSearchResponse(response);
        return response.getCount();
    }

    @Override
    public Set<String> getDocumentIdsForType(String type) {
        final Timer.Context context = documentIdsForTypeTimed.time();
        try {
            return getDocumentIdsForTypeTimed(type);
        } finally {
            context.stop();
        }
    }

    private Set<String> getDocumentIdsForTypeTimed(String type) {
        Set<String> ret = new HashSet<>();
        SearchRequestBuilder request = getClient().prepareSearch(config.esIndex()).setSearchType(SearchType.SCAN).setQuery(
                QueryBuilders.constantScoreQuery(FilterBuilders.andFilter(
                        FilterBuilders.termFilter("ecm:primaryType", type),
                        FilterBuilders.notFilter(FilterBuilders.termFilter("ecm:isProxy", "true"))))).setScroll(
                getScrollTime()).setSize(config.getScrollSize()).addField("_uid");
        logSearchRequest(request);
        SearchResponse response = request.execute().actionGet();
        logSearchResponse(response);
        while (true) {
            response = getClient().prepareSearchScroll(response.getScrollId()).setScroll(getScrollTime()).execute().actionGet();
            if (log.isTraceEnabled()) {
                logSearchResponse(response);
            }
            for (SearchHit hit : response.getHits()) {
                ret.add(hit.getId());
            }
            if (response.getHits().getHits().length == 0) {
                break;
            }
        }
        return ret;
    }

    private TimeValue getScrollTime() {
        return TimeValue.timeValueMinutes(config.getScrollTime());
    }

    private Client getClient() {
        return client;
    }

    private void logSearchResponse(ActionResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("Response: " + response.toString());
        }
    }

    private void logSearchRequest(ActionRequestBuilder request) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Search query: curl -XGET 'http://localhost:9200/%s/%s/_search?pretty' -d '%s'",
                    config.esIndex(), DOC_TYPE, request.toString()));
        }
    }

}
