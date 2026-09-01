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
import jakarta.inject.Singleton;
import org.apache.ibatis.session.Configuration;

@Singleton
final class TestGeneratedMapperScanRegistration implements MyBatisMapperScanRegistration {

    @Override
    public String getCustomizerType() {
        return TestGeneratedMapperScanCustomizer.class.getName();
    }

    @Override
    public void register(Configuration configuration) {
        configuration.addMapper(TestGeneratedMapper.class);
    }
}
