package com.mystreet.web;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de integración de punta a punta (perfil dev, H2 en memoria, seed incluido).
 * Automatiza la matriz de seguridad que antes se verificaba a mano con PowerShell.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@mystreet.com";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String PRODUCT_JSON =
            "{\"name\":\"Test Shoe\",\"brand\":\"Nike\",\"price\":99.90,\"sizes\":[\"9\"],\"stockQty\":3}";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    // ---------- helpers ----------

    private String json(Map<String, String> fields) throws Exception {
        return mapper.writeValueAsString(fields);
    }

    private String tokenFrom(String responseBody) throws Exception {
        return mapper.readTree(responseBody).get("token").asText();
    }

    private String adminToken() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return tokenFrom(body);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String registerUserToken() throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Test User", "email", uniqueEmail(),
                                "password", "password123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return tokenFrom(body);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- catálogo (público) ----------

    @Test
    void catalog_isPublicAndContainsSeedData() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(8)));
    }

    @Test
    void catalog_filtersByBrandIgnoringCase() throws Exception {
        mvc.perform(get("/api/products").param("brand", "nike"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].brand", everyItem(is("Nike"))));
    }

    @Test
    void catalog_filtersBySize() throws Exception {
        mvc.perform(get("/api/products").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Ultraboost Light")));
    }

    @Test
    void product_unknownId_returns404() throws Exception {
        mvc.perform(get("/api/products/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void product_malformedId_returns400() throws Exception {
        mvc.perform(get("/api/products/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ---------- autenticación ----------

    @Test
    void register_returnsTokenAndNeverCreatesAdmin() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Ana Prueba", "email", uniqueEmail(),
                                "password", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.isAdmin").value(false));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        String body = json(Map.of("fullName", "Ana", "email", email, "password", "password123"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidBody_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "", "email", "no-es-email", "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_admin_returnsAdminFlag() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAdmin").value(true));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", "incorrecta"))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- reglas de acceso sobre productos ----------

    @Test
    void createProduct_withoutToken_returns401() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void createProduct_withGarbageToken_returns401() throws Exception {
        mvc.perform(post("/api/products")
                        .header("Authorization", bearer("esto.no.es.un.token"))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_asRegularUser_returns403() throws Exception {
        mvc.perform(post("/api/products")
                        .header("Authorization", bearer(registerUserToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void admin_canCreateReadAndDeleteProduct() throws Exception {
        String token = adminToken();

        String created = mvc.perform(post("/api/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Shoe"))
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(created).get("id").asText();

        mvc.perform(get("/api/products/" + id)).andExpect(status().isOk());

        mvc.perform(delete("/api/products/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void createProduct_withInvalidData_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"brand\":\"Nike\",\"price\":-1,\"sizes\":[],\"stockQty\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void createProduct_withMalformedJson_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest());
    }
}
