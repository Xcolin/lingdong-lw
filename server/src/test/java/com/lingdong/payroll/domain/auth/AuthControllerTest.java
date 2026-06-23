package com.lingdong.payroll.domain.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seededAdminCanLoginWithDatabasePassword() throws Exception {
        jdbcTemplate.update("update sys_user set enabled = 1 where username = 'admin'");
        String body = objectMapper.writeValueAsString(new LoginBody("admin", "admin123"));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(response).get("data");

        assertThat(data.get("username").asText()).isEqualTo("admin");
        assertThat(data.get("roleCodes")).extracting(JsonNode::asText).contains("ADMIN");
        assertThat(data.get("permissions")).extracting(JsonNode::asText).contains("user:view", "batch:view");
    }

    @Test
    void disabledUserCannotLoginEvenWithCorrectPassword() throws Exception {
        jdbcTemplate.update("update sys_user set enabled = 0 where username = 'admin'");

        String body = objectMapper.writeValueAsString(new LoginBody("admin", "admin123"));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(objectMapper.readTree(response).get("message").asText()).isEqualTo("用户名或密码错误");
    }

    @Test
    void loginReturnsDatabaseUserRolesAndGrantedPermissions() throws Exception {
        jdbcTemplate.update(
                "insert into sys_user(username, password_hash, display_name, enabled) values (?, ?, ?, 1)",
                "operator1",
                passwordEncoder.encode("op123456"),
                "录入员一"
        );
        jdbcTemplate.update(
                "insert into sys_user_role(user_id, role_id) " +
                        "select u.id, r.id from sys_user u, sys_role r where u.username = 'operator1' and r.code = 'OPERATOR'"
        );
        jdbcTemplate.update(
                "insert into sys_user_permission(user_id, permission_id) " +
                        "select u.id, p.id from sys_user u, sys_permission p where u.username = 'operator1' and p.code in ('batch:view', 'batch:create')"
        );

        String body = objectMapper.writeValueAsString(new LoginBody("operator1", "op123456"));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(response).get("data");

        assertThat(data.get("username").asText()).isEqualTo("operator1");
        assertThat(data.get("displayName").asText()).isEqualTo("录入员一");
        assertThat(data.get("roleCodes")).extracting(JsonNode::asText).containsExactly("OPERATOR");
        assertThat(data.get("permissions")).extracting(JsonNode::asText).containsExactlyInAnyOrder("batch:view", "batch:create");
    }

    @Test
    void protectedApiWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/batches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedApiWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/batches")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    private record LoginBody(String username, String password) {
    }
}
