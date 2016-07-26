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
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nuxeo.tools.esync.config.ESyncConfig;
import dagger.ObjectGraph;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws SQLException, IOException {
        ESyncConfig config = getConfig(args);
        ObjectGraph objectGraph = ObjectGraph.create(new AppModule(config));
        App app = objectGraph.get(App.class);
        app.injectCheckers(objectGraph);
        app.run();
    }

    private static ESyncConfig getConfig(String[] args) {
        ESyncConfig ret;
        if (args.length <= 0) {
            ret = ConfigFactory.create(ESyncConfig.class);
        } else {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(args[0]));
            } catch (IOException e) {
                log.error("Wrong file " + args[0] + e.getMessage(), e);
                throw new IllegalArgumentException(e);
            }
            ret = ConfigFactory.create(ESyncConfig.class, props);
        }
        return ret;
    }

}
