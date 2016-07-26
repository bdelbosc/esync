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

public class EsHash {
    private static final String DOC_TYPE = "doc";
    private static final int DEFAULT_SHARD = 5;

    public static int djbHash(String type, String id) {
        long hash = 5381;
        for (int i = 0; i < type.length(); i++) {
            hash = ((hash << 5) + hash) + type.charAt(i);
        }
        for (int i = 0; i < id.length(); i++) {
            hash = ((hash << 5) + hash) + id.charAt(i);
        }
        return (int) hash;
    }

    public static int simpleHash(String type, String id) {
        return type.hashCode() + 31 * id.hashCode();
    }

    public static int getShard(String id, int nbShard) {
        return simpleHash(DOC_TYPE, id) % nbShard;
    }

    /**
     * Return the shard number where the doc id is indexed
     */
    public static int getShard(String id) {
        return getShard(id, DEFAULT_SHARD);
    }
}
