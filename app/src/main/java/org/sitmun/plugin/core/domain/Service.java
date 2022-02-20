package org.sitmun.plugin.core.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.constraints.HttpURL;
import org.sitmun.plugin.core.constraints.SpatialReferenceSystem;
import org.sitmun.plugin.core.converters.StringListAttributeConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.*;

/**
 * Service.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_SERVICE")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Service {

  /**
   * Unique identifier.
   */
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
  @JsonView(WorkspaceApplication.View.class)
  private Integer id;

  /**
   * Service name.
   */
  @Column(name = "SER_NAME", length = 60)
  @NotBlank
  @JsonView(WorkspaceApplication.View.class)
  private String name;

  /**
   * Service description.
   */
  @Column(name = "SER_ABSTRACT", length = SHORT_DESCRIPTION)
  private String description;

  /**
   * Service endpoint.
   */
  @Column(name = "SER_URL", length = URL)
  @NotNull
  @HttpURL
  @JsonView(WorkspaceApplication.View.class)
  private String serviceURL;

  /**
   * List of SRS supported by this service.
   */
  @Column(name = "SER_PROJECTS", length = 1000)
  @Convert(converter = StringListAttributeConverter.class)
  @SpatialReferenceSystem
  @JsonView(WorkspaceApplication.View.class)
  private List<String> supportedSRS;

  /**
   * Legend endpoint.
   */
  @Column(name = "SER_LEGEND", length = URL)
  @HttpURL
  @JsonView(WorkspaceApplication.View.class)
  private String legendURL;

  /**
   * Get information endpoint.
   */
  @Column(name = "SER_INFOURL", length = URL)
  @HttpURL
  @JsonView(WorkspaceApplication.View.class)
  private String getInformationURL;

  /**
   * Created date.
   */
  @Column(name = "SER_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * Service type.
   * The protocol that can be used to request the service.
   */
  @Column(name = "SER_PROTOCOL", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.SERVICE_TYPE)
  @JsonView(WorkspaceApplication.View.class)
  private String type;

  /**
   * Native protocol.
   * Required when SITMUN acts as reverse proxy and the origin protocol has a protocol
   * different from the protocol declared in {@link #type}.
   */
  @Column(name = "SER_NAT_PROT", length = IDENTIFIER)
  @CodeList(CodeLists.SERVICE_NATIVE_PROTOCOL)
  private String nativeProtocol;

  /**
   * <code>true</code> if the service is blocked and cannot be used.
   */
  @NotNull
  @Column(name = "SER_BLOCKED")
  private Boolean blocked;

  /**
   * Layers provided by this service.
   */
  @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<Cartography> layers = new HashSet<>();

  /**
   * Service parameters.
   */
  @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @JsonView(WorkspaceApplication.View.class)
  private Set<ServiceParameter> parameters = new HashSet<>();

  /**
   * Service authentication mode.
   */
  @Column(name = "SER_AUTH_MOD", length = IDENTIFIER)
  @CodeList(CodeLists.SERVICE_AUTHENTICATION_MODE)
  private String authenticationMode;

  /**
   * User.
   */
  @Column(name = "SER_USER", length = IDENTIFIER)
  private String user;

  /**
   * Password.
   */
  @Column(name = "SER_PWD", length = 50)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  @JsonIgnore
  @Transient
  private String storedPassword;

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
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Service))
      return false;

    Service other = (Service) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
