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
package org.nuxeo.tools.esync.checker;

import java.util.Set;

import org.nuxeo.tools.esync.event.MissingEvent;
import org.nuxeo.tools.esync.event.TrailingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import org.nuxeo.tools.esync.event.DiffTypeEvent;

public class DocumentTypeLister extends AbstractChecker {
    private static final Logger log = LoggerFactory
            .getLogger(DocumentTypeLister.class);
    private String primaryType;

    @Override
    void check() {
        Set<String> esIds = es.getDocumentIdsForType(primaryType);
        Set<String> dbIds = db.getDocumentIdsForType(primaryType);
        Sets.SetView<String> diff = Sets.difference(dbIds, esIds);
        if (diff.isEmpty()) {
            postMessage(String.format("All DB docIds found in ES for type %s",
                    primaryType));
        } else {
            postMessage(String.format("Missing %d documents for type: %s",
                    diff.size(), primaryType));
            for (String key : diff) {
                post(new MissingEvent(key, String.format("type %s", primaryType)));
            }
        }
        diff = Sets.difference(esIds, dbIds);
        if (diff.isEmpty()) {
            postMessage(String.format("All ES docIds found in DB for type %s",
                    primaryType));
        } else {
            postMessage(String.format("%d spurious documents for type: %s",
                    diff.size(), primaryType));
            for (String key : diff) {
                post(new TrailingEvent(key, String.format("type %s", primaryType)));
            }
        }
    }

    @Override
    public boolean autoRun() {
        // run only when receiving an org.nuxeo.tools.esync.event
        return false;
    }

    @Subscribe
    public void handleEvent(DiffTypeEvent event) {
        primaryType = event.getType();
        run();
    }

    @Override
    public void init() {
        eventBus.register(this);
    }

    @Override
    String getName() {
        return "DocumentTypeLister";
    }
}
