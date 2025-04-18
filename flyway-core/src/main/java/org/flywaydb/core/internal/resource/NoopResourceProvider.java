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
package org.flywaydb.core.internal.resource;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;

import java.util.Collection;
import java.util.Collections;

/**
 * No-op resource provider.
 */
public enum NoopResourceProvider implements ResourceProvider {
    INSTANCE;

    @Override
    public LoadableResource getResource(String name) {
        return null;
    }

    /**
     * Retrieve all resources whose name begins with this prefix and ends with any of these suffixes.
     *
     * @param prefix The prefix.
     * @param suffixes The suffixes.
     * @return The matching resources.
     */
    public Collection<LoadableResource> getResources(String prefix, String[] suffixes) {
        return Collections.emptyList();
    }
}
