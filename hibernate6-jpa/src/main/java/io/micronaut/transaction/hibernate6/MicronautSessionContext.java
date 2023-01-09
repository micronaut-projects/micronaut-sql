/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.transaction.hibernate6;

import io.micronaut.core.annotation.TypeHint;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.context.spi.CurrentSessionContext;

/**
 * TODO: This is added temporary just to be able to avoid dependency on hibernate tx dependency in micronaut-data.
 * Remove as soon as lib can be imported and used actual MicronautSessionContext implementation.
 *
 */
@SuppressWarnings("serial")
@TypeHint(MicronautSessionContext.class)
public final class MicronautSessionContext implements CurrentSessionContext {
    @Override
    public Session currentSession() throws HibernateException {
        return null;
    }
}
