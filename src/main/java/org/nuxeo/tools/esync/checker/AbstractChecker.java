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

import javax.inject.Inject;

import com.google.common.eventbus.EventBus;

import org.nuxeo.tools.esync.config.ESyncConfig;
import org.nuxeo.tools.esync.db.Db;
import org.nuxeo.tools.esync.es.Es;
import org.nuxeo.tools.esync.event.ErrorEvent;
import org.nuxeo.tools.esync.event.Event;
import org.nuxeo.tools.esync.event.InfoEvent;

public abstract class AbstractChecker implements Runnable {

    @Inject
    ESyncConfig config;
    @Inject
    EventBus eventBus;
    @Inject
    Db db;
    @Inject
    Es es;

    public void post(Event event) {
        eventBus.post(event);
    }

    public void postMessage(String message) {
        eventBus.post(new InfoEvent(getName() + ": " + message));
    }

    public void postError(String message) {
        eventBus.post(new ErrorEvent(getName() + ": " + message));
    }

    public boolean autoRun() {
        return true;
    }

    @Override
    public void run() {
        postMessage("Starting");
        db.initialize(config);
        es.initialize(config);
        try {
            check();
        } finally {
            db.close();
            es.close();
        }
        postMessage("Terminated");
    }

    abstract void check();

    abstract String getName();

    public void init() {
    }
}
