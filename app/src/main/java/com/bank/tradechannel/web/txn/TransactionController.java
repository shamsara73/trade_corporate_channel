package com.bank.tradechannel.web.txn;

import com.bank.tradechannel.domain.finance.FsaService;
import com.bank.tradechannel.domain.finance.FsaService.TxnView;
import com.bank.tradechannel.domain.finance.Money;
import com.bank.tradechannel.domain.finance.RepaymentAction;
import com.bank.tradechannel.domain.finance.RepaymentPayer;
import com.bank.tradechannel.domain.finance.RepaymentRefusedException;
import com.bank.tradechannel.domain.finance.RepaymentRequest;
import com.bank.tradechannel.domain.txn.StaleUpdateException;
import com.bank.tradechannel.web.CustomerProperties;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The customer's transactions and their events. Every lookup is scoped to the signed-in customer; a
 * transaction of another customer is "not found" (ground rule 8).
 */
@Controller
@RequestMapping("/transactions")
class TransactionController {
  private final FsaService service;
  private final CustomerProperties customer;
  private final Clock clock;

  TransactionController(FsaService service, CustomerProperties customer, Clock clock) {
    this.service = service;
    this.customer = customer;
    this.clock = clock;
  }

  @GetMapping
  String list(Model model) {
    model.addAttribute("transactions", service.list(customer.toCustomer()));
    return "transactions/list";
  }

  @GetMapping("/{id}")
  String detail(@PathVariable UUID id, Model model) {
    TxnView view = view(id);
    model.addAttribute("view", view);
    model.addAttribute("autoRefresh", view.waiting());
    return "transactions/detail";
  }

  @GetMapping("/{id}/repay")
  String repayForm(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
    TxnView view = view(id);
    if (!view.canRepay()) {
      redirect.addFlashAttribute("notice", view.whyNoRepay().orElseThrow());
      return "redirect:/transactions/" + id;
    }
    var form = new RepayForm();
    form.setAction(RepaymentAction.FINAL.name());
    form.setPayer(RepaymentPayer.DEBIT_PARTY.name());
    form.setValueDate(LocalDate.now(clock));
    form.setPrincipal(view.txn().outstanding().map(BigDecimal::toPlainString).orElse(""));
    form.setInterest("");
    model.addAttribute("form", form);
    return repayPage(view, model);
  }

  @PostMapping("/{id}/repay")
  String repay(
      @PathVariable UUID id,
      @Valid @ModelAttribute("form") RepayForm form,
      BindingResult errors,
      Model model,
      Principal user,
      RedirectAttributes redirect) {
    TxnView view = view(id);
    if (!errors.hasErrors()) {
      try {
        Currency currency = Currency.getInstance(view.txn().currencyCode());
        var request =
            new RepaymentRequest(
                form.getRepaymentReference().strip(),
                RepaymentAction.valueOf(form.getAction()),
                form.getValueDate(),
                RepaymentPayer.valueOf(form.getPayer()),
                new Money(new BigDecimal(form.getPrincipal()), currency),
                form.getInterest().isBlank() ? BigDecimal.ZERO : interest(form.getInterest(), currency));
        service.repay(customer.toCustomer(), id, request, user.getName());
        return "redirect:/transactions/" + id;
      } catch (RepaymentRefusedException e) {
        e.reason().ifPresent(errors::reject);
        e.fieldErrors().forEach(errors::rejectValue);
      } catch (StaleUpdateException e) {
        errors.reject("fsa.repay.concurrent");
      } catch (IllegalArgumentException e) {
        errors.rejectValue("principal", "finance.error.decimals", new Object[] {view.txn().currencyCode()}, null);
      }
    }
    return repayPage(view, model);
  }

  @GetMapping("/{id}/documents/{documentId}")
  ResponseEntity<byte[]> document(@PathVariable UUID id, @PathVariable UUID documentId) {
    return service
        .document(customer.toCustomer(), id, documentId)
        .map(
            d ->
                ResponseEntity.ok()
                    .contentType(mediaType(d.document().mimeType()))
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                            .filename(fileName(d.document().documentId(), d.document().mimeType()))
                            .build()
                            .toString())
                    .body(d.content()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private String repayPage(TxnView view, Model model) {
    model.addAttribute("view", view);
    model.addAttribute("actions", RepaymentAction.values());
    model.addAttribute("payers", RepaymentPayer.values());
    return "transactions/repay";
  }

  private TxnView view(UUID id) {
    return service
        .view(customer.toCustomer(), id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static BigDecimal interest(String value, Currency currency) {
    BigDecimal amount = new BigDecimal(value);
    if (amount.stripTrailingZeros().scale() > currency.getDefaultFractionDigits()) {
      throw new IllegalArgumentException("too many decimals");
    }
    return amount;
  }

  private static MediaType mediaType(String mime) {
    try {
      return MediaType.parseMediaType(mime);
    } catch (IllegalArgumentException e) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  private static String fileName(String documentId, String mime) {
    String base = documentId.isBlank() ? "document" : documentId.replaceAll("[^A-Za-z0-9._-]", "_");
    return "application/pdf".equals(mime) ? base + ".pdf" : base;
  }
}
