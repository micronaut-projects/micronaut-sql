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
     * The model containing information about the data source whose password has changed.
     */
    private final DataSourcePasswordModel dataSourcePasswordModel;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public DataSourcePasswordChangedEvent(DataSourcePasswordModel source) {
        super(source);
        this.dataSourcePasswordModel = source;
    }

    /**
     * Returns the model containing information about the data source whose password has changed.
     *
     * @return the data source password model.
     */
    public DataSourcePasswordModel getDataSourcePasswordModel() {
        return dataSourcePasswordModel;
    }

    /**
     * A simple container holding information about a data source and its new password.
     *
     * @param dataSourceName the name of the data source.
     * @param newPassword the new password of the data source.
     */
    public record DataSourcePasswordModel(String dataSourceName, String newPassword) {}
}
