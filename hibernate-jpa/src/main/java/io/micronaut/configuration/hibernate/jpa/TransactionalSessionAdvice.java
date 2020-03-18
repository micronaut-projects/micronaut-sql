package io.micronaut.configuration.hibernate.jpa;

import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Type;
import io.micronaut.core.annotation.Internal;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An introduction advice annotation used to create a transaction aware session.
 * Considered internal and not for explicit usage.
 *
 * @see TransactionalSessionInterceptor
 * @see TransactionalSession
 */
@Retention(RUNTIME)
@Introduction
@Type(TransactionalSessionInterceptor.class)
@Internal
@interface TransactionalSessionAdvice {
}
