/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.mybatis;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requests compile-time discovery of MyBatis mapper interfaces.
 *
 * <p>The {@code micronaut-mybatis-processor} generates a {@link MyBatisMapperScanRegistration}
 * for every annotated type. The registration adds the mapper interfaces found in {@link #value()}
 * and listed in {@link #mappers()} to the MyBatis {@code Configuration} of the {@link #datasource()}
 * without runtime classpath scanning, which makes the registration work in GraalVM native images.</p>
 *
 * <p>The annotation can be placed on any type, it does not have to be a bean. Only mapper interfaces
 * compiled together with the annotated type are discovered by package; mapper interfaces from other
 * modules must be listed in {@link #mappers()}.</p>
 *
 * @since 7.2.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyBatisMapperScan {

    /**
     * @return Packages containing MyBatis mapper interfaces. Sub-packages and nested interfaces are included.
     */
    String[] value() default {};

    /**
     * @return The datasource name to which the discovered mappers are registered
     */
    String datasource() default "default";

    /**
     * @return Mapper interfaces to register directly
     */
    Class<?>[] mappers() default {};

    /**
     * Whether to generate the GraalVM native image metadata (dynamic proxy entries for the mapper interfaces
     * and reflection entries for their result and parameter types), using the same mechanism as
     * {@code @ReflectiveAccess}. The metadata is ignored on the JVM, so it only needs to be disabled when a
     * project wants to manage the native image configuration itself.
     *
     * @return Whether to generate GraalVM native image metadata
     */
    boolean nativeImageMetadata() default true;
}
