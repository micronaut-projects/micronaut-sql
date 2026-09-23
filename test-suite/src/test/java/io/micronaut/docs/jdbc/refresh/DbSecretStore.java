package io.micronaut.docs.jdbc.refresh;

import jakarta.inject.Singleton;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Stands in for an external secret service (Vault, a cloud secret manager, ...) in the documentation example.
 */
@Singleton
public class DbSecretStore {

    private final AtomicReference<String> password = new AtomicReference<>();

    public String currentPassword() {
        return password.get();
    }

    public void rotate(String newPassword) {
        password.set(newPassword);
    }
}
