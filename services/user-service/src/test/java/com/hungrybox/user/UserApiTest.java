package com.hungrybox.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerLoginAndReadProfile() throws Exception {
        String register = """
                {"fullName":"Priya Raman","email":"Priya@Example.com","password":"supersecret1","phone":"+91 98765 43210"}
                """;
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(register))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("priya@example.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));

        String token = login("priya@example.com", "supersecret1");

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Priya Raman"));

        mvc.perform(put("/api/users/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Priya R\",\"phone\":\"9876543210\",\"address\":\"12 North Masi St, Madurai\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("12 North Masi St, Madurai"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String body = """
                {"fullName":"Arun","email":"arun@example.com","password":"password123"}
                """;
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.dev\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileRequiresToken() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminsCanListUsers() throws Exception {
        String adminToken = login("admin@test.dev", "Admin@12345");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ADMIN"));

        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Kavin\",\"email\":\"kavin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        String customerToken = login("kavin@example.com", "password123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }
}
