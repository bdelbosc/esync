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

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.nuxeo.tools.esync.config.ESyncConfig;
import org.nuxeo.tools.esync.db.Document;

public interface Es {

    void initialize(ESyncConfig config);

    void close();

    Document getDocument(String id) throws NoSuchElementException;

    List<Document> getDocsWithInvalidAcl(Set<String> acl, String path, List<String> excludePaths);

    long getCardinality();

    long getProxyCardinality();

    long getVersionCardinality();

    long getOrphanCardinality();

    Map<String, Long> getTypeCardinality();

    Set<String> getDocumentIdsForType(String type);
}
