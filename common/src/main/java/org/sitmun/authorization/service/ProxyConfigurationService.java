package org.sitmun.authorization.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sitmun.authorization.dto.ConfigProxyDto;
import org.sitmun.authorization.dto.ConfigProxyRequest;
import org.sitmun.authorization.dto.DatasourcePayloadDto;
import org.sitmun.authorization.dto.HttpSecurityDto;
import org.sitmun.authorization.dto.OgcWmsPayloadDto;
import org.sitmun.authorization.dto.PayloadDto;
import org.sitmun.authorization.exception.BadRequestException;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

@org.springframework.stereotype.Service
public class ProxyConfigurationService {
	
	private final String SQL_COMMAND_KEY = "command";
	
	private final String CONNECTION_TYPE_KEY = "SQL";
	
	private final String VARY_KEY = "VARY";

	private final CartographyRepository cartographyRepository;
	
	private final TaskRepository taskRepository;
	
	@Value("${sitmun.proxy.config-response-validity-in-seconds}")
	private int responseValidityTime;
	
	public ProxyConfigurationService (CartographyRepository cartographyRepository,
			TaskRepository taskRepository) {
		this.cartographyRepository = cartographyRepository;
		this.taskRepository = taskRepository;
	}
	
	public boolean validateUserAccess(ConfigProxyRequest configProxyRequest, String userName) {
		boolean result = true;
		
		return true;
	}
	
	public ConfigProxyDto getConfiguration(ConfigProxyRequest configProxyRequest, long expirationTimeToken) {
		ConfigProxyDto result = null;
		PayloadDto payload = null;
		String configType = "";
		List<String> varyParameters = new ArrayList<String>();
		if(CONNECTION_TYPE_KEY.equalsIgnoreCase(configProxyRequest.getType())) {
			Optional<Task> task = taskRepository.findById(configProxyRequest.getTypeId());
			if (task.isPresent()) {
				Task effectiveTask = task.get();
				DatabaseConnection databaseConnection = effectiveTask.getConnection();
				payload = getDatasourceConfiguration(databaseConnection, getSqlByTask(effectiveTask));
				configType = CONNECTION_TYPE_KEY;
			}
		}else {
			Optional<Cartography> cartography = cartographyRepository.findById(configProxyRequest.getTypeId());
			if(cartography.isPresent()) {
				Cartography effectiveCartography = cartography.get();
				Service service = effectiveCartography.getService();
				Map<String, String> parameters = configProxyRequest.getParameters();
				addParameters(parameters, service.getParameters(), varyParameters);
				payload = getOgcWmsConfiguration(service, parameters, configProxyRequest.getMethod());
				configType = service.getType();
			}
		}
		if(payload != null) {
			long expirationTime = expirationTimeToken > 0 ? expirationTimeToken / 1000 : (new Date().getTime() / 1000) + responseValidityTime; 
			result = ConfigProxyDto.builder()
			  .type(configType)
			  .exp(expirationTime)
			  .vary(varyParameters)
			  .payload(payload)
			  .build();
		} else {
			throw new BadRequestException("Bad request");
		}
		return result;
	}
	
	private OgcWmsPayloadDto getOgcWmsConfiguration(Service service, Map<String, String> parameters, String method) {
		
		if (service == null) {
			return null;
		}
		
		HttpSecurityDto security = null;
		if (service.getPasswordSet()) {
			security = HttpSecurityDto.builder()
			  .type("http")
			  .scheme("basic")
			  .username(service.getUser())
			  .password(service.getPassword())
			  .build();
		}
		
		return OgcWmsPayloadDto.builder()
		    .uri(service.getServiceURL())
			.method(method)
			.parameters(parameters)
			.security(security)
			.build();
	}
	
	private DatasourcePayloadDto getDatasourceConfiguration(DatabaseConnection databaseConnection, String sql) {
		return databaseConnection != null ? DatasourcePayloadDto.builder()
		    .uri(databaseConnection.getUrl())
		    .user(databaseConnection.getUser())
		    .password(databaseConnection.getPassword())
		    .driver(databaseConnection.getDriver())
		    .sql(sql)
		    .build() : null;
	}
	
	private void addParameters(Map<String, String> requestParameters, Set<ServiceParameter> serviceParameters, List<String> varyParameters) {
		serviceParameters.forEach(p -> {
			if(VARY_KEY.equalsIgnoreCase(p.getType())) {
				varyParameters.add(p.getName());
			} else if (!requestParameters.containsKey(p.getName())){
				requestParameters.put(p.getName(), p.getValue());
			}
		});
	}
	
	private String getSqlByTask(Task task) {
		String sql = "";
		Map<String, Object> taskParams = task.getProperties();
		if(taskParams != null && taskParams.containsKey(SQL_COMMAND_KEY)) {
			sql = (String)taskParams.get(SQL_COMMAND_KEY);
		}
		
		if (!StringUtils.hasText(sql)) {
			throw new BadRequestException("Bad request");
		}
		return sql;
	}
}
