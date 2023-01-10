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
package io.micronaut.sql.hibernate.jpa;

import org.hibernate.context.spi.CurrentSessionContext;

/**
 * Provides the value for {@link org.hibernate.cfg.AvailableSettings#CURRENT_SESSION_CONTEXT_CLASS}.
 *
 * @author Denis Stepanov
 * @since 3.3.2
 */
public interface HibernateCurrentSessionContextClassProvider {

    /**
     * @return the class implementing {@link CurrentSessionContext}
     */
    Class<? extends CurrentSessionContext> get();

}
