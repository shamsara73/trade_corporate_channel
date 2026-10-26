package com.bank.tradechannel.web.finance;

import com.bank.tradechannel.domain.finance.FinanceRequest;
import com.bank.tradechannel.domain.finance.FinanceRequestService;
import com.bank.tradechannel.domain.finance.InterestFrom;
import com.bank.tradechannel.domain.finance.Money;
import com.bank.tradechannel.domain.finance.TradeGateway.GatewayUnavailableException;
import com.bank.tradechannel.web.CustomerProperties;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Finance Standalone request (TFFINNEW): fill in, send, see the references. */
@Controller
@RequestMapping("/finance")
class FinanceController {
  private final FinanceRequestService service;
  private final CustomerProperties customer;
  private final FinanceProperties properties;
  private final Clock clock;

  FinanceController(
      FinanceRequestService service,
      CustomerProperties customer,
      FinanceProperties properties,
      Clock clock) {
    this.service = service;
    this.customer = customer;
    this.properties = properties;
    this.clock = clock;
  }

  @ModelAttribute
  void common(Model model) {
    model.addAttribute("customer", customer.toCustomer());
    model.addAttribute("currencies", properties.currencies());
    model.addAttribute("productTypes", properties.productTypes());
    model.addAttribute("interestOptions", InterestFrom.values());
  }

  @GetMapping("/new")
  String newRequest(Model model) {
    var form = new FinanceForm();
    form.setCurrency(properties.currencies().get(0));
    form.setProductType(properties.productTypes().get(0));
    form.setStartDate(LocalDate.now(clock));
    form.setTenorDays(properties.defaultTenorDays());
    form.setInterestFrom(InterestFrom.AFTER.name());
    model.addAttribute("form", form);
    return "finance/new";
  }

  @PostMapping
  String send(
      @Valid @ModelAttribute("form") FinanceForm form,
      BindingResult errors,
      RedirectAttributes redirect) {
    Optional<FinanceRequest> request =
        errors.hasErrors() ? Optional.empty() : toRequest(form, errors);
    if (request.isEmpty()) {
      return "finance/new";
    }
    try {
      var sent = service.send(request.get(), customer.toCustomer());
      redirect.addFlashAttribute("sent", sent);
      return "redirect:/finance/sent";
    } catch (GatewayUnavailableException e) {
      errors.reject("finance.error.unavailable");
      return "finance/new";
    }
  }

  @GetMapping("/sent")
  String sent(Model model) {
    return model.containsAttribute("sent") ? "finance/sent" : "redirect:/finance/new";
  }

  /**
   * Checks the annotations cannot do: the choice lists come from configuration, and an amount's
   * decimals depend on its currency.
   */
  private Optional<FinanceRequest> toRequest(FinanceForm form, BindingResult errors) {
    if (!properties.productTypes().contains(form.getProductType())) {
      errors.rejectValue("productType", "finance.error.choice");
    }
    if (!properties.currencies().contains(form.getCurrency())) {
      errors.rejectValue("currency", "finance.error.choice");
      return Optional.empty();
    }
    Currency currency = Currency.getInstance(form.getCurrency());
    Optional<Money> finance = money(form.getFinanceAmount(), currency, "financeAmount", errors);
    String transaction =
        form.getTransactionAmount().isBlank()
            ? form.getFinanceAmount()
            : form.getTransactionAmount();
    Optional<Money> underlying = money(transaction, currency, "transactionAmount", errors);
    if (finance.isEmpty() || underlying.isEmpty() || errors.hasErrors()) {
      return Optional.empty();
    }
    return Optional.of(
        new FinanceRequest(
            form.getTheirReference().strip(),
            finance.get(),
            underlying.get(),
            form.getStartDate(),
            form.getTenorDays(),
            form.isTrustReceiptLoan(),
            InterestFrom.valueOf(form.getInterestFrom()),
            form.getProductType(),
            form.getGoodsDescription().strip(),
            form.getMessageToBank().strip(),
            form.getAdditionalInformation().strip()));
  }

  private static Optional<Money> money(
      String value, Currency currency, String field, BindingResult errors) {
    try {
      return Optional.of(new Money(new BigDecimal(value), currency));
    } catch (IllegalArgumentException e) {
      errors.rejectValue(
          field, "finance.error.decimals", new Object[] {currency.getCurrencyCode()}, null);
      return Optional.empty();
    }
  }
}
