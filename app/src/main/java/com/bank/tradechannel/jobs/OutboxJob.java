package com.bank.tradechannel.jobs;

import com.bank.tradechannel.domain.finance.OutboxDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Delivers due outbox messages to TI every couple of seconds. */
public final class OutboxJob {
  private static final Logger LOG = LoggerFactory.getLogger(OutboxJob.class);

  private final OutboxDispatcher dispatcher;

  OutboxJob(OutboxDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Scheduled(fixedDelayString = "${channel.jobs.poll-millis:2000}")
  void dispatch() {
    try {
      dispatcher.dispatchDue();
    } catch (RuntimeException e) {
      // Logged and retried on the next run; a claimed message is re-claimed when its lease expires.
      LOG.warn("outbox dispatch failed: {}", e.toString());
    }
  }
}
