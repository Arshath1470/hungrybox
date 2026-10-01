package com.hungrybox.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungrybox.billing.BillingDtos.Invoice;
import com.hungrybox.billing.BillingDtos.InvoiceStatus;
import com.hungrybox.common.events.OrderCancelledEvent;
import com.hungrybox.common.events.OrderPlacedEvent;
import com.hungrybox.common.events.Topics;
import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.security.JwtService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingFlowTest {

    private static final long CUSTOMER_ID = 21L;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtService jwtService;

    @Autowired
    OrderEventsListener listener;

    @Autowired
    InvoiceRepository invoices;

    // Replace Boot's auto-configured template by name so no broker is needed.
    @MockitoBean(name = "kafkaTemplate")
    KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        reset(kafkaTemplate);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void paysOrderAndPublishesResultExactlyOnce() throws Exception {
        String event = orderPlaced(1001L, "386.50");

        listener.onOrderPlaced(event);
        listener.onOrderPlaced(event); // redelivery must not double-bill

        Invoice invoice = invoices.findByOrder(1001L).orElseThrow();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.total()).isEqualByComparingTo("386.50");
        assertThat(invoice.invoiceNumber()).endsWith("-001001");
        verify(kafkaTemplate, times(1)).send(eq(Topics.PAYMENT_COMPLETED), eq("1001"),
                argThat(json -> json.contains("\"status\":\"PAID\"")));
    }

    @Test
    void declinesOrdersAboveTheLimit() throws Exception {
        listener.onOrderPlaced(orderPlaced(1002L, "1500.00"));

        Invoice invoice = invoices.findByOrder(1002L).orElseThrow();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.FAILED);
        assertThat(invoice.failureReason()).contains("limit");
        verify(kafkaTemplate).send(eq(Topics.PAYMENT_COMPLETED), eq("1002"),
                argThat(json -> json.contains("\"status\":\"FAILED\"")));
    }

    @Test
    void cancellationRefundsPaidInvoice() throws Exception {
        listener.onOrderPlaced(orderPlaced(1003L, "250.00"));
        listener.onOrderCancelled(objectMapper.writeValueAsString(
                new OrderCancelledEvent(1003L, CUSTOMER_ID, "Cancelled by customer", Instant.now())));

        Invoice invoice = invoices.findByOrder(1003L).orElseThrow();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.REFUNDED);
        assertThat(invoice.refundReference()).startsWith("RFD-");
    }

    @Test
    void invoiceAccessIsScopedToOwnerAndAdmins() throws Exception {
        listener.onOrderPlaced(orderPlaced(1004L, "120.00"));
        String owner = jwtService.issue(CUSTOMER_ID, AuthenticatedUser.ROLE_CUSTOMER);
        String stranger = jwtService.issue(999L, AuthenticatedUser.ROLE_CUSTOMER);
        String admin = jwtService.issue(1L, AuthenticatedUser.ROLE_ADMIN);

        mvc.perform(get("/api/billing/invoices/order/1004").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(get("/api/billing/invoices/order/1004").header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/billing/invoices").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value((int) CUSTOMER_ID));

        mvc.perform(get("/api/billing/summary").header("Authorization", "Bearer " + owner))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/billing/summary").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidCount").isNumber())
                .andExpect(jsonPath("$.last7Days").isArray());
    }

    private String orderPlaced(long orderId, String total) throws Exception {
        BigDecimal amount = new BigDecimal(total);
        return objectMapper.writeValueAsString(new OrderPlacedEvent(orderId, CUSTOMER_ID, 1L, amount,
                BigDecimal.ZERO, BigDecimal.ZERO, amount, Instant.now()));
    }
}
