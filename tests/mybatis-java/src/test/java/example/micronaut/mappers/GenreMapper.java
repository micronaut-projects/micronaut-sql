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
package example.micronaut.mappers;

import example.micronaut.domain.Genre;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public interface GenreMapper {

    @Select("select * from genre where id=#{id}")
    Genre findById(long id);

    @Insert("insert into genre(name) values(#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(Genre genre);

    @Delete("delete from genre where id=#{id}")
    void deleteById(long id);

    @Update("update genre set name=#{name} where id=#{id}")
    void update(@Param("id") long id, @Param("name") String name);

    @Select("select * from genre")
    List<Genre> findAll();

    @Select("select * from genre order by ${sort} ${order}")
    List<Genre> findAllBySortAndOrder(@Param("sort") @NotNull @Pattern(regexp = "id|name") String sort,
                                      @Param("order") @NotNull @Pattern(regexp = "asc|ASC|desc|DESC") String order);

    @Select("select * from genre order by ${sort} ${order} limit ${offset}, ${max}")
    List<Genre> findAllByOffsetAndMaxAndSortAndOrder(@Param("offset") @PositiveOrZero int offset,
                                                     @Param("max") @Positive int max,
                                                     @Param("sort") @NotNull @Pattern(regexp = "id|name") String sort,
                                                     @Param("order") @NotNull @Pattern(regexp = "asc|ASC|desc|DESC") String order);

    @Select("select * from genre limit ${offset}, ${max}")
    List<Genre> findAllByOffsetAndMax(@Param("offset") @PositiveOrZero int offset, @Param("max") @Positive int max);
}
