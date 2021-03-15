package org.sitmun.plugin.core.domain;

import lombok.*;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.constraints.HttpURL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;
import static org.sitmun.plugin.core.domain.Constants.URL;

/**
 * Download file task.
 */
@Entity
@Table(name = "STM_DOWNLOAD")
@PrimaryKeyJoinColumn(name = "DOW_ID")
@Builder(builderMethodName = "downloadBuilder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DownloadTask extends Task {

  /**
   * Download extension.
   */
  @Column(name = "DOW_EXT", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.DOWNLOAD_TASK_FORMAT)
  private String format;

  /**
   * Download scope.
   */
  @Column(name = "DOW_TYPE", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.DOWNLOAD_TASK_SCOPE)
  private String scope;

  /**
   * Location of the file to be downloaded.
   */
  @Column(name = "DOW_PATH", length = URL)
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
