package org.sitmun.decorators;

import java.util.Map;

import org.sitmun.authorization.dto.DatasourcePayloadDto;
import org.sitmun.authorization.dto.PayloadDto;
import org.springframework.stereotype.Component;

@Component
public class QueryFixedFiltersDecorator implements Decorator {

    @Override
    public boolean accept(Object target, PayloadDto payload) {
        return payload instanceof DatasourcePayloadDto;
    }

    @Override
    public void addBehavior(Object target, PayloadDto payload) {
        DatasourcePayloadDto datasourcePayloadDto = (DatasourcePayloadDto) payload;
        String sql = datasourcePayloadDto.getSql();
        if (sql != null && !sql.isEmpty()) {
            Map<String, String> fixedFilters = (Map<String, String>) target;
            for (String key : fixedFilters.keySet()) {
                sql = sql.replace("${" + key + "}", getFilterValue(fixedFilters.get(key)));
            }
            datasourcePayloadDto.setSql(sql);
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
