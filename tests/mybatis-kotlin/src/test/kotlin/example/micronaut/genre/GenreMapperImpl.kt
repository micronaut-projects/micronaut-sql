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
import jakarta.inject.Singleton
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

@Singleton // <1>
open class GenreMapperImpl(private val sqlSessionFactory: SqlSessionFactory) : GenreMapper { // <2>

    override fun findById(id: Long): Genre? {
        sqlSessionFactory.openSession().use { sqlSession ->  // <3>
            return getGenreMapper(sqlSession).findById(id) // <5>
        }
    }

    override fun save(genre: Genre) {
        sqlSessionFactory.openSession().use { sqlSession ->
            getGenreMapper(sqlSession).save(genre)
            sqlSession.commit() // <6>
        }
    }

    override fun deleteById(id: Long) {
        sqlSessionFactory.openSession().use { sqlSession ->
            getGenreMapper(sqlSession).deleteById(id)
            sqlSession.commit()
        }
    }

    override fun update(id: Long, name: String?) {
        sqlSessionFactory.openSession().use { sqlSession ->
            getGenreMapper(sqlSession).update(id, name)
            sqlSession.commit()
        }
    }

    override fun findAll(): List<Genre> {
        sqlSessionFactory.openSession().use { sqlSession -> return getGenreMapper(sqlSession).findAll() }
    }

    override fun findAllBySortAndOrder(@Pattern(regexp = "id|name") sort: String,
                                       @Pattern(regexp = "asc|ASC|desc|DESC") order: String): List<Genre> {
        sqlSessionFactory.openSession().use { sqlSession ->
            return getGenreMapper(sqlSession).findAllBySortAndOrder(sort, order)
        }
    }

    override fun findAllByOffsetAndMaxAndSortAndOrder(@PositiveOrZero offset: Int,
                                                      @Positive max: Int,
                                                      @Pattern(regexp = "id|name") sort: String,
                                                      @Pattern(regexp = "asc|ASC|desc|DESC") order: String): List<Genre> {
        sqlSessionFactory.openSession().use { sqlSession ->
            return getGenreMapper(sqlSession).findAllByOffsetAndMaxAndSortAndOrder(offset, max, sort, order)
        }
    }

    override fun findAllByOffsetAndMax(@PositiveOrZero offset: Int,
                                       @Positive max: Int): List<Genre> {
        sqlSessionFactory.openSession().use { sqlSession ->
            return getGenreMapper(sqlSession).findAllByOffsetAndMax(offset, max)
        }
    }

    private fun getGenreMapper(sqlSession: SqlSession): GenreMapper {
        return sqlSession.getMapper(GenreMapper::class.java) // <4>
    }
}
