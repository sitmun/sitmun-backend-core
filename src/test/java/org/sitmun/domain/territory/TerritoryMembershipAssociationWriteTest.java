package org.sitmun.domain.territory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.sitmun.test.URIConstants.TERRITORY_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Territory membership association writes")
class TerritoryMembershipAssociationWriteTest {

  private static final String MEMBERS_URI = TERRITORY_URI + "/members";
  private static final String MEMBER_OF_URI = TERRITORY_URI + "/memberOf";

  @Autowired private MockMvc mvc;
  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private final List<Territory> territories = new ArrayList<>();

  @AfterEach
  void cleanup() {
    for (Territory territory : territories) {
      jdbcTemplate.update(
          "DELETE FROM STM_GRP_TER WHERE GTE_TERID = ? OR GTE_TERMID = ?",
          territory.getId(),
          territory.getId());
    }
    territories.forEach(territory -> territoryRepository.deleteById(territory.getId()));
    territories.clear();
  }

  @Test
  @DisplayName("PUT members inserts one STM_GRP_TER row visible from both sides")
  @WithMockUser(roles = "ADMIN")
  void putMembersInsertsOneJoinRowVisibleFromBothSides() throws Exception {
    Territory parent = territory("parent");
    Territory child = territory("child");

    putAssociation(MEMBERS_URI, parent.getId(), child);

    assertThat(joinRows(parent.getId(), child.getId())).isEqualTo(1);
    assertMembersContains(parent, child);
    assertMemberOfContains(child, parent);
  }

  @Test
  @DisplayName("PUT memberOf inserts the same STM_GRP_TER pair")
  @WithMockUser(roles = "ADMIN")
  void putMemberOfInsertsTheSamePair() throws Exception {
    Territory parent = territory("parent");
    Territory child = territory("child");

    putAssociation(MEMBER_OF_URI, child.getId(), parent);

    assertThat(joinRows(parent.getId(), child.getId())).isEqualTo(1);
    assertMembersContains(parent, child);
    assertMemberOfContains(child, parent);
  }

  @Test
  @DisplayName("Replacing members leaves the member-of row")
  @WithMockUser(roles = "ADMIN")
  void replacingMembersLeavesTheMemberOfRow() throws Exception {
    Territory center = territory("center");
    Territory child = territory("child");
    Territory parent = territory("parent");
    Territory replacement = territory("next");

    putAssociation(MEMBERS_URI, center.getId(), child);
    putAssociation(MEMBER_OF_URI, center.getId(), parent);

    putAssociation(MEMBERS_URI, center.getId(), replacement);

    assertThat(joinRows(parent.getId(), center.getId())).isEqualTo(1);
    assertThat(joinRows(center.getId(), replacement.getId())).isEqualTo(1);
    assertThat(joinRows(center.getId(), child.getId())).isZero();
    assertMemberOfContains(center, parent);
    assertMembersContains(center, replacement);
  }

  @Test
  @DisplayName("Replacing memberOf leaves the members row")
  @WithMockUser(roles = "ADMIN")
  void replacingMemberOfLeavesTheMembersRow() throws Exception {
    Territory center = territory("center");
    Territory child = territory("child");
    Territory parent = territory("parent");
    Territory replacement = territory("next");

    putAssociation(MEMBERS_URI, center.getId(), child);
    putAssociation(MEMBER_OF_URI, center.getId(), parent);

    putAssociation(MEMBER_OF_URI, center.getId(), replacement);

    assertThat(joinRows(center.getId(), child.getId())).isEqualTo(1);
    assertThat(joinRows(replacement.getId(), center.getId())).isEqualTo(1);
    assertThat(joinRows(parent.getId(), center.getId())).isZero();
    assertMembersContains(center, child);
    assertMemberOfContains(center, replacement);
  }

  private Territory territory(String label) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Territory territory =
        territoryRepository.save(
            Territory.builder()
                .name(label + "-" + suffix)
                .code(label + "-" + suffix)
                .blocked(false)
                .build());
    territories.add(territory);
    return territory;
  }

  private void putAssociation(String uri, Integer ownerId, Territory target) throws Exception {
    mvc.perform(
            put(uri, ownerId)
                .content(TERRITORY_URI.replace("{0}", target.getId().toString()))
                .contentType("text/uri-list"))
        .andExpect(status().isNoContent());
  }

  private void assertMembersContains(Territory owner, Territory member) throws Exception {
    mvc.perform(get(MEMBERS_URI, owner.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.territories[*].name", hasItem(member.getName())));
  }

  private void assertMemberOfContains(Territory member, Territory parent) throws Exception {
    mvc.perform(get(MEMBER_OF_URI, member.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.territories[*].name", hasItem(parent.getName())));
  }

  private Integer joinRows(Integer territoryId, Integer memberId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM STM_GRP_TER WHERE GTE_TERID = ? AND GTE_TERMID = ?",
        Integer.class,
        territoryId,
        memberId);
  }
}
