package ai.chat2db.plugin.tdengine;

import ai.chat2db.plugin.tdengine.type.TDengineColumnTypeEnum;
import ai.chat2db.plugin.tdengine.value.GeometryValueHandler;
import ai.chat2db.spi.ValueHandler;
import ai.chat2db.spi.jdbc.DefaultValueHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class TDengineValueHandler extends DefaultValueHandler {

    private static final Map<String, ValueHandler> VALUE_HANDLER_MAP = Map.of(
            TDengineColumnTypeEnum.GEOMETRY.name(), new GeometryValueHandler()
    );

    @Override
    public String getString(ResultSet rs, int index, boolean limitSize) throws SQLException {
        Object obj = rs.getObject(index);
        if (obj == null) {
            return null;
        }
        String columnTypeName = rs.getMetaData().getColumnTypeName(index);
        if (TDengineColumnTypeEnum.GEOMETRY.name().equalsIgnoreCase(columnTypeName)) {
            ValueHandler handler = VALUE_HANDLER_MAP.get(TDengineColumnTypeEnum.GEOMETRY.name());
            return handler.getString(rs, index, limitSize);
        } else {
            return super.getString(rs, index, limitSize);
        }
    }

}
