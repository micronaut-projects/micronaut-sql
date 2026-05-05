package io.micronaut.sql.ojdbc.extensions.test;

import oracle.jdbc.spi.PasswordProvider;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FooBarPasswordProvider implements PasswordProvider {

    private static final Parameter VAULT_ID = new Parameter() {
        @Override
        public String name() {
            return "vaultId";
        }

        @Override
        public boolean isSensitive() {
            return false;
        }
    };
    private static final AtomicInteger INVOCATIONS = new AtomicInteger();
    private static final AtomicReference<CharSequence> VAULT_ID_VALUE = new AtomicReference<>();

    @Override
    public char[] getPassword(Map<Parameter, CharSequence> map) {
        INVOCATIONS.incrementAndGet();
        VAULT_ID_VALUE.set(map.get(VAULT_ID));
        return "test".toCharArray();
    }

    @Override
    public Collection<? extends Parameter> getParameters() {
        return List.of(VAULT_ID);
    }

    @Override
    public String getName() {
        return "example-provider";
    }

    public static void reset() {
        INVOCATIONS.set(0);
        VAULT_ID_VALUE.set(null);
    }

    public static int invocations() {
        return INVOCATIONS.get();
    }

    public static CharSequence vaultId() {
        return VAULT_ID_VALUE.get();
    }
}
