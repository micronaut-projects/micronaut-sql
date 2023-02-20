package example.jooq.sync;

import example.sync.IOwnerRepository;
import example.domain.IOwner;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import jakarta.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class OwnerRepository implements IOwnerRepository {

    private final DSLContext db;

    private final static Table<Record> OWNER_TABLE = DSL.table("owners");
    private final static Field<Long> OWNER_ID = DSL.field("id", SQLDataType.BIGINT.identity(true));
    private final static Field<String> OWNER_NAME = DSL.field("name", SQLDataType.VARCHAR(200));
    private final static Field<Integer> OWNER_AGE = DSL.field("age", SQLDataType.INTEGER);

    public OwnerRepository(DSLContext db) {
        this.db = db;
    }

    @PostConstruct
    public void init() {
        runInit();
    }

    @Transactional
    public void runInit() {
        db.createTable(OWNER_TABLE).column(OWNER_ID).column(OWNER_NAME).column(OWNER_AGE).execute();
    }

    @Override
    public Owner create() {
        return new Owner();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    @Override
    public void save(IOwner owner) {
        Long id = db.insertInto(OWNER_TABLE)
                .columns(OWNER_NAME, OWNER_AGE)
                .values(owner.getName(), owner.getAge())
                .returning(OWNER_ID)
                .fetchOne()
                .get(OWNER_ID);
        owner.setId(id);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    @Override
    public void delete(IOwner owner) {
        db.deleteFrom(OWNER_TABLE).where(OWNER_ID.eq(owner.getId())).execute();
    }

    @Override
    public IOwner findById(Long id) {
        return selectPets().where(OWNER_ID.eq(id)).stream().map(this::map).findFirst().get();
    }

    @Override
    public Collection<IOwner> findAll() {
        return selectPets().stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public Optional<IOwner> findByName(String name) {
        return selectPets().where(OWNER_NAME.eq(name)).stream().map(this::map).findFirst();
    }

    private SelectJoinStep<Record3<Long, String, Integer>> selectPets() {
        return db.select(OWNER_ID, OWNER_NAME, OWNER_AGE).from(OWNER_TABLE);
    }

    private IOwner map(Record3<Long, String, Integer> record) {
        return new Owner(record.get(OWNER_ID),
                record.get(OWNER_NAME),
                record.get(OWNER_AGE));
    }
}
