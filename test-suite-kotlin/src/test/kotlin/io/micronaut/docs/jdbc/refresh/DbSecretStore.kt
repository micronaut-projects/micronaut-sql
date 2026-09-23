package io.micronaut.docs.jdbc.refresh

import jakarta.inject.Singleton
import java.util.concurrent.atomic.AtomicReference

/**
 * Stands in for an external secret service (Vault, a cloud secret manager, ...) in the documentation example.
 */
@Singleton
class DbSecretStore {

    private val password = AtomicReference<String>()

    fun currentPassword(): String? = password.get()

    fun rotate(newPassword: String) = password.set(newPassword)
}
