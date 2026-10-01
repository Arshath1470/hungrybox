package com.hungrybox.order;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungrybox.common.events.PaymentCompletedEvent;
import com.hungrybox.common.events.Topics;
import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.security.JwtService;
import com.hungrybox.order.OrderDtos.MenuItemSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiTest {

    private static final long RESTAURANT_ID = 1L;
    private static final long CUSTOMER_ID = 7L;

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PaymentEventsListener paymentListener;

    @MockitoBean
    RestaurantClient restaurantClient;

    // Replace Boot's auto-configured template by name so no broker is needed.
    @MockitoBean(name = "kafkaTemplate")
    KafkaTemplate<String, String> kafkaTemplate;

    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        reset(kafkaTemplate);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(restaurantClient.menuItems(anyCollection())).thenReturn(List.of(
                new MenuItemSnapshot(1L, RESTAURANT_ID, "Ghee Podi Idli", new BigDecimal("90.00"), true),
                new MenuItemSnapshot(2L, RESTAURANT_ID, "Masala Dosa", new BigDecimal("110.00"), true),
                new MenuItemSnapshot(3L, RESTAURANT_ID, "Out Of Stock Vada", new BigDecimal("50.00"), false),
                new MenuItemSnapshot(9L, 2L, "Mutton Biryani", new BigDecimal("320.00"), true)));
        customerToken = jwtService.issue(CUSTOMER_ID, AuthenticatedUser.ROLE_CUSTOMER);
        adminToken = jwtService.issue(1L, AuthenticatedUser.ROLE_ADMIN);
    }

    @Test
    void placingAnOrderPricesItServerSideAndPublishesAnEvent() throws Exception {
        // 2 x 90 + 1 x 110 = 290 subtotal, 40 delivery, 14.50 GST
        long orderId = placeOrder("""
                {"restaurantId":1,"deliveryAddress":"5 West Veli St, Madurai",
                 "items":[{"menuItemId":1,"quantity":1},{"menuItemId":2,"quantity":1},{"menuItemId":1,"quantity":1}]}
                """);

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.subtotal").value(290.0))
                .andExpect(jsonPath("$.deliveryFee").value(40.0))
                .andExpect(jsonPath("$.tax").value(14.5))
                .andExpect(jsonPath("$.total").value(344.5))
                .andExpect(jsonPath("$.items.length()").value(2));

        verify(kafkaTemplate, timeout(2000)).send(eq(Topics.ORDER_PLACED), eq(String.valueOf(orderId)), anyString());
    }

    @Test
    void paymentEventConfirmsOrderAndCustomerCanCancel() throws Exception {
        long orderId = placeOrder("""
                {"restaurantId":1,"deliveryAddress":"Anna Nagar, Madurai","items":[{"menuItemId":2,"quantity":2}]}
                """);

        paymentListener.onPaymentCompleted(objectMapper.writeValueAsString(new PaymentCompletedEvent(
                orderId, CUSTOMER_ID, PaymentCompletedEvent.PAID, "HB-TEST-1", new BigDecimal("271.00"), null,
                Instant.now())));

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.invoiceNumber").value("HB-TEST-1"));

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(kafkaTemplate, timeout(2000)).send(eq(Topics.ORDER_CANCELLED), eq(String.valueOf(orderId)), anyString());

        // A second cancel is a conflict, and a replayed payment event changes nothing.
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict());
        paymentListener.onPaymentCompleted(objectMapper.writeValueAsString(new PaymentCompletedEvent(
                orderId, CUSTOMER_ID, PaymentCompletedEvent.PAID, "HB-TEST-1", new BigDecimal("271.00"), null,
                Instant.now())));
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void failedPaymentMarksOrderFailed() throws Exception {
        long orderId = placeOrder("""
                {"restaurantId":1,"deliveryAddress":"KK Nagar, Madurai","items":[{"menuItemId":1,"quantity":1}]}
                """);

        paymentListener.onPaymentCompleted(objectMapper.writeValueAsString(new PaymentCompletedEvent(
                orderId, CUSTOMER_ID, PaymentCompletedEvent.FAILED, "HB-TEST-2", new BigDecimal("134.50"),
                "Card declined", Instant.now())));

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.statusReason").value("Card declined"));
    }

    @Test
    void adminDrivesKitchenAndDeliverySteps() throws Exception {
        long orderId = placeOrder("""
                {"restaurantId":1,"deliveryAddress":"Tallakulam, Madurai","items":[{"menuItemId":2,"quantity":1}]}
                """);

        // Admins cannot fake a payment outcome or skip ahead.
        updateStatus(orderId, adminToken, "CONFIRMED").andExpect(status().isBadRequest());
        updateStatus(orderId, adminToken, "PREPARING").andExpect(status().isConflict());

        paymentListener.onPaymentCompleted(objectMapper.writeValueAsString(new PaymentCompletedEvent(
                orderId, CUSTOMER_ID, PaymentCompletedEvent.PAID, "HB-TEST-3", new BigDecimal("155.50"), null,
                Instant.now())));

        updateStatus(orderId, customerToken, "PREPARING").andExpect(status().isForbidden());
        updateStatus(orderId, adminToken, "PREPARING").andExpect(status().isOk());
        updateStatus(orderId, adminToken, "OUT_FOR_DELIVERY").andExpect(status().isOk());
        updateStatus(orderId, adminToken, "DELIVERED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        mvc.perform(get("/api/orders/all").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/all").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidOrdersAreRejected() throws Exception {
        // items from two restaurants
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\":1,\"deliveryAddress\":\"x\",\"items\":[{\"menuItemId\":1,\"quantity\":1},{\"menuItemId\":9,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest());

        // unavailable item
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\":1,\"deliveryAddress\":\"x\",\"items\":[{\"menuItemId\":3,\"quantity\":1}]}"))
                .andExpect(status().isConflict());

        // unknown item
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\":1,\"deliveryAddress\":\"x\",\"items\":[{\"menuItemId\":404,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest());

        // bean validation
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\":1,\"deliveryAddress\":\"\",\"items\":[{\"menuItemId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());

        // no token
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customersCannotSeeEachOthersOrders() throws Exception {
        long orderId = placeOrder("""
                {"restaurantId":1,"deliveryAddress":"Simmakkal, Madurai","items":[{"menuItemId":1,"quantity":1}]}
                """);
        String otherCustomer = jwtService.issue(99L, AuthenticatedUser.ROLE_CUSTOMER);

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + otherCustomer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private long placeOrder(String body) throws Exception {
        String response = mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(long orderId, String token, String status)
            throws Exception {
        return mvc.perform(patch("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"));
    }
}
