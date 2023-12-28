package ai.chat2db.plugin.tdengine.type;

import ai.chat2db.spi.model.Collation;

import java.util.Arrays;
import java.util.List;

public enum TDengineCollationEnum {

    UTF8_GENERAL_CI("utf8_general_ci"),
    ;
    private Collation collation;

    TDengineCollationEnum(String collationName) {
        this.collation = new Collation(collationName);
    }

    public Collation getCollation() {
        return collation;
    }


    public static List<Collation> getCollations() {
        return Arrays.asList(TDengineCollationEnum.values()).stream().map(TDengineCollationEnum::getCollation).collect(java.util.stream.Collectors.toList());
    }

}
