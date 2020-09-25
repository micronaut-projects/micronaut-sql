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
package example.controllers

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import spock.lang.Specification
import spock.lang.Stepwise

import javax.inject.Inject

@Stepwise
abstract class AbstractAppSpec extends Specification {

    @Inject
    @Client("/")
    RxHttpClient client

    def 'should init'() {
        when:
            client.exchange(HttpRequest.GET("/init")).blockingFirst()
        then:
            noExceptionThrown()
    }

    def 'should fetch owners'() {
        when:
            def results = client.retrieve(HttpRequest.GET("/owners"), List).blockingFirst()
        then:
            results.size() == 2
            results[0].name == "Fred"
            results[1].name == "Barney"
    }

    def 'should fetch owner by name'() {
        when:
            def result = client.retrieve(HttpRequest.GET("/owners/Fred"), Map).blockingFirst()
        then:
            result.name == "Fred"
    }

    def 'should fetch pets'() {
        when:
            def results = client.retrieve(HttpRequest.GET("/pets"), List).blockingFirst()
        then:
            results.size() == 3
            results[0].name == "Dino"
            results[0].owner.name == "Fred"
            results[1].name == "Baby Puss"
            results[1].owner.name == "Fred"
            results[2].name == "Hoppy"
            results[2].owner.name == "Barney"
    }

    def 'should fetch pet by name'() {
        when:
            def result = client.retrieve(HttpRequest.GET("/pets/Dino"), Map).blockingFirst()
        then:
            result.name == "Dino"
            result.owner.name == "Fred"
    }

}
