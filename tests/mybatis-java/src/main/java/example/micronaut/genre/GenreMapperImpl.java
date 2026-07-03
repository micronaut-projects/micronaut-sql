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
package example.micronaut.genre;

import example.micronaut.domain.Genre;
import jakarta.inject.Singleton;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

@Singleton // <1>
public class GenreMapperImpl implements GenreMapper {

    private final SqlSessionFactory sqlSessionFactory; // <2>

    public GenreMapperImpl(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory; // <2>
    }

    @Override
    public Genre findById(long id) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) { // <3>
            return getGenreMapper(sqlSession).findById(id); // <5>
        }
    }

    @Override
    public void save(Genre genre) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            getGenreMapper(sqlSession).save(genre);
            sqlSession.commit(); // <6>
        }
    }

    @Override
    public void deleteById(long id) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            getGenreMapper(sqlSession).deleteById(id);
            sqlSession.commit();
        }
    }

    @Override
    public void update(long id, String name) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            getGenreMapper(sqlSession).update(id, name);
            sqlSession.commit();
        }
    }

    @Override
    public List<Genre> findAll() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return getGenreMapper(sqlSession).findAll();
        }
    }

    @Override
    public List<Genre> findAllBySortAndOrder(@NotNull @Pattern(regexp = "id|name") String sort,
                                             @NotNull @Pattern(regexp = "asc|ASC|desc|DESC") String order) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return getGenreMapper(sqlSession).findAllBySortAndOrder(sort, order);
        }
    }

    @Override
    public List<Genre> findAllByOffsetAndMaxAndSortAndOrder(@PositiveOrZero int offset,
                                                            @Positive int max,
                                                            @NotNull @Pattern(regexp = "id|name") String sort,
                                                            @NotNull @Pattern(regexp = "asc|ASC|desc|DESC") String order) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return getGenreMapper(sqlSession).findAllByOffsetAndMaxAndSortAndOrder(offset, max, sort, order);
        }
    }

    @Override
    public List<Genre> findAllByOffsetAndMax(@PositiveOrZero int offset,
                                             @Positive int max) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return getGenreMapper(sqlSession).findAllByOffsetAndMax(offset, max);
        }
    }

    private GenreMapper getGenreMapper(SqlSession sqlSession) {
        return sqlSession.getMapper(GenreMapper.class); // <4>
    }
}
