package org.sitmun.authorization.dto;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.AuthorizationConstants;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps cartography query tasks to DTOs for authorization purposes.
 * Handles the transformation of cartography query tasks into a standardized format
 * that includes service parameters, layer information, and proxy URLs.
 */
@Slf4j
@Component
public class TaskQueryCartographyService implements TaskMapper {

    @Value("${sitmun.proxy-middleware.url:}")
    private String proxyUrl;

    /**
     * Determines if this mapper can handle the given task.
     * @param task The task to check
     * @return true if the task is a cartography query task
     */
    public boolean accept(Task task) {
        return DomainConstants.Tasks.isCartographyQueryTask(task);
    }

    /**
     * Maps a cartography query task to a TaskDto.
     * Constructs the proxy URL and includes service parameters and layer information.
     * @param task The task to map
     * @param application The associated application
     * @param territory The associated territory
     * @return A TaskDto containing the mapped task information
     */
    public TaskDto map(Task task, Application application, Territory territory) {
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> properties = task.getProperties();
        if (properties != null) {
            parameters = convertToJsonObject(properties);
        }

        Cartography cartography = task.getCartography();
        Service service = cartography.getService();

        String url = proxyUrl + "/proxy/" + application.getId() + "/" + territory.getId() + "/" + service.getType() + "/" + service.getId();
        parameters.put(AuthorizationConstants.TaskDto.PARAMETER_SERVICE, getParametersObject(AuthorizationConstants.TaskDto.QUERY, true, service.getType()));
        String layers = cartography.getLayers().stream().reduce((a, b) -> a + "," + b).orElse("");
        if (DomainConstants.Services.isWfsService(service)) {
            parameters.put(AuthorizationConstants.TaskDto.PARAMETER_WFS_TYPENAME, getParametersObject(AuthorizationConstants.TaskDto.QUERY, true, layers));
        } else {
            parameters.put(AuthorizationConstants.TaskDto.PARAMETER_LAYERS, getParametersObject(AuthorizationConstants.TaskDto.QUERY, true, layers));
        }
        return TaskDto.builder()
                .id("task/" + task.getId())
                .type(AuthorizationConstants.TaskDto.SIMPLE)
                .parameters(parameters)
                .url(url)
                .build();
    }

    /**
     * Converts task properties to a JSON-compatible parameter map.
     * @param properties The task properties to convert
     * @return A map of parameter names to their configuration
     */
    public Map<String, Object> convertToJsonObject(Map<String, Object> properties) {
        Map<String, Object> parameters = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listOfParameters = (List<Map<String, Object>>) properties.getOrDefault(DomainConstants.Tasks.PROPERTY_PARAMETERS, Collections.emptyList());

        for (Map<String, Object> param : listOfParameters) {
            if (param.containsKey(DomainConstants.Tasks.PARAMETERS_NAME) &&
                    param.containsKey(DomainConstants.Tasks.PARAMETERS_TYPE) &&
                    param.containsKey(DomainConstants.Tasks.PARAMETERS_REQUIRED)) {
                String name = String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_NAME));
                String type = String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_TYPE));
                Boolean required = Boolean.valueOf(String.valueOf(param.get(DomainConstants.Tasks.PARAMETERS_REQUIRED)));

                Map<String, Object> values = getParametersObject(type, required, null);
                parameters.put(name, values);
            }
        }

        return parameters;
    }

    /**
     * Creates a parameter configuration object with type, required flag, and optional value.
     * @param type The parameter type
     * @param required Whether the parameter is required
     * @param value The parameter value (can be null)
     * @return A map containing the parameter configuration
     */
    private Map<String, Object> getParametersObject(String type, Boolean required, String value) {
        Map<String, Object> values = new HashMap<>();
        values.put(AuthorizationConstants.TaskDto.PARAMETER_TYPE, type);
        values.put(AuthorizationConstants.TaskDto.PARAMETER_REQUIRED, required);
        if (value != null) {
            values.put(AuthorizationConstants.TaskDto.PARAMETER_VALUE, value);
        }
        return values;
    }
}
