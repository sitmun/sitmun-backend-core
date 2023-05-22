package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonTypeName("DatasourcePayload")
public class DatasourcePayloadDto extends PayloadDto {

  private String uri;
  private String user;
  private String password;
  private String driver;
  private String sql;

  @Builder
  public DatasourcePayloadDto(List<String> vary, String uri, String user, String password, String driver, String sql) {
    super(vary);
    this.uri = uri;
    this.user = user;
    this.password = password;
    this.driver = driver;
    this.sql = sql;
  }
}
