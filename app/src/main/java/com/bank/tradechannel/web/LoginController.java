package com.bank.tradechannel.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Our own login page: Spring Security's default page does not fit the strict CSP. */
@Controller
class LoginController {

  @GetMapping("/login")
  String login() {
    return "login";
  }
}
