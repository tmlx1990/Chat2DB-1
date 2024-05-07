package ai.chat2db.plugin.sundb;

import ai.chat2db.plugin.sundb.builder.SUNDBSqlBuilder;
import ai.chat2db.plugin.sundb.type.SUNDBColumnTypeEnum;
import ai.chat2db.plugin.sundb.type.SUNDBDefaultValueEnum;
import ai.chat2db.plugin.sundb.type.SUNDBIndexTypeEnum;
import ai.chat2db.spi.MetaData;
import ai.chat2db.spi.SqlBuilder;
import ai.chat2db.spi.jdbc.DefaultMetaService;
import ai.chat2db.spi.model.*;
import ai.chat2db.spi.sql.SQLExecutor;
import ai.chat2db.spi.util.SortUtils;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotEmpty;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SUNDBMetaData extends DefaultMetaService implements MetaData {

    private List<String> systemSchemas = Arrays.asList("CTISYS", "SYS","SYSDBA","SYSSSO","SYSAUDITOR");

    @Override
    public List<Schema> schemas(Connection connection, String databaseName) {
        List<Schema> schemas = SQLExecutor.getInstance().schemas(connection, databaseName, null);
        return SortUtils.sortSchema(schemas, systemSchemas);
    }
      private String format(String tableName){
        return "\"" + tableName + "\"";
      }

    public String tableDDL(Connection connection, String databaseName, String schemaName, String tableName) {
        String sql = """
                     SELECT
                         (SELECT comments FROM user_tab_comments WHERE table_name = '%s') AS comments,
                         (SELECT dbms_metadata.get_ddl('TABLE', '%s', '%s') FROM dual) AS ddl
                     FROM dual;
                     """;
        StringBuilder ddlBuilder = new StringBuilder();
        String tableDDLSql = String.format(sql, tableName, tableName, schemaName);
        SQLExecutor.getInstance().execute(connection, tableDDLSql, resultSet -> {
            if (resultSet.next()) {
                String ddl = resultSet.getString("ddl");
                String comment = resultSet.getString("comments");
                if (StringUtils.isNotBlank(comment)) {
                    ddlBuilder.append(ddl).append("\n").append("COMMENT ON TABLE ").append(format(schemaName))
                            .append(".").append(format(tableName)).append(" IS ").append("'").append(comment).append("';");
                }
            }
        });
        String columnCommentsSql =String.format("select COLNAME,COMMENT$ from SYS.SYSCOLUMNCOMMENTS\n" +
                                          "where SCHNAME = '%s' and TVNAME = '%s'and TABLE_TYPE = 'TABLE';", schemaName,tableName);
           SQLExecutor.getInstance().execute(connection, columnCommentsSql, resultSet->{
               while (resultSet.next()) {
                   String columnName = resultSet.getString("COLNAME");
                   String comment = resultSet.getString("COMMENT$");
                   ddlBuilder.append("COMMENT ON COLUMN ").append(format(schemaName)).append(".").append(format(tableName))
                           .append(".").append(format(columnName)).append(" IS ").append("'").append(comment).append("';").append("\n");
               }
           });
        return ddlBuilder.toString();
    }

    private static String ROUTINES_SQL
        = "SELECT OWNER, NAME, TEXT FROM ALL_SOURCE WHERE TYPE = '%s' AND OWNER = '%s' AND NAME = '%s' ORDER BY LINE";

    @Override
    public Function function(Connection connection, @NotEmpty String databaseName, String schemaName,
        String functionName) {

        String sql = String.format(ROUTINES_SQL, "PROC",schemaName, functionName);
        return SQLExecutor.getInstance().execute(connection, sql, resultSet -> {
            StringBuilder sb = new StringBuilder();
            while (resultSet.next()) {
                sb.append(resultSet.getString("TEXT") + "\n");
            }
            Function function = new Function();
            function.setDatabaseName(databaseName);
            function.setSchemaName(schemaName);
            function.setFunctionName(functionName);
            function.setFunctionBody(sb.toString());
            return function;

        });

    }

    @Override
    public Procedure procedure(Connection connection, @NotEmpty String databaseName, String schemaName,
        String procedureName) {
        String sql = String.format(ROUTINES_SQL, "PROC", schemaName,procedureName);
        return SQLExecutor.getInstance().execute(connection, sql, resultSet -> {
            StringBuilder sb = new StringBuilder();
            while (resultSet.next()) {
                sb.append(resultSet.getString("TEXT") + "\n");
            }
            Procedure procedure = new Procedure();
            procedure.setDatabaseName(databaseName);
            procedure.setSchemaName(schemaName);
            procedure.setProcedureName(procedureName);
            procedure.setProcedureBody(sb.toString());
            return procedure;
        });
    }

    private static String TRIGGER_SQL
        = "SELECT OWNER, TRIGGER_NAME, TABLE_OWNER, TABLE_NAME, TRIGGERING_TYPE, TRIGGERING_EVENT, STATUS, TRIGGER_BODY "
        + "FROM ALL_TRIGGERS WHERE OWNER = '%s' AND TRIGGER_NAME = '%s'";

    private static String TRIGGER_SQL_LIST = "SELECT OWNER, TRIGGER_NAME FROM ALL_TRIGGERS WHERE OWNER = '%s'";

    @Override
    public List<Trigger> triggers(Connection connection, String databaseName, String schemaName) {
        List<Trigger> triggers = new ArrayList<>();
        String sql = String.format(TRIGGER_SQL_LIST, schemaName);
        return SQLExecutor.getInstance().execute(connection, sql, resultSet -> {
            while (resultSet.next()) {
                Trigger trigger = new Trigger();
                trigger.setTriggerName(resultSet.getString("TRIGGER_NAME"));
                trigger.setSchemaName(schemaName);
                trigger.setDatabaseName(databaseName);
                triggers.add(trigger);
            }
            return triggers;
        });
    }

    @Override
    public Trigger trigger(Connection connection, @NotEmpty String databaseName, String schemaName,
        String triggerName) {

        String sql = String.format(TRIGGER_SQL, schemaName, triggerName);
        return SQLExecutor.getInstance().execute(connection, sql, resultSet -> {
            Trigger trigger = new Trigger();
            trigger.setDatabaseName(databaseName);
            trigger.setSchemaName(schemaName);
            trigger.setTriggerName(triggerName);
            if (resultSet.next()) {
                trigger.setTriggerBody(resultSet.getString("TRIGGER_BODY"));
            }
            return trigger;
        });
    }

    private static String VIEW_SQL
        = "SELECT OWNER, VIEW_NAME, TEXT FROM ALL_VIEWS WHERE OWNER = '%s' AND VIEW_NAME = '%s'";

    @Override
    public Table view(Connection connection, String databaseName, String schemaName, String viewName) {
        String sql = String.format(VIEW_SQL, schemaName, viewName);
        return SQLExecutor.getInstance().execute(connection, sql, resultSet -> {
            Table table = new Table();
            table.setDatabaseName(databaseName);
            table.setSchemaName(schemaName);
            table.setName(viewName);
            if (resultSet.next()) {
                table.setDdl(resultSet.getString("TEXT"));
            }
            return table;
        });
    }

    private static String INDEX_SQL = "SELECT i.TABLE_NAME, i.INDEX_TYPE, i.INDEX_NAME, i.UNIQUENESS ,c.COLUMN_NAME, c.COLUMN_POSITION, c.DESCEND, cons.CONSTRAINT_TYPE FROM ALL_INDEXES i JOIN ALL_IND_COLUMNS c ON i.INDEX_NAME = c.INDEX_NAME AND i.TABLE_NAME = c.TABLE_NAME AND i.TABLE_OWNER = c.TABLE_OWNER LEFT JOIN ALL_CONSTRAINTS cons ON i.INDEX_NAME = cons.INDEX_NAME AND i.TABLE_NAME = cons.TABLE_NAME AND i.TABLE_OWNER = cons.OWNER WHERE i.TABLE_OWNER = '%s' AND i.TABLE_NAME = '%s' ORDER BY i.INDEX_NAME, c.COLUMN_POSITION;";

    @Override
    public List<TableIndex> indexes(Connection connection, String databaseName, String schemaName, String tableName) {
        String sql = String.format(INDEX_SQL, schemaName, tableName);
        return SQLExecutor.getInstance().execute(connection, sql, resultSet -> {
            LinkedHashMap<String, TableIndex> map = new LinkedHashMap();
            while (resultSet.next()) {
                String keyName = resultSet.getString("INDEX_NAME");
                TableIndex tableIndex = map.get(keyName);
                if (tableIndex != null) {
                    List<TableIndexColumn> columnList = tableIndex.getColumnList();
                    columnList.add(getTableIndexColumn(resultSet));
                    columnList = columnList.stream().sorted(Comparator.comparing(TableIndexColumn::getOrdinalPosition))
                            .collect(Collectors.toList());
                    tableIndex.setColumnList(columnList);
                } else {
                    TableIndex index = new TableIndex();
                    index.setDatabaseName(databaseName);
                    index.setSchemaName(schemaName);
                    index.setTableName(tableName);
                    index.setName(keyName);
                    index.setUnique("UNIQUE".equalsIgnoreCase(resultSet.getString("UNIQUENESS")));
//                    index.setType(resultSet.getString("Index_type"));
//                    index.setComment(resultSet.getString("Index_comment"));
                    List<TableIndexColumn> tableIndexColumns = new ArrayList<>();
                    tableIndexColumns.add(getTableIndexColumn(resultSet));
                    index.setColumnList(tableIndexColumns);
                    if ("P".equalsIgnoreCase(resultSet.getString("CONSTRAINT_TYPE"))) {
                        index.setType(SUNDBIndexTypeEnum.PRIMARY_KEY.getName());
                    } else if (index.getUnique()) {
                        index.setType(SUNDBIndexTypeEnum.UNIQUE.getName());
                    } else if ("BITMAP".equalsIgnoreCase(resultSet.getString("INDEX_TYPE"))) {
                        index.setType(SUNDBIndexTypeEnum.BITMAP.getName());
                    } else {
                        index.setType(SUNDBIndexTypeEnum.NORMAL.getName());
                    }
                    map.put(keyName, index);
                }
            }
            return map.values().stream().collect(Collectors.toList());
        });

    }

    private TableIndexColumn getTableIndexColumn(ResultSet resultSet) throws SQLException {
        TableIndexColumn tableIndexColumn = new TableIndexColumn();
        tableIndexColumn.setColumnName(resultSet.getString("COLUMN_NAME"));
        tableIndexColumn.setOrdinalPosition(resultSet.getShort("COLUMN_POSITION"));
//        tableIndexColumn.setCollation(resultSet.getString("Collation"));
//        tableIndexColumn.setCardinality(resultSet.getLong("Cardinality"));
//        tableIndexColumn.setSubPart(resultSet.getLong("Sub_part"));
        String collation = resultSet.getString("DESCEND");
        if ("ASC".equalsIgnoreCase(collation)) {
            tableIndexColumn.setAscOrDesc("ASC");
        } else if ("DESC".equalsIgnoreCase(collation)) {
            tableIndexColumn.setAscOrDesc("DESC");
        }
        return tableIndexColumn;
    }

    @Override
    public SqlBuilder getSqlBuilder() {
        return new SUNDBSqlBuilder();
    }

    @Override
    public TableMeta getTableMeta(String databaseName, String schemaName, String tableName) {
        return TableMeta.builder()
                .columnTypes(SUNDBColumnTypeEnum.getTypes())
                .charsets(Lists.newArrayList())
                .collations(Lists.newArrayList())
                .indexTypes(SUNDBIndexTypeEnum.getIndexTypes())
                .defaultValues(SUNDBDefaultValueEnum.getDefaultValues())
                .build();
    }

    @Override
    public String getMetaDataName(String... names) {
        return Arrays.stream(names).filter(name -> StringUtils.isNotBlank(name)).map(name -> "\"" + name + "\"").collect(Collectors.joining("."));
    }


    @Override
    public List<String> getSystemSchemas() {
        return systemSchemas;
    }
}
