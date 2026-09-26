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
package example.micronaut

import io.micronaut.serde.annotation.Serdeable
import io.micronaut.http.uri.UriBuilder
import java.net.URI
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

@Serdeable
class ListingArguments(
    @field:PositiveOrZero var offset: Int? = 0,
    @field:Positive var max: Int? = null,
    @field:Pattern(regexp = "id|name") var sort: String? = null,
    @field:Pattern(regexp = "asc|ASC|desc|DESC") var order: String? = null
) {
    fun of(uriBuilder: UriBuilder): URI {
        max?.let { uriBuilder.queryParam("max", it) }
        order?.let { uriBuilder.queryParam("order", it) }
        offset?.let { uriBuilder.queryParam("offset", it) }
        sort?.let { uriBuilder.queryParam("sort", it) }
        return uriBuilder.build()
    }

    class Builder {
        private val args = ListingArguments()

        fun max(max: Int): Builder = apply { args.max = max }
        fun sort(sort: String?): Builder = apply { args.sort = sort }
        fun order(order: String?): Builder = apply { args.order = order }
        fun offset(offset: Int): Builder = apply { args.offset = offset }
        fun build(): ListingArguments = args
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
