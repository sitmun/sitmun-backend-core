package org.sitmun.plugin.core.domain;


import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;


import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

/**
 * User position in a territory.
 */
@Entity
@Table(name = "STM_POST", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"POS_USERID", "POS_TERID"})})
public class UserPosition {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_CARGO_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "POS_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CARGO_GEN")
  @Column(name = "POS_ID")
  private Integer id;

  /**
   * Position description.
   */
  @Column(name = "POS_POST", length = 250)
  private String name;

  /**
   * Organization.
   */
  @Column(name = "POS_ORG", length = 250)
  private String organization;

  /**
   * Email.
   */
  @Column(name = "POS_EMAIL", length = 250)
  @Email
  private String email;

  /**
   * Creation date.
   */
  @Column(name = "POS_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Expiration date.
   */
  @Column(name = "POS_EXPIRATION")
  @Temporal(TemporalType.TIMESTAMP)
  private Date expirationDate;

  /**
   * Type of user (only used in some cases).
   */
  @Column(name = "POS_TYPE", length = IDENTIFIER)
  @CodeList(CodeLists.USER_POSITION_TYPE)
  private String type;

  /**
   * User.
   */
  @ManyToOne
  @JoinColumn(name = "POS_USERID", foreignKey = @ForeignKey(name = "STM_CGO_FK_USU"))
  @NotNull
  private User user;

  /**
   * Territory.
   */
  @ManyToOne
  @JoinColumn(name = "POS_TERID", foreignKey = @ForeignKey(name = "STM_CGO_FK_TER"))
  @NotNull
  private Territory territory;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(Date datedDate) {
    this.expirationDate = datedDate;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Territory getTerritory() {
    return territory;
  }

  public void setTerritory(Territory territory) {
    this.territory = territory;
  }

  public UserPosition() {
  }

  private UserPosition(Integer id, String name, String organization,
                       @Email String email, Date createdDate, Date expirationDate,
                       String type,
                       @NotNull User user,
                       @NotNull Territory territory) {
    this.id = id;
    this.name = name;
    this.organization = organization;
    this.email = email;
    this.createdDate = createdDate;
    this.expirationDate = expirationDate;
    this.type = type;
    this.user = user;
    this.territory = territory;
  }

  public static class UserPositionBuilder {
    private Integer id;
    private String name;
    private String organization;
    private @Email String email;
    private Date createdDate;
    private Date expirationDate;
    private String type;
    private @NotNull User user;
    private @NotNull Territory territory;

    public UserPositionBuilder setId(Integer id) {
      this.id = id;
      return this;
    }

    public UserPositionBuilder setName(String name) {
      this.name = name;
      return this;
    }

    public UserPositionBuilder setOrganization(String organization) {
      this.organization = organization;
      return this;
    }

    public UserPositionBuilder setEmail(@Email String email) {
      this.email = email;
      return this;
    }

    public UserPositionBuilder setCreatedDate(Date createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public UserPositionBuilder setExpirationDate(Date expirationDate) {
      this.expirationDate = expirationDate;
      return this;
    }

    public UserPositionBuilder setType(String type) {
      this.type = type;
      return this;
    }

    public UserPositionBuilder setUser(@NotNull User user) {
      this.user = user;
      return this;
    }

    public UserPositionBuilder setTerritory(@NotNull Territory territory) {
      this.territory = territory;
      return this;
    }

    public UserPosition build() {
      return new UserPosition(id, name, organization, email, createdDate, expirationDate, type,
          user,
          territory);
    }
  }
}
