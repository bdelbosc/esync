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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class Document {
    private static final String EVERYONE = "Everyone";
    public final String id;
    public Set<String> acl;
    public String path;
    public String parentId;
    public String primaryType;

    static public final Set<String> NO_ACL = Collections.emptySet();

    public Document(String id, Set<String> acl, String path) {
        this.id = id;
        this.acl = acl;
        this.path = path;
    }

    public Document(String id, String primarytype, String[] acl) {
        this.id = id;
        this.acl = new HashSet<>(Arrays.asList(acl));
        this.primaryType = primarytype;
    }

    @Override
    public String toString() {
        return String.format("<doc id=%s primaryType=%s acl=%s path=%s parentid=%s />", id, primaryType,
                StringUtils.join(acl, ","), path, parentId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Document)) {
            return false;
        }
        Document other = (Document) obj;
        if (id != null && !id.equals(other.id)) {
            return false;
        }
        if (acl != null && !acl.equals(other.acl)) {
            if (!acl.contains(EVERYONE) || !other.acl.contains(EVERYONE)) {
                // some org.nuxeo.tools.esync.db backend factorize any ACLR that contains Everyone to Everyone
                return false;
            }
        }
        if (path != null && other.path != null && !path.equals(other.path)) {
            return false;
        }
        if (parentId != null && other.parentId != null && !parentId.equals(other.parentId)) {
            return false;
        }
        return true;
    }

    public void merge(Document other) {
        if (other.acl != null) {
            this.acl = other.acl;
        }
        if (other.path != null) {
            this.path = other.path;
        }
        if (other.parentId != null) {
            this.parentId = other.parentId;
        }
        if (other.primaryType != null) {
            this.primaryType = other.primaryType;
        }
    }
}
