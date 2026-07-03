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
package example.micronaut.genre

import example.micronaut.domain.Genre
import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

interface GenreMapper {

    @Select("select * from genre where id=#{id}")
    fun findById(id: Long): Genre?

    @Insert("insert into genre(name) values(#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun save(genre: Genre)

    @Delete("delete from genre where id=#{id}")
    fun deleteById(id: Long)

    @Update("update genre set name=#{name} where id=#{id}")
    fun update(@Param("id") id: Long, @Param("name") name: String?)

    @Select("select * from genre")
    fun findAll(): List<Genre>

    @Select("select * from genre order by \${sort} \${order}")
    fun findAllBySortAndOrder(@Param("sort") @Pattern(regexp = "id|name") sort: String,
                              @Param("order") @Pattern(regexp = "asc|ASC|desc|DESC") order: String): List<Genre>

    @Select("select * from genre order by \${sort} \${order} limit \${offset}, \${max}")
    fun findAllByOffsetAndMaxAndSortAndOrder(@Param("offset") @PositiveOrZero offset: Int,
                                             @Param("max") @Positive max: Int,
                                             @Param("sort") @Pattern(regexp = "id|name") sort: String,
                                             @Param("order") @Pattern(regexp = "asc|ASC|desc|DESC") order: String): List<Genre>

    @Select("select * from genre limit \${offset}, \${max}")
    fun findAllByOffsetAndMax(@Param("offset") @PositiveOrZero offset: Int,
                              @Param("max") @Positive max: Int): List<Genre>
}
