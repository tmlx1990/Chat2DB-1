package ai.chat2db.plugin.tdengine.type;

import ai.chat2db.spi.model.Charset;

import java.util.Arrays;
import java.util.List;

public enum TDengineCharsetEnum {

    UTF8("utf8", "utf8_general_ci");
    private Charset charset;

    TDengineCharsetEnum(String charsetName, String defaultCollationName) {
        this.charset = new Charset(charsetName, defaultCollationName);
    }


    public Charset getCharset() {
        return charset;
    }

    public static List<Charset> getCharsets() {
        return Arrays.stream(TDengineCharsetEnum.values()).map(TDengineCharsetEnum::getCharset).collect(java.util.stream.Collectors.toList());
    }

}
