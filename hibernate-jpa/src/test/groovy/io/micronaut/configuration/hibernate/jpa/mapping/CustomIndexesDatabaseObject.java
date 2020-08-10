package io.micronaut.configuration.hibernate.jpa.mapping;

import org.hibernate.boot.model.relational.AbstractAuxiliaryDatabaseObject;
import org.hibernate.dialect.Dialect;

public class CustomIndexesDatabaseObject extends AbstractAuxiliaryDatabaseObject {

    @Override
    public String[] sqlCreateStrings(Dialect dialect) {
        return new String[]{"CREATE VIEW custom_view AS SELECT * FROM account"};
    }

    @Override
    public String[] sqlDropStrings(Dialect dialect) {
        return new String[]{"DROP VIEW IF EXISTS custom_view"};
    }

}
