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
package org.nuxeo.tools.esync.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

import org.nuxeo.tools.esync.config.ESyncConfig;
import org.nuxeo.tools.esync.db.dialect.Dialect;
import org.nuxeo.tools.esync.db.dialect.DialectFactory;

public class DbSql implements Db {
    private static final Logger log = LoggerFactory.getLogger(DbSql.class);

    private static final String DENY_ALL = "-Everyone";

    private static final String UNSUPPORTED_ACL = "_UNSUPPORTED_ACL_";

    private ESyncConfig config;

    private Connection dbConnection;

    private Dialect dialect;

    private final static MetricRegistry registry = SharedMetricRegistries.getOrCreate("main");

    private final Timer aclTimer = registry.timer("db.acl");

    private final Timer cardinalityTimer = registry.timer("db.cardinality");

    private final Timer typeCardinalityTimer = registry.timer("db.type.cardinality");

    private final Timer documentIdsForTypeTimed = registry.timer("db.type.documentIdsForType");

    @Override
    public void initialize(ESyncConfig config) {
        this.config = config;
        dialect = DialectFactory.create(config.dbDriver());
        getDbConnection();
    }

    @Override
    public void close() {
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                log.warn(e.getMessage(), e);
            }
            dbConnection = null;
        }
    }

    public List<Document> getDocumentWithAcl() {
        final Timer.Context context = aclTimer.time();
        try {
            return getDocumentWithAclTimed();
        } finally {
            context.stop();
        }
    }

    private List<Document> getDocumentWithAclTimed() {
        List<Document> ret;
        PreparedStatement ps;
        try {
            ps = getDbConnection().prepareStatement(dialect.getAclQuery());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ret = Collections.emptyList();
            } else {
                ret = new ArrayList<>();
                do {
                    String primaryType = rs.getString(2);
                    String[] acl = decodeAcl(rs.getString(3));
                    ret.add(new Document(rs.getString(1), primaryType, acl));
                } while (rs.next());
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return ret;
    }

    @Override
    public long getCardinality() {
        final Timer.Context context = cardinalityTimer.time();
        try {
            return getDbCount(dialect.getCountQuery());
        } finally {
            context.stop();
        }
    }

    @Override
    public long getProxyCardinality() {
        final Timer.Context context = cardinalityTimer.time();
        try {
            return getDbCount(dialect.getProxyCountQuery());
        } finally {
            context.stop();
        }
    }

    @Override
    public long getVersionCardinality() {
        final Timer.Context context = cardinalityTimer.time();
        try {
            return getDbCount(dialect.getVersionCountQuery());
        } finally {
            context.stop();
        }
    }

    @Override
    public long getOrphanCardinality() {
        final Timer.Context context = cardinalityTimer.time();
        try {
            return getDbCount(dialect.getOrpheanCountQuery());
        } finally {
            context.stop();
        }
    }

    @Override
    public Document getDocument(String id) {
        Document ret = null;
        PreparedStatement ps;
        try {
            ps = getDbConnection().prepareStatement(dialect.getDocumentQuery(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String primaryType = rs.getString(2);
                String[] acl = decodeAcl(rs.getString(3));
                ret = new Document(rs.getString(1), primaryType, acl);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return ret;

    }

    private long getDbCount(String query) {
        int count = -1;
        try {
            Statement st = getDbConnection().createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                count = rs.getInt("count");
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return count;
    }

    private String[] decodeAcl(String aclString) {
        ArrayList<String> acl = new ArrayList<>(Arrays.asList(StringUtils.split(aclString, ",")));
        // remove trailing -Everyone
        int lastAceIndex = acl.size() - 1;
        if (DENY_ALL.equals(acl.get(lastAceIndex))) {
            acl.remove(lastAceIndex);
        }
        for (int i = 0; i < acl.size(); i++) {
            String ace = acl.get(i);
            if (ace.startsWith("-")) {
                acl.set(i, UNSUPPORTED_ACL);
            }
        }
        return acl.toArray(new String[acl.size()]);
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
        Map<String, Long> ret = new LinkedHashMap<>();
        try {
            Statement st = getDbConnection().createStatement();
            ResultSet rs = st.executeQuery(dialect.getTypeQuery());
            while (rs.next()) {
                String primaryType = rs.getString("primarytype");
                long count = rs.getLong("count");
                ret.put(primaryType, count);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return ret;
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
        try {
            Statement st = getDbConnection().createStatement();
            ResultSet rs = st.executeQuery(dialect.getDocumentIdsForType(type));
            while (rs.next()) {
                String id = rs.getString("id");
                ret.add(id);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return ret;
    }

    private Connection getDbConnection() {
        if (dbConnection == null) {
            log.debug("Connect to database:" + config.dbUrl() + " from " + getHostName());
            try {
                Class.forName(config.dbDriver());
                dbConnection = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                throw new IllegalArgumentException(e);
            } catch (ClassNotFoundException e) {
                String msg = "Missing JDBC Driver: " + e.getMessage();
                log.error(msg);
                throw new RuntimeException(msg);
            }
        }
        return dbConnection;
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
}
