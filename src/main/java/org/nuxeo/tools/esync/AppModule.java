package org.nuxeo.tools.esync;/*
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
import javax.inject.Singleton;

import org.nuxeo.tools.esync.checker.AclChecker;
import org.nuxeo.tools.esync.checker.CardinalityChecker;
import org.nuxeo.tools.esync.checker.DocumentTypeLister;
import org.nuxeo.tools.esync.checker.TypeCardinalityChecker;
import com.google.common.eventbus.EventBus;

import org.nuxeo.tools.esync.config.ESyncConfig;
import dagger.Module;
import dagger.Provides;
import org.nuxeo.tools.esync.db.Db;
import org.nuxeo.tools.esync.db.DbMongo;
import org.nuxeo.tools.esync.db.DbSql;
import org.nuxeo.tools.esync.es.Es;
import org.nuxeo.tools.esync.es.EsDefault;

/**
 * Dagger app org.nuxeo.tools.esync.module
 */
@Module(injects = { App.class,
        TypeCardinalityChecker.class,
        AclChecker.class,
        CardinalityChecker.class,
        DocumentTypeLister.class
        }, complete = false, library = true)
public class AppModule {

    private final ESyncConfig config;

    public AppModule(ESyncConfig config) {
        this.config = config;
    }

    @Provides
    ESyncConfig provideConfig() {
        return config;
    }

    @Provides
    @Singleton
    Es provideEs() {
        EsDefault ret = new EsDefault();
        return ret;
    }

    @Provides
    Db provideDb() {
        Db ret;
        if (config.mongoUri() != null) {
            ret = new DbMongo();
        } else {
            ret = new DbSql();
        }
        // System.out.println("provide org.nuxeo.tools.esync.db " + ret);
        // for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
        //   System.out.println(ste);
        // }
        return ret;
    }

    @Provides
    @Singleton
    EventBus provideEventBus() {
        EventBus ret = new EventBus();
        return ret;
    }

}
