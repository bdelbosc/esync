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
package org.nuxeo.tools.esync.db.dialect;

public abstract class Dialect {
    // Some hints:
    // - The misc table contains version entries but no proxy
    // - The Root document is not present in the misc table
    // - versions are orphan (parentid is null)
    // - proxies hold a primaryType set to ecm:proxy
    private static final String COUNT_QUERY = "SELECT count(1) AS count FROM misc m JOIN hierarchy h ON m.id = h.id WHERE isversion IS NULL AND primarytype!='Root'";
    private static final String COUNT_PROXY_QUERY = "SELECT count(1) AS count FROM proxies";
    private static final String COUNT_VERSION_QUERY = "SELECT count(1) AS count FROM versions";
    private static final String COUNT_ORPHEAN_QUERY = "SELECT count(1) AS count FROM misc m JOIN hierarchy h ON m.id = h.id WHERE h.parentid IS NULL AND h.isversion IS NULL";
    private static final String TYPE_QUERY = "SELECT primarytype, count(1) AS count FROM hierarchy h JOIN misc m ON m.id = h.id GROUP BY primarytype ORDER BY 2 DESC";
    private static final String DOCUMENT_IDS_FOR_TYPE = "SELECT id FROM hierarchy WHERE primarytype = '%s'";

    public abstract String getAclQuery();

    public abstract String getDocumentQuery(String id);

    public String getCountQuery() {
        return COUNT_QUERY;
    }

    public String getProxyCountQuery() {
        return COUNT_PROXY_QUERY;
    }

    public String getVersionCountQuery() {
        return COUNT_VERSION_QUERY;
    }

    public String getOrpheanCountQuery() {
        return COUNT_ORPHEAN_QUERY;
    }

    public String getTypeQuery() {
        return TYPE_QUERY;
    }

    public String getDocumentIdsForType(String type) {
        return String.format(DOCUMENT_IDS_FOR_TYPE, type);
    }
}
