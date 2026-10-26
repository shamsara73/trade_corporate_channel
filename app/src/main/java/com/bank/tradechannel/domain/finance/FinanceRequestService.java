package com.bank.tradechannel.domain.finance;

import java.time.Clock;

/**
 * Sends a finance request to TI.
 *
 * <p>Proof-of-concept shortcut: the request goes straight to the gateway when the user presses
 * Send. Ground rule 7 (outbox, approval, enquiry before re-send) arrives with tasks T5 to T9 and
 * replaces this direct call.
 */
public final class FinanceRequestService {
  private final TradeGateway gateway;
  private final ReferenceGenerator references;
  private final Clock clock;

  public FinanceRequestService(TradeGateway gateway, ReferenceGenerator references, Clock clock) {
    this.gateway = gateway;
    this.references = references;
    this.clock = clock;
  }

  public Sent send(FinanceRequest request, Customer customer) {
    var submission =
        new FinanceSubmission(request, customer, references.newTransaction(), clock.instant());
    return new Sent(submission, gateway.send(submission));
  }

  /** What was sent and where. */
  public record Sent(FinanceSubmission submission, TradeGateway.SendReceipt receipt) {}
}
