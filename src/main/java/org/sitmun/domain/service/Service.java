package org.sitmun.domain.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import lombok.*;
import org.sitmun.authorization.client.dto.ClientConfigurationViews;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.infrastructure.persistence.type.basic.Http;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;
import org.sitmun.infrastructure.persistence.type.i18n.I18n;
import org.sitmun.infrastructure.persistence.type.list.StringListAttributeConverter;
import org.sitmun.infrastructure.persistence.type.srs.Srs;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Service. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_SERVICE")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Service {

  /** Unique identifier. */
  @TableGenerator(
      name = "STM_SERVICE_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "SER_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_SERVICE_GEN")
  @Column(name = "SER_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /** Service name. */
  @Column(name = "SER_NAME", length = 60)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /** Service description. */
  @Column(name = "SER_ABSTRACT", length = PersistenceConstants.LONG_DESCRIPTION)
  @I18n
  private String description;

  /** Service endpoint. */
  @Column(name = "SER_URL", length = PersistenceConstants.URL)
  @NotNull
  @Http
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String serviceURL;

  /** List of SRS supported by this service. */
  @Column(name = "SER_PROJECTS", length = 1000)
  @Convert(converter = StringListAttributeConverter.class)
  @Srs
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private List<String> supportedSRS;

  /** Legend endpoint. */
  @Column(name = "SER_LEGEND", length = PersistenceConstants.URL)
  @Http
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String legendURL;

  /** Get information endpoint. */
  @Column(name = "SER_INFOURL", length = PersistenceConstants.URL)
  @Http
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String getInformationURL;

  /** Created date. */
  @Column(name = "SER_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /** Service type. The protocol that can be used to request the service. */
  @Column(name = "SER_PROTOCOL", length = PersistenceConstants.IDENTIFIER)
  @NotNull
  @CodeList(CodeListsConstants.SERVICE_TYPE)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String type;

  /**
   * Native protocol. Required when SITMUN acts as reverse proxy and the origin protocol has a
   * protocol different from the protocol declared in {@link #type}.
   */
  @Column(name = "SER_NAT_PROT", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.SERVICE_NATIVE_PROTOCOL)
  private String nativeProtocol;

  /** <code>true</code> if the service is blocked and cannot be used. */
  @NotNull
  @Column(name = "SER_BLOCKED")
  private Boolean blocked;

  /** <code>true</code> if the service is blocked and cannot be used. */
  @NotNull
  @Column(name = "SER_PROXIED", nullable = false)
  @Builder.Default
  private Boolean isProxied = false;

  /** Layers provided by this service. */
  @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<Cartography> layers = new HashSet<>();

  /** Service parameters. */
  @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Set<ServiceParameter> parameters = new HashSet<>();

  /** Service authentication mode. */
  @Column(name = "SER_AUTH_MOD", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.SERVICE_AUTHENTICATION_MODE)
  private String authenticationMode;

  /** User. */
  @Column(name = "SER_USER", length = PersistenceConstants.IDENTIFIER)
  private String user;

  /** Password. */
  @Column(name = "SER_PWD", length = 50)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  @JsonIgnore @Transient private String storedPassword;

  /**
   * True if the password is set.
   *
   * @return true if password is not empty
   */
  public Boolean getPasswordSet() {
    return password != null && !password.isEmpty();
  }

  @PostLoad
  public void postLoad() {
    storedPassword = password;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Service other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
