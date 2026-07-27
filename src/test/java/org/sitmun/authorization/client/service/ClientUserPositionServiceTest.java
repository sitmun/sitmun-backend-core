package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionDTO;
import org.sitmun.domain.user.position.UserPositionMapper;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientUserPositionService")
class ClientUserPositionServiceTest {

  private static final String USERNAME = "alice";

  @Mock private UserApplicationAccessPolicy userApplicationAccessPolicy;
  @Mock private UserPositionRepository userPositionRepository;
  @Mock private UserPositionMapper userPositionMapper;

  @InjectMocks private ClientUserPositionService clientUserPositionService;

  private User owner;
  private UserPosition position;
  private UserPositionDTO requestDto;

  @BeforeEach
  void setUp() {
    owner = new User();
    owner.setId(10);
    owner.setUsername(USERNAME);

    Territory territory = Territory.builder().id(20).name("Territory A").build();

    position =
        UserPosition.builder()
            .id(1)
            .name("Old name")
            .organization("Old org")
            .email("old@example.com")
            .type("ADMIN")
            .user(owner)
            .territory(territory)
            .build();

    requestDto =
        UserPositionDTO.builder()
            .id(1)
            .name("New name")
            .organization("New org")
            .email("new@example.com")
            .expirationDate(new Date())
            .type("USER")
            .userId(99)
            .territoryId(99)
            .build();
  }

  @Test
  @DisplayName("rejects null id with 400")
  void updateOwnedPositionRejectsNullId() {
    requestDto.setId(null);

    assertThatThrownBy(() -> clientUserPositionService.updateOwnedPosition(USERNAME, requestDto))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));

    verify(userPositionRepository, never()).findById(any());
  }

  @Test
  @DisplayName("rejects missing position with 404")
  void updateOwnedPositionRejectsMissingId() {
    when(userApplicationAccessPolicy.mayUseClientConfigEndpoints(USERNAME)).thenReturn(true);
    when(userPositionRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> clientUserPositionService.updateOwnedPosition(USERNAME, requestDto))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  @DisplayName("rejects foreign owner with AccessDeniedException")
  void updateOwnedPositionRejectsForeignOwner() {
    when(userApplicationAccessPolicy.mayUseClientConfigEndpoints("bob")).thenReturn(true);
    when(userPositionRepository.findById(1)).thenReturn(Optional.of(position));

    assertThatThrownBy(() -> clientUserPositionService.updateOwnedPosition("bob", requestDto))
        .isInstanceOf(AccessDeniedException.class);

    verify(userPositionRepository, never()).save(any());
  }

  @Test
  @DisplayName("rejects blocked user with AccessDeniedException")
  void updateOwnedPositionRejectsBlockedUser() {
    when(userApplicationAccessPolicy.mayUseClientConfigEndpoints(USERNAME)).thenReturn(false);

    assertThatThrownBy(() -> clientUserPositionService.updateOwnedPosition(USERNAME, requestDto))
        .isInstanceOf(AccessDeniedException.class);

    verify(userPositionRepository, never()).findById(any());
  }

  @Test
  @DisplayName("persists only mutable fields for owner")
  void updateOwnedPositionPersistsOnlyMutableFields() {
    when(userApplicationAccessPolicy.mayUseClientConfigEndpoints(USERNAME)).thenReturn(true);
    when(userPositionRepository.findById(1)).thenReturn(Optional.of(position));
    when(userPositionRepository.save(any(UserPosition.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(userPositionMapper.toDto(any(UserPosition.class)))
        .thenAnswer(
            inv -> {
              UserPosition saved = inv.getArgument(0);
              return UserPositionDTO.builder()
                  .id(saved.getId())
                  .name(saved.getName())
                  .organization(saved.getOrganization())
                  .email(saved.getEmail())
                  .expirationDate(saved.getExpirationDate())
                  .type(saved.getType())
                  .userId(saved.getUser().getId())
                  .territoryId(saved.getTerritory().getId())
                  .build();
            });

    UserPositionDTO result = clientUserPositionService.updateOwnedPosition(USERNAME, requestDto);

    ArgumentCaptor<UserPosition> captor = ArgumentCaptor.forClass(UserPosition.class);
    verify(userPositionRepository).save(captor.capture());
    UserPosition saved = captor.getValue();

    assertThat(saved.getName()).isEqualTo("New name");
    assertThat(saved.getOrganization()).isEqualTo("New org");
    assertThat(saved.getEmail()).isEqualTo("new@example.com");
    assertThat(saved.getExpirationDate()).isEqualTo(requestDto.getExpirationDate());
    assertThat(saved.getType()).isEqualTo("USER");
    assertThat(saved.getUser().getId()).isEqualTo(10);
    assertThat(saved.getTerritory().getId()).isEqualTo(20);
    assertThat(result.getName()).isEqualTo("New name");
  }
}
