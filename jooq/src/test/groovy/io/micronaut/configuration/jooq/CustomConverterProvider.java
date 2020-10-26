/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.configuration.jooq;

import org.jetbrains.annotations.Nullable;
import org.jooq.Converter;
import org.jooq.ConverterProvider;
import org.jooq.impl.DefaultConverterProvider;

import javax.inject.Named;
import javax.inject.Singleton;

public class CustomConverterProvider implements ConverterProvider {

    private final DefaultConverterProvider delegate = new DefaultConverterProvider();

    @Override
    public @Nullable <T, U> Converter<T, U> provide(Class<T> tType, Class<U> uType) {
        return delegate.provide(tType, uType);
    }

}
