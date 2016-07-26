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

public class Postgresql extends Dialect {
    private static final String ACL_QUERY = "SELECT id, primaryType, nx_get_read_acl(id) FROM hierarchy WHERE id IN (SELECT DISTINCT(id) FROM acls)";
    private static final String DOC_QUERY = "SELECT id, primaryType, nx_get_read_acl(id) FROM hierarchy WHERE id='%s'";

    @Override
    public String getAclQuery() {
        return ACL_QUERY;
    }

    @Override
    public String getDocumentQuery(String id) {
        return String.format(DOC_QUERY, id);
    }
}
