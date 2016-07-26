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

import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;

/**
 * Use reflection to get the list of checkers
 */
public class Discovery {

    static public Set<Class<? extends AbstractChecker>> getCheckersClass(
            Set<String> filter, Set<String> blackList) {
        Reflections reflections = new Reflections("org.nuxeo.tools.esync.checker");
        Set<Class<? extends AbstractChecker>> checkers = reflections
                .getSubTypesOf(AbstractChecker.class);
        if ((filter == null || filter.isEmpty()) && (blackList == null || blackList.isEmpty())) {
            return checkers;
        }
        Set<Class<? extends AbstractChecker>> ret = new HashSet<>(filter.size());
        // explicit list
        if (filter != null && !filter.isEmpty()) {
            for (Class klass : checkers) {
                if (filter.contains(klass.getSimpleName())) {
                    ret.add(klass);
                }
            }
            return ret;
        }
        // all except black listed
        for (Class klass : checkers) {
            String name = klass.getSimpleName();
            if (blackList.contains(name)) {
                continue;
            }
            ret.add(klass);
        }
        return ret;
    }

}
