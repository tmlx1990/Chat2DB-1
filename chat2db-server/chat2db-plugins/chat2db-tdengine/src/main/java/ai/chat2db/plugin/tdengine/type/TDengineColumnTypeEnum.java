package ai.chat2db.plugin.tdengine.type;

import ai.chat2db.spi.ColumnBuilder;
import ai.chat2db.spi.enums.EditStatus;
import ai.chat2db.spi.model.ColumnType;
import ai.chat2db.spi.model.TableColumn;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum TDengineColumnTypeEnum implements ColumnBuilder {

    TIMESTAMP("TIMESTAMP", true, false, true, false, false, false, true, true, true, false),

    INT("INT", false, false, true, true, false, false, true, true, false, false),

    INT_UNSIGNED("INT UNSIGNED", false, false, true, true, false, false, true, true, false, false),

    BIGINT("BIGINT", false, false, true, true, false, false, true, true, false, false),

    BIGINT_UNSIGNED("BIGINT UNSIGNED", false, false, true, true, false, false, true, true, false, false),

    FLOAT("FLOAT", true, true, true, false, false, false, true, true, false, false),

    DOUBLE("DOUBLE", true, true, true, false, false, false, true, true, false, false),

    BINARY("BINARY", true, false, true, false, false, false, true, true, false, false),

    SMALLINT("SMALLINT", false, false, true, true, false, false, true, true, false, false),

    SMALLINT_UNSIGNED("SMALLINT UNSIGNED", false, false, true, true, false, false, true, true, false, false),

    TINYINT("TINYINT", true, false, true, true, false, false, true, true, false, false),

    TINYINT_UNSIGNED("TINYINT UNSIGNED", true, false, true, true, false, false, true, true, false, false),

    BOOL("BOOL", false, false, true, true, false, false, true, true, false, false),

    NCHAR("NCHAR", true, false, true, false, false, false, true, true, false, true),

    JSON("JSON", false, false, true, false, false, false, true, true, false, false),

    VARCHAR("VARCHAR", true, false, true, false, true, true, true, true, false, false),

    GEOMETRY("GEOMETRY", false, false, true, false, false, false, true, false, false, false),

    VARBINARY("VARBINARY", true, false, true, false, false, false, true, true, false, false);

    private ColumnType columnType;

    public static TDengineColumnTypeEnum getByType(String dataType) {
        return COLUMN_TYPE_MAP.get(dataType.toUpperCase());
    }

    public ColumnType getColumnType() {
        return columnType;
    }


    TDengineColumnTypeEnum(String dataTypeName, boolean supportLength, boolean supportScale, boolean supportNullable, boolean supportAutoIncrement, boolean supportCharset, boolean supportCollation, boolean supportComments, boolean supportDefaultValue, boolean supportExtent, boolean supportValue) {
        this.columnType = new ColumnType(dataTypeName, supportLength, supportScale, supportNullable, supportAutoIncrement, supportCharset, supportCollation, supportComments, supportDefaultValue, supportExtent,supportValue,false);
    }

    private static Map<String, TDengineColumnTypeEnum> COLUMN_TYPE_MAP = Maps.newHashMap();

    static {
        for (TDengineColumnTypeEnum value : TDengineColumnTypeEnum.values()) {
            COLUMN_TYPE_MAP.put(value.getColumnType().getTypeName(), value);
        }
    }


    @Override
    public String buildCreateColumnSql(TableColumn column) {
        TDengineColumnTypeEnum type = COLUMN_TYPE_MAP.get(column.getColumnType().toUpperCase());
        if (type == null) {
            return "";
        }
        StringBuilder script = new StringBuilder();

        script.append("`").append(column.getName()).append("`").append(" ");

        script.append(buildDataType(column, type)).append(" ");

        script.append(buildCharset(column,type)).append(" ");

        script.append(buildCollation(column,type)).append(" ");

        script.append(buildNullable(column,type)).append(" ");

        script.append(buildDefaultValue(column,type)).append(" ");

        script.append(buildExt(column,type)).append(" ");

        script.append(buildAutoIncrement(column,type)).append(" ");

        script.append(buildComment(column,type)).append(" ");

        return script.toString();
    }

    private String buildCharset(TableColumn column, TDengineColumnTypeEnum type) {
        if(!type.getColumnType().isSupportCharset() || StringUtils.isEmpty(column.getCharSetName())){
            return "";
        }
        return StringUtils.join("CHARACTER SET ",column.getCharSetName());
    }

    private String buildCollation(TableColumn column, TDengineColumnTypeEnum type) {
        if(!type.getColumnType().isSupportCollation() || StringUtils.isEmpty(column.getCollationName())){
            return "";
        }
        return StringUtils.join("COLLATE ",column.getCollationName());
    }

    @Override
    public String buildModifyColumn(TableColumn tableColumn) {

        if (EditStatus.DELETE.name().equals(tableColumn.getEditStatus())) {
            return StringUtils.join("DROP COLUMN `", tableColumn.getName() + "`");
        }
        if (EditStatus.ADD.name().equals(tableColumn.getEditStatus())) {
            return StringUtils.join("ADD COLUMN ", buildCreateColumnSql(tableColumn));
        }
        if (EditStatus.MODIFY.name().equals(tableColumn.getEditStatus())) {
            if (!StringUtils.equalsIgnoreCase(tableColumn.getOldName(), tableColumn.getName())) {
                return StringUtils.join("CHANGE COLUMN `", tableColumn.getOldName(), "` ", buildCreateColumnSql(tableColumn));
            } else {
                return StringUtils.join("MODIFY COLUMN ", buildCreateColumnSql(tableColumn));
            }
        }
        return "";
    }

    private String buildAutoIncrement(TableColumn column, TDengineColumnTypeEnum type) {
        if(!type.getColumnType().isSupportAutoIncrement()){
            return "";
        }
        if (column.getAutoIncrement() != null && column.getAutoIncrement()) {
            return "AUTO_INCREMENT";
        }
        return "";
    }

    private String buildComment(TableColumn column, TDengineColumnTypeEnum type) {
        if(!type.columnType.isSupportComments() || StringUtils.isEmpty(column.getComment())){
            return "";
        }
        return StringUtils.join("COMMENT '",column.getComment(),"'");
    }

    private String buildExt(TableColumn column, TDengineColumnTypeEnum type) {
        if(!type.columnType.isSupportExtent() || StringUtils.isEmpty(column.getExtent())){
            return "";
        }
        return column.getComment();
    }

    private String buildDefaultValue(TableColumn column, TDengineColumnTypeEnum type) {
        if(!type.getColumnType().isSupportDefaultValue() || StringUtils.isEmpty(column.getDefaultValue())){
            return "";
        }

        if("EMPTY_STRING".equalsIgnoreCase(column.getDefaultValue().trim())){
            return StringUtils.join("DEFAULT ''");
        }

        if("NULL".equalsIgnoreCase(column.getDefaultValue().trim())){
            return StringUtils.join("DEFAULT NULL");
        }

        if(Arrays.asList(VARCHAR,BINARY,VARBINARY).contains(type)){
            return StringUtils.join("DEFAULT '",column.getDefaultValue(),"'");
        }

        if(Arrays.asList(TIMESTAMP).contains(type)){
            if("CURRENT_TIMESTAMP".equalsIgnoreCase(column.getDefaultValue().trim())){
                return StringUtils.join("DEFAULT ",column.getDefaultValue());
            }
            return StringUtils.join("DEFAULT '",column.getDefaultValue(),"'");
        }

        return StringUtils.join("DEFAULT ",column.getDefaultValue());
    }

    private String buildNullable(TableColumn column, TDengineColumnTypeEnum type) {
        if(!type.getColumnType().isSupportNullable()){
            return "";
        }
        if (column.getNullable()!=null && 1==column.getNullable()) {
            return "NULL";
        } else {
            return "NOT NULL";
        }
    }

    private String buildDataType(TableColumn column, TDengineColumnTypeEnum type) {
        String columnType = type.columnType.getTypeName();
        if (Arrays.asList(BINARY, VARBINARY, VARCHAR).contains(type)) {
            return StringUtils.join(columnType, "(", column.getColumnSize(), ")");
        }

        if (Arrays.asList(TIMESTAMP).contains(type)) {
            if (column.getColumnSize() == null || column.getColumnSize() == 0) {
                return columnType;
            } else {
                return StringUtils.join(columnType, "(", column.getColumnSize(), ")");
            }
        }


        if (Arrays.asList(FLOAT, DOUBLE,TINYINT).contains(type)) {
            if (column.getColumnSize() == null || column.getDecimalDigits() == null) {
                return columnType;
            }
            if (column.getColumnSize() != null && column.getDecimalDigits() == null) {
                return StringUtils.join(columnType, "(", column.getColumnSize() + ")");
            }
            if (column.getColumnSize() != null && column.getDecimalDigits() != null) {
                return StringUtils.join(columnType, "(", column.getColumnSize() + "," + column.getDecimalDigits() + ")");
            }
        }

        if (Arrays.asList(TINYINT_UNSIGNED).contains(type)) {
            if (column.getColumnSize() == null || column.getDecimalDigits() == null) {
                return columnType;
            }
            if (column.getColumnSize() != null && column.getDecimalDigits() == null) {
                return unsignedDataType(columnType, "(" + column.getColumnSize() + ")");
            }
            if (column.getColumnSize() != null && column.getDecimalDigits() != null) {
                return unsignedDataType(columnType, "(" + column.getColumnSize() + "," + column.getDecimalDigits() + ")");
            }
        }

        return columnType;
    }

    public String buildColumn(TableColumn column) {
        TDengineColumnTypeEnum type = COLUMN_TYPE_MAP.get(column.getColumnType().toUpperCase());
        if (type == null) {
            return "";
        }
        StringBuilder script = new StringBuilder();

        script.append("`").append(column.getName()).append("`").append(" ");
        script.append(buildDataType(column, type)).append(" ");
        return script.toString();
    }

    private String unsignedDataType(String dataTypeName, String middle) {
        String[] split = dataTypeName.split(" ");
        if (split.length == 2) {
            return StringUtils.join(split[0], middle, split[1]);
        }
        return StringUtils.join(dataTypeName, middle);
    }

    public static List<ColumnType> getTypes(){
       return Arrays.stream(TDengineColumnTypeEnum.values()).map(columnTypeEnum ->
                columnTypeEnum.getColumnType()
        ).toList();
    }


}
