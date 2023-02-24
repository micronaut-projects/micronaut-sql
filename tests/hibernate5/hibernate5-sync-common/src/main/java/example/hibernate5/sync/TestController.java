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
package example.hibernate5.sync;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/test-proxies")
class TestController {

    @Get
    void init() {
        try {
            Class.forName("net.bytebuddy.ByteBuddy");
            throw new IllegalStateException("ByteBuddy shouldn't be present on classpath");
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        try {
            Class.forName("javassist.util.proxy.ProxyFactory");
            throw new IllegalStateException("Javassist shouldn't be present on classpath");
        } catch (ClassNotFoundException e) {
            // Ignore
        }
    }

}
