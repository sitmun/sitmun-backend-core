package org.sitmun.domain;

import lombok.*;
import org.sitmun.config.PersistenceConstants;
import org.sitmun.constraints.CodeList;
import org.sitmun.constraints.CodeLists;
import org.sitmun.constraints.HttpURL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Download file task.
 */
@Entity
@Table(name = "STM_DOWNLOAD")
@Builder(builderMethodName = "downloadBuilder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DownloadTask {

  @Id
  @Column(name = "DOW_ID")
  private Integer id;

  /**
   * Download extension.
   */
  @Column(name = "DOW_EXT", length = PersistenceConstants.IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.DOWNLOAD_TASK_FORMAT)
  private String format;

  /**
   * Download scope.
   */
  @Column(name = "DOW_TYPE", length = PersistenceConstants.IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.DOWNLOAD_TASK_SCOPE)
  private String scope;

  /**
   * Location of the file to be downloaded.
   */
  @Column(name = "DOW_PATH", length = PersistenceConstants.URL)
  @NotNull
  @HttpURL
  private String path;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DownloadTask))
      return false;

    DownloadTask other = (DownloadTask) o;

    return getId() != null &&
      getId().equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
