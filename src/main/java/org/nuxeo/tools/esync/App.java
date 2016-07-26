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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.nuxeo.tools.esync.checker.AbstractChecker;
import org.nuxeo.tools.esync.listener.DiffListener;
import org.nuxeo.tools.esync.listener.ErrorListener;
import org.nuxeo.tools.esync.listener.InfoListener;
import org.nuxeo.tools.esync.listener.MissingListener;
import org.nuxeo.tools.esync.listener.TrailingListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nuxeo.tools.esync.checker.Discovery;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.eventbus.EventBus;

import org.nuxeo.tools.esync.config.ESyncConfig;
import dagger.ObjectGraph;

public class App implements Runnable {
    private final static Logger log = LoggerFactory.getLogger(App.class);

    private final static MetricRegistry registry = SharedMetricRegistries
            .getOrCreate("main");
    private final static ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final static PrintStream printStream = new PrintStream(baos);
    private final static ConsoleReporter reporter = ConsoleReporter
            .forRegistry(registry).outputTo(printStream)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS).build();

    @Inject
    EventBus eventBus;
    @Inject
    ESyncConfig config;
    final List<AbstractChecker> checkers = new ArrayList<>();

    public void injectCheckers(ObjectGraph objectGraph) {
        for (Class checkerClass : Discovery.getCheckersClass(config.getCheckers(),
                config.getCheckersBlackList())) {
            log.info("Injecting checker: " + checkerClass.getSimpleName());
            AbstractChecker checker = (AbstractChecker) objectGraph.get(checkerClass);
            checker.init();
            checkers.add(checker);
        }
    }

    @Override
    public void run() {
        log.info("Starting esync...");
        registerListeners();
        runCheckers();
        log.info("End of esync");
        reportMetrics();
    }

    private void registerListeners() {
        eventBus.register(new InfoListener());
        eventBus.register(new TrailingListener());
        eventBus.register(new MissingListener());
        eventBus.register(new DiffListener());
        eventBus.register(new ErrorListener());
    }

    private void runCheckers() {
        int poolSize = Math.min(config.getPoolSize(), checkers.size());
        if (poolSize < 1) {
            log.error("No checkers found");
        }
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        log.info(String.format(
                "Executing %d checkers with a pool of %d thread(s).",
                checkers.size(), poolSize));
        for (AbstractChecker checker : checkers) {
            if (checker.autoRun()) {
                pool.execute(checker);
            }
        }
        pool.shutdown();
        try {
            if (pool.awaitTermination(config.getTimeoutMinutes(),
                    TimeUnit.MINUTES)) {
                log.info("All checkers terminated");
            } else {
                log.error(String.format(
                        "Timeout on worker pool after %d minutes.",
                        config.getTimeoutMinutes()));
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void reportMetrics() {
        reporter.report();
        reporter.stop();
        String stats = null;
        try {
            stats = baos.toString("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }
        log.debug(stats);
    }
}
