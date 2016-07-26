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

import java.util.List;
import java.util.Set;

import org.nuxeo.tools.esync.config.ESyncConfig;

public interface Db {

    void initialize(ESyncConfig config);

    void close();

    /**
     * Return the list of document that holds an ACL
     */
    List<Document> getDocumentWithAcl();

    /**
     * Get the total number of documents, excluding proxies, versions and the Root document.
     */
    long getCardinality();

    long getProxyCardinality();

    long getVersionCardinality();

    long getOrphanCardinality();

    Document getDocument(String id);

    java.util.Map<String, Long> getTypeCardinality();

    Set<String> getDocumentIdsForType(String type);
}
