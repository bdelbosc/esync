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
package org.nuxeo.tools.esync.db.dialect;

public class DialectFactory {

    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    private static final String MSSQL_DRIVE = "net.sourceforge.jtds.jdbc.Driver";
    private static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";

    public static Dialect create(String driver) {
        switch (driver) {
            case POSTGRESQL_DRIVER:
                return new Postgresql();
            case MSSQL_DRIVE:
                return new Mssql();
            case ORACLE_DRIVER:
                return new Oracle();
        }
        throw new IllegalArgumentException("Unknown driver :" + driver);
    }
}
