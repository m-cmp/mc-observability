package mcmp.mc.observability.agent.trigger.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StringListTypeHandler extends BaseTypeHandler<List<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            String joinedString = String.join(",", parameter);
            ps.setString(i, joinedString);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String columnValue = rs.getString(columnName);
        if (columnValue == null) {
            return Collections.emptyList();  // 또는 null 반환
        }
        return Arrays.asList(columnValue.split(","));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnValue = rs.getString(columnIndex);
        if (columnValue == null) {
            return Collections.emptyList();  // 또는 null 반환
        }
        return Arrays.asList(columnValue.split(","));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String columnValue = cs.getString(columnIndex);
        if (columnValue == null) {
            return Collections.emptyList();  // 또는 null 반환
        }
        return Arrays.asList(columnValue.split(","));
    }
}
