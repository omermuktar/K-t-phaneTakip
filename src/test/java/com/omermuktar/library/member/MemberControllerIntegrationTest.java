package com.omermuktar.library.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private MemberRepository memberRepository;

  @Test
  void createMemberNormalizesFieldsAndSetsLocation() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "membershipNumber": "mem-100",
                      "fullName": " Ömer Muktar ",
                      "email": "OMER@EXAMPLE.COM"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.membershipNumber").value("MEM-100"))
        .andExpect(jsonPath("$.fullName").value("Ömer Muktar"))
        .andExpect(jsonPath("$.email").value("omer@example.com"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void duplicateNumberAndEmailReturnSpecificConflicts() throws Exception {
    memberRepository.saveAndFlush(Member.create("MEM-100", "Ömer", "omer@example.com"));

    mockMvc
        .perform(
            post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("mem-100", "Ada Lovelace", "ada@example.com")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MEMBERSHIP_NUMBER_EXISTS"));

    mockMvc
        .perform(
            post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("MEM-101", "Ada Lovelace", "OMER@EXAMPLE.COM")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MEMBER_EMAIL_EXISTS"));
  }

  @Test
  void getUpdateSearchAndDeleteFlowWorks() throws Exception {
    Member member =
        memberRepository.saveAndFlush(Member.create("MEM-100", "Ömer Muktar", "omer@example.com"));

    mockMvc
        .perform(get("/api/v1/members/{id}", member.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Ömer Muktar"));

    mockMvc
        .perform(
            put("/api/v1/members/{id}", member.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "membershipNumber": "MEM-200",
                      "fullName": "Ömer Şamil Muktar",
                      "email": "omer.samil@example.com",
                      "active": false
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.membershipNumber").value("MEM-200"))
        .andExpect(jsonPath("$.active").value(false));

    mockMvc
        .perform(
            get("/api/v1/members")
                .param("query", "şamil")
                .param("active", "false")
                .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(delete("/api/v1/members/{id}", member.getId()))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/v1/members/{id}", member.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void invalidMemberBodyReturnsValidationDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("x", "", "not-an-email")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.violations.length()").value(3));
  }

  @Test
  void updateCannotTakeAnotherMembersUniqueFields() throws Exception {
    Member first =
        memberRepository.saveAndFlush(Member.create("MEM-100", "Ömer", "omer@example.com"));
    Member second =
        memberRepository.saveAndFlush(Member.create("MEM-101", "Ada", "ada@example.com"));

    mockMvc
        .perform(
            put("/api/v1/members/{id}", second.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "membershipNumber": "%s",
                      "fullName": "Ada Lovelace",
                      "email": "ada@example.com",
                      "active": true
                    }
                    """
                        .formatted(first.getMembershipNumber())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MEMBERSHIP_NUMBER_EXISTS"));
  }

  private static String memberJson(String number, String name, String email) {
    return """
    {
      "membershipNumber": "%s",
      "fullName": "%s",
      "email": "%s"
    }
    """
        .formatted(number, name, email);
  }
}
