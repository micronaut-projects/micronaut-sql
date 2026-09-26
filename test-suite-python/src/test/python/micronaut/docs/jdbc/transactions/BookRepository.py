# tag::imports[]
from jakarta.inject import Singleton
from jakarta.transaction import Transactional
from javax.sql import DataSource

from micronaut.docs.jdbc.transactions.Book import Book
# end::imports[]


# tag::clazz[]
@Singleton
class BookRepository:
    def __init__(self, data_source: DataSource):  # <1>
        self.data_source = data_source

    @Transactional  # <2>
    def save_book(self, book: Book) -> None:
        connection = self.data_source.getConnection()  # <3>
        try:
            statement = connection.prepareStatement("INSERT INTO books (title, pages) VALUES (?, ?)")
            statement.setString(1, book.title)
            statement.setInt(2, book.pages)
            statement.executeUpdate()
            statement.close()
        finally:
            connection.close()
    # end::clazz[]

    @Transactional
    def create_table(self) -> None:
        connection = self.data_source.getConnection()
        try:
            statement = connection.createStatement()
            statement.execute("CREATE TABLE IF NOT EXISTS books (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255) NOT NULL, pages INT NOT NULL)")
            statement.close()
        finally:
            connection.close()

    @Transactional
    def count(self) -> int:
        connection = self.data_source.getConnection()
        try:
            statement = connection.createStatement()
            result_set = statement.executeQuery("SELECT COUNT(*) FROM books")
            result_set.next()
            count = result_set.getInt(1)
            statement.close()
            return count
        finally:
            connection.close()
    # tag::clazz[]
# end::clazz[]
