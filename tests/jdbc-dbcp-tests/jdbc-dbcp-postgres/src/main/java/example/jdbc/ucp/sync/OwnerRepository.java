package example.jdbc.ucp.sync;

import example.domain.IOwner;
import example.sync.IOwnerRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Singleton
public class OwnerRepository implements IOwnerRepository {

    private final DataSource dataSource;

    public OwnerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() throws SQLException {
        runInit();
    }

    @Transactional
    public void runInit() throws SQLException {
        PreparedStatement stmt = dataSource.getConnection().prepareStatement("""
            CREATE TABLE owners (id INTEGER GENERATED ALWAYS AS IDENTITY NOT NULL,
                                 name VARCHAR(200) NOT NULL,
                                 age INTEGER NOT NULL,
                                 PRIMARY KEY (id))""");
        stmt.executeUpdate();
    }

    @Override
    public Owner create() {
        return new Owner();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    @Override
    public void save(IOwner owner) {
        try {
            PreparedStatement stmt = dataSource.getConnection().prepareStatement("INSERT INTO owners (name, age) VALUES (?, ?)",
                new String[]{"id"});
            stmt.setString(1, owner.getName());
            stmt.setInt(2, owner.getAge());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                owner.setId(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert owner", e);
        }
    }

    @Transactional(Transactional.TxType.MANDATORY)
    @Override
    public void delete(IOwner owner) {
        try {
            PreparedStatement stmt = dataSource.getConnection().prepareStatement("DELETE FROM owners WHERE id = ?");
            stmt.setLong(1, owner.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete owner", e);
        }
    }

    @Override
    public IOwner findById(Long id) {
        try {
            PreparedStatement stmt = dataSource.getConnection().prepareStatement("SELECT * FROM owners WHERE id = ?");
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find owner by id", e);
        }
    }

    @Override
    public Collection<IOwner> findAll() {
        try {
            PreparedStatement stmt = dataSource.getConnection().prepareStatement("SELECT * FROM owners");
            ResultSet rs = stmt.executeQuery();
            List<IOwner> resultList = new ArrayList<>();
            while (rs.next()) {
                resultList.add(map(rs));
            }
            return resultList;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all owners", e);
        }
    }

    @Override
    public Optional<IOwner> findByName(String name) {
        try {
            PreparedStatement stmt = dataSource.getConnection().prepareStatement("SELECT * FROM owners WHERE name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(map(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find owner by name", e);
        }
    }

    private IOwner map(ResultSet rs) throws SQLException {
        return new Owner(rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("age"));
    }

}
