package com.hungrybox.billing;

import com.hungrybox.billing.BillingDtos.BillingSummary;
import com.hungrybox.billing.BillingDtos.Invoice;
import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.security.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/invoices")
    public List<Invoice> myInvoices(@CurrentUser AuthenticatedUser user) {
        return billingService.invoicesFor(user);
    }

    @GetMapping("/invoices/all")
    public List<Invoice> allInvoices(@CurrentUser AuthenticatedUser user) {
        return billingService.allInvoices(user);
    }

    @GetMapping("/invoices/order/{orderId}")
    public Invoice invoiceForOrder(@CurrentUser AuthenticatedUser user, @PathVariable long orderId) {
        return billingService.invoiceForOrder(user, orderId);
    }

    @GetMapping("/summary")
    public BillingSummary summary(@CurrentUser AuthenticatedUser user) {
        return billingService.summary(user);
    }
}
