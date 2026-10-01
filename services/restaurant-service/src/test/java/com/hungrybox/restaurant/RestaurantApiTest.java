package com.hungrybox.restaurant;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.security.JwtService;
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
class RestaurantApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @Test
    void catalogueIsPublicAndSeeded() throws Exception {
        mvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))));

        mvc.perform(get("/api/restaurants").param("cuisine", "biryani"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Vaigai Biryani House"));

        mvc.perform(get("/api/restaurants").param("q", "parotta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Midnight Parotta Point"));
    }

    @Test
    void detailIncludesMenu() throws Exception {
        mvc.perform(get("/api/restaurants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurant.id").value(1))
                .andExpect(jsonPath("$.menu", hasSize(greaterThanOrEqualTo(3))));

        mvc.perform(get("/api/restaurants/999")).andExpect(status().isNotFound());
    }

    @Test
    void menuItemLookupReturnsPricesForOrderService() throws Exception {
        mvc.perform(get("/api/restaurants/menu-items").param("ids", "1,2,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].price").isNumber());
    }

    @Test
    void onlyAdminsCanChangeTheCatalogue() throws Exception {
        String body = """
                {"name":"Test Kitchen","cuisine":"Fusion","city":"Chennai","rating":4.1,"deliveryTimeMins":25}
                """;
        mvc.perform(post("/api/restaurants").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        String customer = jwtService.issue(10L, AuthenticatedUser.ROLE_CUSTOMER);
        mvc.perform(post("/api/restaurants").header("Authorization", "Bearer " + customer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        String admin = jwtService.issue(1L, AuthenticatedUser.ROLE_ADMIN);
        String created = mvc.perform(post("/api/restaurants").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Kitchen"))
                .andReturn().getResponse().getContentAsString();
        long restaurantId = Long.parseLong(created.replaceAll(".*\"id\":(\\d+).*", "$1"));

        String item = mvc.perform(post("/api/restaurants/" + restaurantId + "/menu-items")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fusion Dosa\",\"category\":\"Mains\",\"price\":150.00,\"veg\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(true))
                .andReturn().getResponse().getContentAsString();
        long itemId = Long.parseLong(item.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mvc.perform(patch("/api/restaurants/menu-items/" + itemId + "/availability")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
