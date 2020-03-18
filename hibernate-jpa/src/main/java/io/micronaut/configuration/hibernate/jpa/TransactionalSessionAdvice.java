package io.micronaut.configuration.hibernate.jpa;

import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Type;
import io.micronaut.core.annotation.Internal;
import io.micronaut.transaction.jdbc.TransactionalConnection;
import io.micronaut.transaction.jdbc.TransactionalConnectionInterceptor;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An introduction advice annotation used to create a transaction aware session.
 * Considered internal and not for explicit usage.
 *
 * @see TransactionalConnectionInterceptor
 * @see TransactionalConnection
 */
@Retention(RUNTIME)
@Introduction
@Type(TransactionalSessionInterceptor.class)
@Internal
@interface TransactionalSessionAdvice {
}
