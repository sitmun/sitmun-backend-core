package org.sitmun.decorators;

import java.util.Map;

import org.sitmun.authorization.dto.DatasourcePayloadDto;
import org.sitmun.authorization.dto.PayloadDto;
import org.springframework.stereotype.Component;

@Component
public class QueryPaginationDecorator implements Decorator {

    @Override
    public boolean accept(Object target, PayloadDto payload) {
        return payload instanceof DatasourcePayloadDto;
    }

    @Override
    public void addBehavior(Object target, PayloadDto payload) {
        DatasourcePayloadDto datasourcePayloadDto = (DatasourcePayloadDto) payload;
        String sql = datasourcePayloadDto.getSql();
        if (sql != null && !sql.isEmpty()) {
            Map<String, String> pagination = (Map<String, String>) target;
            if (pagination.containsKey("LIMIT")) {
                sql = sql.concat(" LIMIT " + pagination.get("LIMIT"));
            }

            if (pagination.containsKey("OFFSET")) {
                sql = sql.concat(" OFFSET " + pagination.get("OFFSET"));
            }

            datasourcePayloadDto.setSql(sql);

        }
    }
}
