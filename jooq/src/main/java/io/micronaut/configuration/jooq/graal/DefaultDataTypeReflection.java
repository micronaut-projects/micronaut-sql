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

import io.micronaut.core.annotation.TypeHint;
import io.micronaut.core.annotation.TypeHint.AccessType;

@TypeHint(
        typeNames = {
                "java.time.LocalDate[]",
                "java.time.LocalDateTime[]",
                "java.time.LocalTime[]",
                "java.time.ZonedDateTime[]",
                "java.time.OffsetDateTime[]",
                "java.time.OffsetTime[]",
                "java.time.Instant[]",
                "java.sql.Timestamp[]",
                "java.sql.Date[]",
                "java.sql.Time[]",
                "java.math.BigInteger[]",
                "java.math.BigDecimal[]",
                "org.jooq.types.UNumber[]",
                "org.jooq.types.UByte[]",
                "org.jooq.types.UInteger[]",
                "org.jooq.types.ULong[]",
                "org.jooq.types.Unsigned[]",
                "org.jooq.types.UShort[]",
                "java.lang.Byte[]",
                "java.lang.Integer[]",
                "java.lang.Long[]",
                "java.lang.Float[]",
                "java.lang.Double[]",
                "java.lang.String[]",
                "org.jooq.types.YearToMonth[]",
                "org.jooq.types.YearToSecond[]",
                "org.jooq.types.DayToSecond[]",
                "org.jooq.RowId[]",
                "org.jooq.Result[]",
                "org.jooq.Record[]",
                "org.jooq.JSON[]",
                "org.jooq.JSONB[]",
                "java.util.UUID[]",
                "byte[]",
        },
        accessType = AccessType.ALL_PUBLIC_CONSTRUCTORS
)
final class DefaultDataTypeReflection {

}
