package io.micronaut.docs.jdbc.refresh

import jakarta.inject.Singleton

import java.util.concurrent.atomic.AtomicReference

/**
 * Stands in for an external secret service (Vault, a cloud secret manager, ...) in the documentation example.
 */
@Singleton
class DbSecretStore {

    private final AtomicReference<String> password = new AtomicReference<>()

    String currentPassword() {
        password.get()
    }

    void rotate(String newPassword) {
        password.set(newPassword)
    }
}
