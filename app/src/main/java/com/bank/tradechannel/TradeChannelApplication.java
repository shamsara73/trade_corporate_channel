package com.bank.tradechannel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The whole channel in one application (ADR 0002): web pages, background jobs and the gateway to
 * TI. The packages below keep the boundaries that would become modules if the application is split
 * later; the architecture tests enforce them.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TradeChannelApplication {

  public static void main(String[] args) {
    SpringApplication.run(TradeChannelApplication.class, args);
  }
}
