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
package io.micronaut.configuration.hibernate6.jpa.proxy;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;

/**
 * Default delegating interceptor for all {@link GenerateProxy} proxies.
 *
 * @author Denis Stepanov
 * @since 3.3.0
 */
@Internal
@Prototype
public final class IntroducedHibernateProxyAdvice implements MethodInterceptor<Object, Object> {

    private static final String INITIALIZE_PROXY_METHOD = "$registerInterceptor";

    private MethodInterceptor<Object, Object> interceptor;

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        if (interceptor == null) {
            if (INITIALIZE_PROXY_METHOD.equals(context.getMethodName())) {
                interceptor = (MethodInterceptor<Object, Object>) context.getParameterValues()[0];
                return null;
            }
            return context.proceed();
        }
        return interceptor.intercept(context);
    }

}
