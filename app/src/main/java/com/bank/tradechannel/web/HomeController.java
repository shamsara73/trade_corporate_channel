package com.bank.tradechannel.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The landing page. Product pages are added per task from form definitions (ADR 0006). */
@Controller
class HomeController {
  private final WebProperties properties;

  HomeController(WebProperties properties) {
    this.properties = properties;
  }

  @GetMapping("/")
  String home(Model model) {
    model.addAttribute("environmentLabel", properties.environmentLabel());
    return "home";
  }
}
