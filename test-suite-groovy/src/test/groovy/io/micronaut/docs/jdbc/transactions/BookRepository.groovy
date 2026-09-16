package io.micronaut.docs.jdbc.transactions

// tag::imports[]
import jakarta.inject.Singleton
import jakarta.transaction.Transactional

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
// end::imports[]

// tag::clazz[]
@Singleton
class BookRepository {

    private final DataSource dataSource

    BookRepository(DataSource dataSource) { // <1>
        this.dataSource = dataSource
    }

    @Transactional // <2>
    void saveBook(Book book) {
        try (Connection connection = dataSource.connection // <3>
             PreparedStatement statement = connection.prepareStatement("INSERT INTO books (title, pages) VALUES (?, ?)")) {
            statement.setString(1, book.title)
            statement.setInt(2, book.pages)
            statement.executeUpdate()
        }
    }
    // end::clazz[]

    @Transactional
    void createTable() {
        try (Connection connection = dataSource.connection
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS books (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255) NOT NULL, pages INT NOT NULL)")
        }
    }

    @Transactional
    int count() {
        try (Connection connection = dataSource.connection
             Statement statement = connection.createStatement()
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM books")) {
            resultSet.next()
            return resultSet.getInt(1)
        }
    }
    // tag::clazz[]
}
// end::clazz[]
