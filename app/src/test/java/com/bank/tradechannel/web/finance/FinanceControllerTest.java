package com.bank.tradechannel.web.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.tradechannel.domain.finance.FsaService;
import com.bank.tradechannel.domain.txn.Customer;
import com.bank.tradechannel.domain.txn.EventStatus;
import com.bank.tradechannel.domain.txn.EventType;
import com.bank.tradechannel.domain.txn.OutboxEntry.OutboxStatus;
import com.bank.tradechannel.domain.txn.TxnStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The CREATE form of a Finance Standalone: a valid request becomes a transaction with its CREATE
 * event and a TFFINNEW waiting in the outbox (ground rule 7); nothing goes to TI from the page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FinanceControllerTest {
  static final Customer CUSTOMER = new Customer("0009990001", "POC TEST CUSTOMER", "001");
  @Autowired MockMvc mvc;
  @Autowired FsaService service;

  static MockHttpServletRequestBuilder validPost(String... overrides) {
    var fields = new java.util.LinkedHashMap<String, String>();
    fields.put("theirReference", "FIN-REQ-2026-9");
    fields.put("currency", "USD");
    fields.put("financeAmount", "102.00");
    fields.put("transactionAmount", "");
    fields.put("startDate", "2026-10-26");
    fields.put("tenorDays", "60");
    fields.put("productType", "PSF");
    fields.put("interestFrom", "AFTER");
    fields.put("goodsDescription", "Description of Goods");
    fields.put("messageToBank", "");
    fields.put("additionalInformation", "");
    for (int i = 0; i < overrides.length; i += 2) {
      fields.put(overrides[i], overrides[i + 1]);
    }
    var request = post("/finance").with(user("demo")).with(csrf());
    fields.forEach(request::param);
    return request;
  }

  @Test
  void needsSignIn() throws Exception {
    mvc.perform(get("/finance/new")).andExpect(redirectedUrlPattern("**/login"));
  }

  @Test
  void showsTheFormWithTheConfiguredCustomerAndDefaults() throws Exception {
    mvc.perform(get("/finance/new").with(user("demo")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("POC TEST CUSTOMER")))
        .andExpect(content().string(containsString("value=\"60\"")))
        .andExpect(content().string(containsString("name=\"_csrf\"")))
        .andExpect(content().string(containsString("Send to the bank")));
  }

  /** Posts and returns the id of the new transaction from the redirect. */
  UUID create(MockHttpServletRequestBuilder request) throws Exception {
    String location =
        mvc.perform(request)
            .andExpect(redirectedUrlPattern("/transactions/*"))
            .andReturn()
            .getResponse()
            .getRedirectedUrl();
    return UUID.fromString(location.substring("/transactions/".length()));
  }

  @Test
  void savesAValidRequestAsATransactionWithItsCreateEventAndOutboxMessage() throws Exception {
    UUID id = create(validPost());
    var view = service.view(CUSTOMER, id).orElseThrow();
    assertThat(view.txn().status()).isEqualTo(TxnStatus.PENDING);
    assertThat(view.txn().financeAmount()).isEqualByComparingTo("102.00");
    assertThat(view.txn().maturityDate()).hasToString("2026-12-25");
    assertThat(view.events()).singleElement().satisfies(e -> {
      assertThat(e.event().type()).isEqualTo(EventType.CREATE);
      assertThat(e.event().status()).isEqualTo(EventStatus.PENDING);
      var request = e.create().orElseThrow();
      assertThat(request.transactionAmount()).isEqualTo(request.financeAmount());
      assertThat(e.delivery().orElseThrow().status()).isEqualTo(OutboxStatus.PENDING);
      assertThat(e.delivery().orElseThrow().operation()).isEqualTo("TFFINNEW");
    });
    assertThat(view.canRepay()).isFalse();
  }

  @Test
  void theCustomerCannotBeChosenByTheRequest_groundRule8() throws Exception {
    UUID id = create(validPost("customer", "OTHER", "mnemonic", "OTHER", "behalfOfBranch", "999"));
    var txn = service.view(CUSTOMER, id).orElseThrow().txn();
    assertThat(txn.customer()).isEqualTo(CUSTOMER);
  }

  @Test
  void showsErrorsBesideTheFieldsAndSavesNothing() throws Exception {
    int before = service.list(CUSTOMER).size();
    mvc.perform(
            validPost(
                "theirReference", "",
                "financeAmount", "12,5",
                "tenorDays", "0",
                "goodsDescription", "x".repeat(51)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Please correct the fields marked below.")))
        .andExpect(content().string(containsString("Enter an amount such as 1250.00.")))
        .andExpect(content().string(containsString("must be greater than or equal to 1")))
        .andExpect(content().string(containsString("must not be blank")))
        .andExpect(content().string(containsString("size must be between 0 and 50")));
    assertThat(service.list(CUSTOMER)).hasSize(before);
  }

  @Test
  void checksTheChoicesAndTheDecimalsOfTheCurrency() throws Exception {
    int before = service.list(CUSTOMER).size();
    mvc.perform(validPost("currency", "XXX"))
        .andExpect(content().string(containsString("Choose one of the listed values.")));
    mvc.perform(validPost("productType", "ZZZ"))
        .andExpect(content().string(containsString("Choose one of the listed values.")));
    mvc.perform(validPost("financeAmount", "100.555"))
        .andExpect(content().string(containsString("Too many decimals for USD.")));
    assertThat(service.list(CUSTOMER)).hasSize(before);
  }
}
