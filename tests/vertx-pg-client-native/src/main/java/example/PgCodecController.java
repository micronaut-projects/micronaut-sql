package example;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.pgclient.impl.codec.DataType;
import io.vertx.pgclient.impl.codec.DataTypeCodec;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Controller("/postgres")
final class PgCodecController {

    @Get("/codec")
    String codec() {
        return decodeTimestamp("infinity") == LocalDateTime.MAX
            && decodeTimestamp("-infinity") == LocalDateTime.MIN ? "ok" : "invalid";
    }

    private static Object decodeTimestamp(String value) {
        ByteBuf buffer = Unpooled.copiedBuffer(value, StandardCharsets.UTF_8);
        try {
            return DataTypeCodec.decodeText(DataType.TIMESTAMP, 0, buffer.readableBytes(), buffer);
        } finally {
            buffer.release();
        }
    }
}
