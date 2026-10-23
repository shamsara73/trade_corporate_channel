package com.bank.tradechannel.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** T0 acceptance: the home page renders, escapes model values and carries a CSRF token. */
@SpringBootTest(properties = "channel.web.environment-label=<script>alert(1)</script>")
@AutoConfigureMockMvc
class HomePageTest {

  @Autowired MockMvc mvc;

  @Test
  void rendersWithTextFromMessages() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<h1>Welcome</h1>")))
        .andExpect(content().string(containsString("Trade Corporate Channel")));
  }

  @Test
  void escapesModelValues() throws Exception {
    mvc.perform(get("/"))
        .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
        .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
  }

  @Test
  void formsCarryACsrfToken() throws Exception {
    mvc.perform(get("/")).andExpect(content().string(containsString("name=\"_csrf\"")));
  }

  @Test
  void postWithoutCsrfTokenIsRejected() throws Exception {
    mvc.perform(post("/logout")).andExpect(status().isForbidden());
  }

  @Test
  void sendsAStrictContentSecurityPolicy() throws Exception {
    mvc.perform(get("/"))
        .andExpect(
            header().string("Content-Security-Policy", SecurityConfig.CONTENT_SECURITY_POLICY))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }

  @Test
  void otherPagesNeedAuthentication() throws Exception {
    mvc.perform(get("/transactions")).andExpect(status().is4xxClientError());
  }

  @Test
  void healthIsPublic() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
