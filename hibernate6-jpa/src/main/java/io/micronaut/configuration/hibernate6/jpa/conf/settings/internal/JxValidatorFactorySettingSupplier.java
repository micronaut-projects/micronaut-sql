/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.configuration.hibernate6.jpa.conf.settings.internal;

import io.micronaut.configuration.hibernate6.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate6.jpa.conf.settings.SettingsSupplier;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import org.hibernate.cfg.AvailableSettings;

import javax.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Map;

/**
 * Validation factory settings supplier.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Requires(classes = ValidatorFactory.class, bean = ValidatorFactory.class)
@Prototype
final class JxValidatorFactorySettingSupplier implements SettingsSupplier {

    private final ValidatorFactory validatorFactory;

    JxValidatorFactorySettingSupplier(ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    @Override
    public Map<String, Object> supply(JpaConfiguration jpaConfiguration) {
        return Collections.singletonMap(AvailableSettings.JPA_VALIDATION_FACTORY, validatorFactory);
    }
}
