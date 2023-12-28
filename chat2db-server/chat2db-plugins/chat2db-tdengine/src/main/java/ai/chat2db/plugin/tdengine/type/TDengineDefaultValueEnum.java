package ai.chat2db.plugin.tdengine.type;

import ai.chat2db.spi.model.DefaultValue;

import java.util.Arrays;
import java.util.List;

public enum TDengineDefaultValueEnum {

    NULL("NULL"),
    CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
    ;
    private DefaultValue defaultValue;

    TDengineDefaultValueEnum(String defaultValue) {
        this.defaultValue = new DefaultValue(defaultValue);
    }


    public DefaultValue getDefaultValue() {
        return defaultValue;
    }

    public static List<DefaultValue> getDefaultValues() {
        return Arrays.stream(TDengineDefaultValueEnum.values()).map(TDengineDefaultValueEnum::getDefaultValue).collect(java.util.stream.Collectors.toList());
    }

}
