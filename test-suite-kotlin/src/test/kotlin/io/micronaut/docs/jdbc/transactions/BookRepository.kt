package io.micronaut.docs.jdbc.transactions

// tag::imports[]
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import javax.sql.DataSource
// end::imports[]

// tag::clazz[]
@Singleton
open class BookRepository(private val dataSource: DataSource) { // <1>

    @Transactional // <2>
    open fun saveBook(book: Book) {
        dataSource.connection.use { connection -> // <3>
            connection.prepareStatement("INSERT INTO books (title, pages) VALUES (?, ?)").use { statement ->
                statement.setString(1, book.title)
                statement.setInt(2, book.pages)
                statement.executeUpdate()
            }
        }
    }
    // end::clazz[]

    @Transactional
    open fun createTable() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE IF NOT EXISTS books (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255) NOT NULL, pages INT NOT NULL)")
            }
        }
    }

    @Transactional
    open fun count(): Int {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM books").use { resultSet ->
                    resultSet.next()
                    return resultSet.getInt(1)
                }
            }
        }
    }
    // tag::clazz[]
}
// end::clazz[]
