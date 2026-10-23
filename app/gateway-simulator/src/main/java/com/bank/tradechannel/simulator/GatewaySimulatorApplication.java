package com.bank.tradechannel.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A fake TI for development and CI: answers like TI on the same queues, driven by scenario files
 * (ADR 0009, task T4). Never deployed to production.
 */
@SpringBootApplication
public class GatewaySimulatorApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewaySimulatorApplication.class, args);
  }
}
