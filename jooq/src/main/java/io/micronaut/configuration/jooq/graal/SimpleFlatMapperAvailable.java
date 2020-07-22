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
package io.micronaut.configuration.jooq.graal;

import io.micronaut.core.annotation.Internal;

import java.util.function.BooleanSupplier;

/**
 * Companion class for DefaultReflectionServiceSubstitute.
 *
 * @author Lukas Moravec
 * @since 2.3.5
 */
@Internal
public class SimpleFlatMapperAvailable implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("org.simpleflatmapper.reflect.DefaultReflectionService");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
