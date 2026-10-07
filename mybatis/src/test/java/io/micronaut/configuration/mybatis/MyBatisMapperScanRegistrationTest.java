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

import io.micronaut.configuration.mybatis.generated.TestGeneratedMapper;
import io.micronaut.configuration.mybatis.generated.TestOtherDataSourceMapper;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MyBatisMapperScanRegistrationTest {

    private final MyBatisMapperScanRegistration registration = new MyBatisMapperScanRegistration() {
        @Override
        public String getDatasourceName() {
            return "default";
        }

        @Override
        public void register(Configuration configuration) {
            // an explicitly listed mapper that is also found by the package fallback
            addMapper(configuration, TestGeneratedMapper.class);
            addMappers(configuration, TestGeneratedMapper.class.getPackageName());
        }
    };

    @Test
    void packageFallbackSkipsAlreadyRegisteredMappers() {
        Configuration configuration = new Configuration();
        registration.register(configuration);
        assertTrue(configuration.hasMapper(TestGeneratedMapper.class));
        assertTrue(configuration.hasMapper(TestOtherDataSourceMapper.class));

        // and is idempotent
        registration.register(configuration);
    }
}
