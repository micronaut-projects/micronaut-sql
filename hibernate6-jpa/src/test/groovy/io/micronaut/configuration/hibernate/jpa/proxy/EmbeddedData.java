package io.micronaut.configuration.hibernate.jpa.proxy;

import jakarta.persistence.Embeddable;

@Embeddable
public class EmbeddedData {

    private String f;

    public String getF() {
        return f;
    }

    public void setF(String f) {
        this.f = f;
    }
}
