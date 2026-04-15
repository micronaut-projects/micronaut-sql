package example;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.vertx.pgclient.impl.codec.DataTypeCodec;

@Controller("/postgres")
final class PgCodecController {

    @Get("/codec")
    String codec() {
        return DataTypeCodec.LDT_PLUS_INFINITY.getYear() > 0
            && DataTypeCodec.LDT_MINUS_INFINITY.getYear() < 0 ? "ok" : "invalid";
    }
}
