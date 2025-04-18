/*-
 * ========================LICENSE_START=================================
 * flyway-database-sybasease
 * ========================================================================
 * Copyright (C) 2010 - 2025 Red Gate Software Ltd
 * ========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.flywaydb.database.sybasease;

import lombok.CustomLog;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.extensibility.Tier;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.Results;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.sqlscript.Delimiter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@CustomLog
public class SybaseASEDatabase extends Database<SybaseASEConnection> {
    private String databaseName = null;
    private boolean supportsMultiStatementTransactions = false;

    public SybaseASEDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory, StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    protected SybaseASEConnection doGetConnection(Connection connection) {
        return new SybaseASEConnection(this, connection);
    }

    @Override
    public void ensureSupported(Configuration configuration) {
        ensureDatabaseIsRecentEnough("15.7");

        ensureDatabaseNotOlderThanOtherwiseRecommendUpgradeToFlywayEdition("16.3", Tier.PREMIUM, configuration);

        recommendFlywayUpgradeIfNecessary("16.3");
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        return "CREATE TABLE " + table.getName() + " (\n" +
                "    installed_rank INT NOT NULL,\n" +
                "    version VARCHAR(50) NULL,\n" +
                "    description VARCHAR(200) NOT NULL,\n" +
                "    type VARCHAR(20) NOT NULL,\n" +
                "    script VARCHAR(1000) NOT NULL,\n" +
                "    checksum INT NULL,\n" +
                "    installed_by VARCHAR(100) NOT NULL,\n" +
                "    installed_on datetime DEFAULT getDate() NOT NULL,\n" +
                "    execution_time INT NOT NULL,\n" +
                "    success decimal NOT NULL,\n" +
                "    PRIMARY KEY (installed_rank)\n" +
                ")\n" +
                "lock datarows on 'default'\n" +
                (baseline ? getBaselineStatement(table) + "\n" : "") +
                "go\n" +
                "CREATE INDEX " + table.getName() + "_s_idx ON " + table.getName() + " (success)\n" +
                "go\n";
    }

    @Override
    public boolean supportsEmptyMigrationDescription() {
        // Sybase will convert the empty string to a single space implicitly, which won't error on updating the
        // history table but will subsequently fail validation of the history table against the file name.
        return false;
    }

    @Override
    public Delimiter getDefaultDelimiter() {
        return Delimiter.GO;
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getMainConnection().getJdbcTemplate().queryForString("SELECT user_name()");
    }

    @Override
    public boolean supportsDdlTransactions() {
        return false;
    }

    @Override
    public String getBooleanTrue() {
        return "1";
    }

    @Override
    public String getBooleanFalse() {
        return "0";
    }

    @Override
    public String getOpenQuote() {
        return "";
    }

    @Override
    public String getCloseQuote() {
        return "";
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    @Override
    /**
     * Multi statement transaction support is dependent on the 'ddl in tran' option being set.
     * However, setting 'ddl in tran' doesn't necessarily mean that multi-statement transactions are supported.
     * i.e.
     *  - multi statement transaction support => ddl in tran
     *  - ddl in tran =/> multi statement transaction support
     * Also, ddl in tran can change during execution for unknown reasons.
     * Therefore, as a best guess:
     *  - When this method is called, check ddl in tran
     *  - If ddl in tran is true, assume support for multi statement transactions forever more
     *      - Never check ddl in tran again
     *  - If ddl in tran is false, return false
     *      - Check ddl in tran again on the next call
     */
    public boolean supportsMultiStatementTransactions() {
        if (supportsMultiStatementTransactions) {
            LOG.debug("ddl in tran was found to be true at some point during execution." +
                              "Therefore multi statement transaction support is assumed.");
            return true;
        }

        boolean ddlInTran = getDdlInTranOption();

        if (ddlInTran) {
            LOG.debug("ddl in tran is true. Multi statement transaction support is now assumed.");
            supportsMultiStatementTransactions = true;
        }

        return supportsMultiStatementTransactions;
    }

    boolean getDdlInTranOption() {
        try {
            // http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc36273.1600/doc/html/san1393052037182.html
            String databaseName = getDatabaseName();
            // The Sybase driver (v7.07) concatenates "null" to this query and we can't see why. By adding a one-line
            // comment marker we can at least prevent this causing us problems until we get a resolution.
            String getDatabaseMetadataQuery = "sp_helpdb " + databaseName + " -- ";
            Results results = getMainConnection().getJdbcTemplate().executeStatement(getDatabaseMetadataQuery);
            for (int resultsIndex = 0; resultsIndex < results.getResults().size(); resultsIndex++) {
                List<String> columns = results.getResults().get(resultsIndex).columns();
                if (columns != null) {
                    int statusIndex = getStatusIndex(columns);
                    if (statusIndex > -1) {
                        String options = results.getResults().get(resultsIndex).data().get(0).get(statusIndex);
                        return options.contains("ddl in tran");
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw new FlywayException(e);
        }
    }

    private int getStatusIndex(List<String> columns) {
        for (int statusIndex = 0; statusIndex < columns.size(); statusIndex++) {
            if ("status".equals(columns.get(statusIndex))) {
                return statusIndex;
            }
        }
        return -1;
    }

    String getDatabaseName() throws SQLException {
        if (databaseName == null) {
            databaseName = getMainConnection().getJdbcTemplate().queryForString("select db_name()");
        }
        return databaseName;
    }
}
