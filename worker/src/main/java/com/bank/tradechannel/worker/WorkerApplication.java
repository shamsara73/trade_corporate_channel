package com.bank.tradechannel.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Background work: the outbox dispatcher, TI's replies and outward messages, and status polling
 * (ADR 0002, 0003, 0008). Only a health endpoint is served over HTTP.
 */
@SpringBootApplication
public class WorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(WorkerApplication.class, args);
  }
}
