package org.sitmun.authorization.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_GROUP_ID_PREFIX;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.ApplicationBackgroundDto;
import org.sitmun.authorization.client.dto.ProfileDto;
import org.sitmun.authorization.client.service.ApplicationBackgroundView;
import org.sitmun.authorization.client.service.Profile;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;

@DisplayName("ProfileMapper application background order")
class ProfileMapperApplicationBackgroundOrderTest {

  private final ProfileMapper mapper =
      new ProfileMapper() {
        @Override
        public ProfileDto map(Profile profile, Application application, Territory territory) {
          return null;
        }
      };

  @Test
  void mapApplicationBackgroundView_includesOrder() {
    ApplicationBackgroundView applicationBackground =
        ApplicationBackgroundView.builder()
            .id(PROFILE_GROUP_ID_PREFIX + 10)
            .title("Ortho")
            .thumbnail("thumb.png")
            .order(7)
            .groupId(10)
            .build();

    ApplicationBackgroundDto dto = mapper.map(applicationBackground);

    assertThat(dto.getOrder()).isEqualTo(7);
    assertThat(dto.getTitle()).isEqualTo("Ortho");
    assertThat(dto.getId()).isEqualTo(PROFILE_GROUP_ID_PREFIX + 10);
    assertThat(dto.getThumbnail()).isEqualTo("thumb.png");
  }
}
