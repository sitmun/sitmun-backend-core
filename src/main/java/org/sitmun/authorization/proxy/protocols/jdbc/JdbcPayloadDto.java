package org.sitmun.authorization.proxy.protocols.jdbc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.authorization.proxy.dto.PayloadDto;

@Getter
@Setter
@JsonTypeName("DatasourcePayload")
public class JdbcPayloadDto extends PayloadDto {

  private String uri;
  private String user;
  private String password;
  private String driver;
  private String sql;

  @Builder
  public JdbcPayloadDto(
      List<String> vary, String uri, String user, String password, String driver, String sql) {
    super(vary);
    this.uri = uri;
    this.user = user;
    this.password = password;
    this.driver = driver;
    this.sql = sql;
  }
}
