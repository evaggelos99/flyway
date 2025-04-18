/*-
 * ========================LICENSE_START=================================
 * flyway-core
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
package org.flywaydb.core.internal.callback;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.Error;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.callback.Warning;
import org.flywaydb.core.api.output.OperationResult;

import java.util.List;

/**
 * A callback executor that does nothing.
 */
public enum NoopCallbackExecutor implements CallbackExecutor {
    INSTANCE;

    @Override
    public void onEvent(Event event) {
    }

    @Override
    public void onMigrateOrUndoEvent(Event event) {
    }

    @Override
    public void setMigrationInfo(MigrationInfo migrationInfo) {
    }

    @Override
    public void onEachMigrateOrUndoEvent(Event event) {
    }

    @Override
    public void onOperationFinishEvent(Event event, OperationResult operationResult) {
    }

    @Override
    public void onEachMigrateOrUndoStatementEvent(Event event, String sql, List<Warning> warnings, List<Error> errors) {
    }
}
