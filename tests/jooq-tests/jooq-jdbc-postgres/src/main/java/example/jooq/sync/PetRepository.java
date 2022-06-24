package example.jooq.sync;

import example.sync.IPetRepository;
import example.domain.IPet;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class PetRepository implements IPetRepository {

    private final DSLContext db;
    private final OwnerRepository ownerRepository;

    private final static Table<Record> PET_TABLE = DSL.table("pets");
    private final static Field<Long> PET_ID = DSL.field("id", SQLDataType.BIGINT.identity(true));
    private final static Field<String> PET_NAME = DSL.field("name", SQLDataType.VARCHAR(200));
    private final static Field<String> PET_TYPE = DSL.field("type", SQLDataType.VARCHAR(200));
    private final static Field<Long> PET_OWNER = DSL.field("owner", SQLDataType.BIGINT);

    public PetRepository(DSLContext db, OwnerRepository ownerRepository) {
        this.db = db;
        this.ownerRepository = ownerRepository;
    }

    @PostConstruct
    public void init() {
        runInit();
    }

    @Transactional
    public void runInit() {
        db.createTable(PET_TABLE).column(PET_ID).column(PET_NAME).column(PET_TYPE).column(PET_OWNER).execute();
    }

    @Override
    public IPet create() {
        return new Pet();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    @Override
    public void save(IPet pet) {
        Long id = db.insertInto(PET_TABLE)
                .columns(PET_NAME, PET_TYPE, PET_OWNER)
                .values(pet.getName(), pet.getType() == null ? null : pet.getType().name(), pet.getOwner().getId())
                .returning(PET_ID)
                .fetchOne()
                .get(PET_ID);
        pet.setId(id);
    }

    @Override
    public Collection<IPet> findAll() {
        return selectPets().stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public Optional<IPet> findByName(String name) {
        return selectPets().where(PET_NAME.eq(name)).stream().map(this::map).findFirst();
    }

    private SelectJoinStep<Record4<Long, String, String, Long>> selectPets() {
        return db.select(PET_ID, PET_NAME, PET_TYPE, PET_OWNER).from(PET_TABLE);
    }

    private IPet map(Record4<Long, String, String, Long> record) {
        return new Pet(record.get(PET_ID),
                record.get(PET_NAME),
                Optional.ofNullable(record.get(PET_TYPE)).map(IPet.PetType::valueOf).orElse(null),
                (Owner) ownerRepository.findById(record.get(PET_OWNER)));
    }
}
