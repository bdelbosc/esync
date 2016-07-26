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
package org.nuxeo.tools.esync.event;

import org.nuxeo.tools.esync.db.Document;

/**
 * Event to notify difference between a document in org.nuxeo.tools.esync.db and org.nuxeo.tools.esync.es
 */
public class DiffEvent extends Event {

    private final Document dbDocument;
    private final Document esDocument;

    public DiffEvent(Document dbDocument, Document esDocument, String message) {
        super(message);
        this.dbDocument = dbDocument;
        this.esDocument = esDocument;
    }

    public Document getDbDocument() {
        return dbDocument;
    }

    public Document getEsDocument() {
        return esDocument;
    }
}
