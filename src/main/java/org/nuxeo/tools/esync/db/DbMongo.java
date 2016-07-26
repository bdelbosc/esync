package org.nuxeo.tools.esync.db;/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bdelbosc
 */

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.nuxeo.tools.esync.config.ESyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 7.1
 */
public class DbMongo implements Db {
    private static final Logger log = LoggerFactory.getLogger(DbMongo.class);
    private static final String ROOT_TYPE = "Root";
    private String uri;
    private String dbName;
    private String collectionName;
    private MongoCollection<org.bson.Document> mongoCollection;
    private MongoClient mongoClient;

    @Override
    public void initialize(ESyncConfig config) {
        uri = config.mongoUri();
        dbName = config.mongoDbName();
        collectionName = config.mongoCollection();
        getMongoCollection();
    }

    private MongoCollection<org.bson.Document> getMongoCollection() {
        if (mongoCollection == null) {
            log.debug("Connect to mongoClient: " + uri + " on org.nuxeo.tools.esync.db: " + dbName + " mongoCollection: " + collectionName +
                    " from " + getHostName());
            try {
                if (mongoClient == null) {
                    mongoClient = new MongoClient(new MongoClientURI(uri));
                }
                MongoDatabase db = mongoClient.getDatabase(dbName);
                mongoCollection = db.getCollection(collectionName);
            } catch (MongoException e) {
                log.error("Failed to connect to " + uri, e);
                throw new RuntimeException(e.getMessage());
            }
        }
        assert (mongoCollection != null);
        return mongoCollection;
    }

    private static String getHostName() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return hostname;
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            mongoCollection = null;
        }
    }

    @Override
    public List<Document> getDocumentWithAcl() {
        final List<Document> ret = new ArrayList<>();
        MongoCollection<org.bson.Document> table = getMongoCollection();
        BasicDBObject searchQuery = new BasicDBObject("ecm:acp", new BasicDBObject("$exists", true));
        FindIterable<org.bson.Document> iterable = table.find(searchQuery);
        iterable.forEach(new Block<org.bson.Document>() {
            @Override
            public void apply(final org.bson.Document document) {
                ArrayList<String> racl = document.get("ecm:racl", ArrayList.class);
                String[] acl = racl.toArray(new String[racl.size()]);
                Document doc = new Document(document.getString("ecm:id"),
                        document.getString("ecm:primaryType"),
                        acl);
                ret.add(doc);
            }
        });
        return ret;
    }

    @Override
    public long getCardinality() {
        MongoCollection<org.bson.Document> table = getMongoCollection();
        // The Root document is not returned by Es or VCS but by Mongo
        return table.count() - 1;
    }

    @Override
    public long getProxyCardinality() {
        MongoCollection<org.bson.Document> table = getMongoCollection();
        BasicDBObject searchQuery = new BasicDBObject("ecm:isProxy", true);
        return table.count(searchQuery);
    }

    @Override
    public long getVersionCardinality() {
        MongoCollection<org.bson.Document> table = getMongoCollection();
        BasicDBObject searchQuery = new BasicDBObject("ecm:isVersion", true);
        return table.count(searchQuery);
    }

    @Override
    public long getOrphanCardinality() {
        MongoCollection<org.bson.Document> table = getMongoCollection();
        BasicDBObject searchQuery = new BasicDBObject("ecm:parentId", new BasicDBObject("$exists", false));
        searchQuery.append("ecm:isVersion", new BasicDBObject("$ne", true));
        // Don't take in account the root document
        long ret = table.count(searchQuery) - 1;
        return ret;
    }

    private void findAndDump(BasicDBObject searchQuery) {
        MongoCollection<org.bson.Document> table = getMongoCollection();
        FindIterable<org.bson.Document> iterable = table.find(searchQuery);
        iterable.forEach(new Block<org.bson.Document>() {
            @Override
            public void apply(final org.bson.Document document) {
                System.out.println(document);
            }
        });
    }

    @Override
    public Document getDocument(String id) {
        final Document[] ret = new Document[1];
        MongoCollection<org.bson.Document> table = getMongoCollection();
        BasicDBObject searchQuery = new BasicDBObject("ecm:id", id);
        new ArrayList<>();
        FindIterable<org.bson.Document> iterable = table.find(searchQuery);
        iterable.forEach(new Block<org.bson.Document>() {
            @Override
            public void apply(final org.bson.Document document) {
                ArrayList<String> racl = document.get("ecm:racl", ArrayList.class);
                String[] acl = racl.toArray(new String[racl.size()]);
                Document doc = new Document(document.getString("ecm:id"),
                        document.getString("ecm:primaryType"),
                        acl);
                ret[0] = doc;
            }
        });
        return ret[0];
    }

    @Override
    public Map<String, Long> getTypeCardinality() {
        final Map<String, Long> ret = new LinkedHashMap<>();
        MongoCollection<org.bson.Document> table = getMongoCollection();
        AggregateIterable<org.bson.Document> iterable = table.aggregate(asList(
                new org.bson.Document("$match", new BasicDBObject("ecm:isProxy", new BasicDBObject("$ne", true))),
                new org.bson.Document("$group",new org.bson.Document("_id", "$ecm:primaryType")
                                .append("count", new org.bson.Document("$sum", 1)))));
        iterable.forEach(new Block<org.bson.Document>() {
            @Override
            public void apply(final org.bson.Document document) {
                String primaryType = document.getString("_id");
                long count = document.getInteger("count");
                if (!ROOT_TYPE.equals(primaryType)) {
                    ret.put(primaryType, count);
                }
            }
        });
        return ret;
    }

    @Override
    public Set<String> getDocumentIdsForType(String type) {
        final Set<String> ret = new HashSet<>();
        MongoCollection<org.bson.Document> table = getMongoCollection();
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("ecm:primaryType", type);
        FindIterable<org.bson.Document> iterable = table.find(searchQuery);
        iterable.forEach(new Block<org.bson.Document>() {
            @Override
            public void apply(final org.bson.Document document) {
                ret.add(document.getString("ecm:id"));
            }
        });
        return ret;
    }
}
