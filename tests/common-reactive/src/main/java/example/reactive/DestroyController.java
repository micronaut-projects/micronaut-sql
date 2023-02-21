package example.reactive;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.transaction.annotation.TransactionalAdvice;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller("/destroy")
class DestroyController {

    private final IOwnerRepository ownerRepository;
    private final IPetRepository petRepository;

    DestroyController(IOwnerRepository ownerRepository, IPetRepository petRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
    }

    @Get
    @TransactionalAdvice
    Mono<Void> destroy() {
        return Flux.concat(
            petRepository.destroy(),
            ownerRepository.destroy()
        ).then();
    }
}
