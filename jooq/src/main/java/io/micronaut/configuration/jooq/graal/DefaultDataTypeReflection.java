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
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.Result;
import org.jooq.RowId;
import org.jooq.types.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;

@TypeHint(accessType = AccessType.ALL_PUBLIC_CONSTRUCTORS, value = {
        LocalDate[].class,
        LocalDateTime[].class,
        LocalTime[].class,
        ZonedDateTime[].class,
        OffsetDateTime[].class,
        OffsetTime[].class,
        Instant[].class,
        Timestamp[].class,
        BigInteger[].class,
        BigDecimal[].class,
        UNumber[].class,
        UByte[].class,
        UInteger[].class,
        ULong[].class,
        Unsigned[].class,
        UShort[].class,
        YearToMonth[].class,
        YearToSecond[].class,
        RowId[].class,
        Result[].class,
        JSON[].class,
        JSONB[].class
})
final class DefaultDataTypeReflection {

}
