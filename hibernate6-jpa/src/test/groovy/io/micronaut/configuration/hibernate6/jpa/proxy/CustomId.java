package io.micronaut.configuration.hibernate6.jpa.proxy;

import java.io.Serializable;

public class CustomId implements Serializable {

    private String a;
    private String b;

    public CustomId() {
    }

    public CustomId(String a, String b) {
        this.a = a;
        this.b = b;
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }
}
