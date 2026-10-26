package com.bank.tradechannel.web.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.tradechannel.domain.finance.FinanceSubmission;
import com.bank.tradechannel.domain.finance.TradeGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceControllerTest {
  @Autowired MockMvc mvc;
  @MockBean TradeGateway gateway;

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

  @Test
  void sendsAValidRequestAndShowsTheReferences() throws Exception {
    when(gateway.send(any())).thenReturn(new TradeGateway.SendReceipt("QM1/TI.IN"));
    var result =
        mvc.perform(validPost())
            .andExpect(redirectedUrl("/finance/sent"))
            .andExpect(flash().attributeExists("sent"))
            .andReturn();
    var captor = ArgumentCaptor.forClass(FinanceSubmission.class);
    verify(gateway).send(captor.capture());
    FinanceSubmission s = captor.getValue();
    assertThat(s.request().financeAmount().plain()).isEqualTo("102.00");
    assertThat(s.request().transactionAmount()).isEqualTo(s.request().financeAmount());
    assertThat(s.request().maturityDate()).hasToString("2026-12-25");

    mvc.perform(get("/finance/sent").with(user("demo")).flashAttrs(result.getFlashMap()))
        .andExpect(content().string(containsString(s.references().eBankMasterRef())))
        .andExpect(content().string(containsString(s.references().theirRef())))
        .andExpect(content().string(containsString("QM1/TI.IN")))
        .andExpect(content().string(containsString("2026-12-25")));
  }

  @Test
  void theCustomerCannotBeChosenByTheRequest_groundRule8() throws Exception {
    when(gateway.send(any())).thenReturn(new TradeGateway.SendReceipt("x"));
    mvc.perform(validPost("customer", "OTHER", "mnemonic", "OTHER", "behalfOfBranch", "999"))
        .andExpect(redirectedUrl("/finance/sent"));
    var captor = ArgumentCaptor.forClass(FinanceSubmission.class);
    verify(gateway).send(captor.capture());
    assertThat(captor.getValue().customer().mnemonic()).isEqualTo("0009990001");
  }

  @Test
  void showsErrorsBesideTheFieldsAndSendsNothing() throws Exception {
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
    verify(gateway, never()).send(any());
  }

  @Test
  void checksTheChoicesAndTheDecimalsOfTheCurrency() throws Exception {
    mvc.perform(validPost("currency", "XXX"))
        .andExpect(content().string(containsString("Choose one of the listed values.")));
    mvc.perform(validPost("productType", "ZZZ"))
        .andExpect(content().string(containsString("Choose one of the listed values.")));
    mvc.perform(validPost("financeAmount", "100.555"))
        .andExpect(content().string(containsString("Too many decimals for USD.")));
    verify(gateway, never()).send(any());
  }

  @Test
  void saysSoWhenTheBankCannotBeReached() throws Exception {
    when(gateway.send(any()))
        .thenThrow(new TradeGateway.GatewayUnavailableException("down", new RuntimeException()));
    mvc.perform(validPost())
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Nothing was sent")));
  }

  @Test
  void sentPageWithoutARequestGoesBackToTheForm() throws Exception {
    mvc.perform(get("/finance/sent").with(user("demo"))).andExpect(redirectedUrl("/finance/new"));
  }
}
