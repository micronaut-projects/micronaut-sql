package example.jdbc.hikari.sqlite;

import example.domain.IPet;
import example.sync.IPetRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Singleton
public class PetRepository implements IPetRepository {

    private final DataSource dataSource;
    private final OwnerRepository ownerRepository;

    public PetRepository(DataSource dataSource, OwnerRepository ownerRepository) {
        this.dataSource = dataSource;
        this.ownerRepository = ownerRepository;
    }

    @PostConstruct
    public void init() throws SQLException {
        runInit();
    }

    @Transactional
    public void runInit() throws SQLException {
        PreparedStatement stmt = dataSource.getConnection().prepareStatement("""
            CREATE TABLE IF NOT EXISTS pets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR(200) NOT NULL,
                type VARCHAR(200),
                owner INTEGER NOT NULL
            )""");
        stmt.executeUpdate();
    }

    @Override
    public IPet create() {
        return new Pet();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    @Override
    public void save(IPet pet) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO pets (name, type, owner) VALUES (?, ?, ?)",
                 new String[]{"id"})) {
            stmt.setString(1, pet.getName());
            stmt.setString(2, pet.getType() != null ? pet.getType().name() : null);
            stmt.setLong(3, pet.getOwner().getId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    pet.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert pet", e);
        }
    }

    @Transactional(Transactional.TxType.MANDATORY)
    @Override
    public void delete(IPet pet) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM pets WHERE id = ?")) {
            stmt.setLong(1, pet.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete pet", e);
        }
    }

    @Override
    public Collection<IPet> findAll() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, type, owner FROM pets");
             ResultSet rs = stmt.executeQuery()) {
            List<IPet> resultList = new ArrayList<>();
            while (rs.next()) {
                resultList.add(map(rs));
            }
            return resultList;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all pets", e);
        }
    }

    @Override
    public Optional<IPet> findByName(String name) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, type, owner FROM pets WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find pet by name", e);
        }
    }

    private IPet map(ResultSet rs) throws SQLException {
        return new Pet(rs.getLong("id"),
            rs.getString("name"),
            Optional.ofNullable(rs.getString("type")).map(IPet.PetType::valueOf).orElse(null),
            (Owner) ownerRepository.findById(rs.getLong("owner")));
    }
}
