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
package org.nuxeo.tools.esync.config;

import org.aeonbits.owner.Config;

import java.util.Set;

@Config.Sources({ "file:~/.esync.conf", "file:/etc/esync.conf" })
public interface ESyncConfig extends Config {

    @Key("nuxeo.db.user")
    String dbUser();

    @Key("nuxeo.db.password")
    String dbPassword();

    @Key("nuxeo.db.jdbc.url")
    String dbUrl();

    /**
     * The database driver class to load
     */
    @Key("nuxeo.db.driver")
    String dbDriver();

    /**
     * For MongoDB only
     * format: mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
     * */
    @Key("nuxeo.mongodb.server")
    String mongoUri();

    @DefaultValue("nuxeo")
    @Key("nuxeo.mongodb.dbname")
    String mongoDbName();

    @DefaultValue("default")
    @Key("nuxeo.mongodb.collection")
    String mongoCollection();

    /**
     * The name of the Elasticsearch index
     */
    @Key("elasticsearch.indexName")
    String esIndex();

    /**
     * A comma separated list of host:port to join the Elasticsearch cluster
     */
    @Key("elasticsearch.addressList")
    String addressList();

    /**
     * The name of the Elasticsearch cluster
     */
    @Key("elasticsearch.clusterName")
    String clusterName();

    /**
     * Limit the number of documents returned by Elasticsearch
     */
    @DefaultValue("1000")
    @Key("elasticsearch.maxResults")
    int maxResults();

    /**
     * Size of the pool of thread running the checkers
     */
    @DefaultValue("4")
    @Key("checker.pool.size")
    int getPoolSize();

    /**
     * Time to wait in minutes for checkers
     */
    @DefaultValue("60")
    @Key("checker.pool.timeoutMinutes")
    long getTimeoutMinutes();

    @DefaultValue("3")
    @Key("checker.scrollTimeMinute")
    long getScrollTime();

    @DefaultValue("100")
    @Key("checker.scrollSize")
    int getScrollSize();

    @DefaultValue("")
    @Key("checker.list")
    Set<String> getCheckers();

    @DefaultValue("")
    @Key("checker.blackList")
    Set<String> getCheckersBlackList();
}
