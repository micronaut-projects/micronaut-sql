package io.micronaut.configuration.hibernate.jpa.mapping;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.configuration.hibernate.jpa.scope.CurrentSession;
import io.micronaut.transaction.annotation.TransactionalAdvice;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;

@TransactionalAdvice
@Singleton
public class AccountRepository {

    private final EntityManager entityManager;

    public AccountRepository(@CurrentSession EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Account create(@NotBlank String username, @NotBlank String password) {
        Account account = new Account(username, password);
        entityManager.persist(account);
        return account;
    }

    @Nullable
    public Account findByUsername(String username) {
        return (Account) entityManager
                .createNativeQuery("SELECT * FROM custom_view WHERE username = :username", Account.class)
                .setParameter("username", username)
                .getSingleResult();
    }

}
