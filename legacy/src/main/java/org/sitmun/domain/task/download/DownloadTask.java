package org.sitmun.domain.task.download;

import lombok.*;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.infrastructure.persistence.type.basic.Http;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;

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
  @CodeList(CodeListsConstants.DOWNLOAD_TASK_FORMAT)
  private String format;

  /**
   * Download scope.
   */
  @Column(name = "DOW_TYPE", length = PersistenceConstants.IDENTIFIER)
  @NotNull
  @CodeList(CodeListsConstants.DOWNLOAD_TASK_SCOPE)
  private String scope;

  /**
   * Location of the file to be downloaded.
   */
  @Column(name = "DOW_PATH", length = PersistenceConstants.URL)
  @NotNull
  @Http
  private String path;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
        return true;
    }

    if (!(o instanceof DownloadTask)) {
        return false;
    }

    DownloadTask other = (DownloadTask) o;

    return getId() != null &&
      getId().equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
