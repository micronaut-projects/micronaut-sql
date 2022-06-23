package reactive;

import example.domain.IOwner;
import example.domain.IPet;

public class Pet implements IPet {

    private Long id;
    private String name;
    private PetType type;
    private Owner owner;

    public Pet() {
    }

    public Pet(Long id, String name, PetType type, Owner owner) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.owner = owner;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Owner getOwner() {
        return owner;
    }

    @Override
    public void setOwner(IOwner owner) {
        this.owner = (Owner) owner;
    }

    @Override
    public PetType getType() {
        return type;
    }

    @Override
    public void setType(PetType type) {
        this.type = type;
    }
}
