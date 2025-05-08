/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.jdbc;

import io.micronaut.context.event.ApplicationEvent;

/**
 * An event indicating that the password of a data source has changed.
 *
 * This event is published when the password of a data source is updated. It contains information about the affected data source and its new password.
 *
 * @see DataSourcePasswordModel
 */
public final class DataSourcePasswordChangedEvent extends ApplicationEvent {

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public DataSourcePasswordChangedEvent(DataSourcePasswordModel source) {
        super(source);
    }

    /**
     * Returns the model containing information about the data source whose password has changed.
     *
     * @return the data source password model.
     */
    public DataSourcePasswordModel getDataSourcePasswordModel() {
        return (DataSourcePasswordModel) getSource();
    }

    /**
     * A simple container holding information about a data source and its new password.
     *
     * @param dataSourceName the name of the data source.
     * @param newPassword the new password of the data source.
     */
    public record DataSourcePasswordModel(String dataSourceName, String newPassword) {
    }
}
