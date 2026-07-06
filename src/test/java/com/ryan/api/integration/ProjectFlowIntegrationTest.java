package com.ryan.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullProjectAndTaskLifecycle() throws Exception {
        String ownerToken = registerAndGetToken("Ada Lovelace", "ada@taskflow.dev", "S3cret!23");
        String outsiderToken = registerAndGetToken("Grace Hopper", "grace@taskflow.dev", "S3cret!23");

        String projectId = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("""
                                {"name": "TaskFlow", "description": "Portfolio project"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("TaskFlow")))
                .andReturn().getResponse().getContentAsString();

        String projectIdValue = objectMapper.readTree(projectId).get("id").asText();

        mockMvc.perform(get("/api/projects/" + projectIdValue)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        String taskJson = mockMvc.perform(post("/api/projects/" + projectIdValue + "/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("""
                                {"title": "Design database schema", "priority": "HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("TODO")))
                .andReturn().getResponse().getContentAsString();

        String taskId = objectMapper.readTree(taskJson).get("id").asText();

        mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("""
                                {"content": "Started working on this"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType("application/json")
                        .content("""
                                {"content": "I should not be able to comment"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Design database schema")));
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"name": "Weak Password", "email": "weak@taskflow.dev", "password": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private String registerAndGetToken(String name, String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("name", name);
                            put("email", email);
                            put("password", password);
                        }})))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("token").asText();
    }
}
