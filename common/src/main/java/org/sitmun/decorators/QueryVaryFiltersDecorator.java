package org.sitmun.decorators;

import java.util.Map;

import org.sitmun.authorization.dto.DatasourcePayloadDto;
import org.sitmun.authorization.dto.PayloadDto;
import org.springframework.stereotype.Component;

@Component
public class QueryVaryFiltersDecorator implements Decorator {

    @Override
    public boolean accept(Object target, PayloadDto payload) {
        return payload instanceof DatasourcePayloadDto;
    }

    @Override
    public void addBehavior(Object target, PayloadDto payload) {
        DatasourcePayloadDto datasourcePayloadDto = (DatasourcePayloadDto) payload;
        String sql = datasourcePayloadDto.getSql();
        if (sql != null && !sql.isEmpty()) {
            Map<String, String> varyFilters = (Map<String, String>) target;
            if (varyFilters != null && !varyFilters.isEmpty()) {
                if (!sql.contains("WHERE")) {
                    sql = sql.concat(" WHERE ");
                } else {
                    sql = sql.concat(" AND ");
                }
                for (String key : varyFilters.keySet()) {
                    sql = sql.concat(key + "=" + getFilterValue(varyFilters.get(key)) + " AND ");
                }
                datasourcePayloadDto.setSql(sql.substring(0, sql.length() - 5));
            }
        }
    }

    private String getFilterValue(String value) {
        if (!value.matches("-?\\d+(\\.\\d+)?")) {
            return "'" + value + "'";
        } else {
            return value;
        }
    }
}
