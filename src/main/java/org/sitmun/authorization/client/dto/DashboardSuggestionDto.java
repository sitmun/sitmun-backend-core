package org.sitmun.authorization.client.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardSuggestionDto {
  private List<ApplicationSuggestion> applications;
  private List<TerritorySuggestion> territories;

  @Getter
  @Setter
  public static class ApplicationSuggestion {
    private int id;
    private String name;
    private String title;
    private String logo;
    private Boolean appPrivate;
  }

  @Getter
  @Setter
  public static class TerritorySuggestion {
    private int id;
    private String name;
    private String territorialAuthorityLogo;
  }
}
