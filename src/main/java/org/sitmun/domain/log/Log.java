package org.sitmun.domain.log;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.persistence.type.list.StringListAttributeConverter;
import org.sitmun.infrastructure.persistence.type.srs.Srs;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Log.
 */
@Entity
@Table(name = "STM_LOG")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Log {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_LOG_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "LOG_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_LOG_GEN")
  @Column(name = "LOG_ID")
  private Integer id;

  /**
   * Action date.
   */
  @Column(name = "LOG_DATE")
  @Temporal(TemporalType.TIMESTAMP)
  private Date date;

  /**
   * Action type.
   */
  @Column(name = "LOG_TYPE", length = 50)
  private String type;

  /**
   * Originated by this user.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_USERID")
  private User user;

  /**
   * Originated by this application.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_APPID")
  private Application application;

  /**
   * Originated in this application.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_TERID")
  private Territory territory;

  /**
   * Originated by this task.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_TASKID")
  private Task task;

  /**
   * Originated by this cartography.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_GIID")
  private Cartography cartography;

  /**
   * Counter (to add up).
   */
  @Column(name = "LOG_COUNT")
  private Integer counter;

  /**
   * Territory code.
   */
  @Column(name = "LOG_TER", length = PersistenceConstants.IDENTIFIER)
  private String territoryCode;

  /**
   * If the log entry involves more than one territory, this field must contain a list of all the
   * territories involved.
   */
  @Column(name = "LOG_TEREXT", length = 250)
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> territories;

  /**
   * Data (or process) requested.
   */
  @Column(name = "LOG_DATA", length = 250)
  private String data;

  /**
   * SRS requested.
   */
  @Column(name = "LOG_SRS", length = PersistenceConstants.IDENTIFIER)
  @Srs
  private String srs;

  /**
   * Format requested.
   */
  @Column(name = "LOG_FORMAT", length = PersistenceConstants.IDENTIFIER)
  private String format;

  /**
   * True if the user used the add buffer option.
   */
  @Column(name = "LOG_BUFFER")
  private Boolean buffer;

  /**
   * Email where the results have been sent.
   */
  @Column(name = "LOG_EMAIL", length = 250)
  @Email
  private String email;

  /**
   * Other information.
   */
  @Column(name = "LOG_OTHER", length = 4000)
  private String other;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Log otherLog)) {
      return false;
    }

    return Objects.equals(id, otherLog.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
