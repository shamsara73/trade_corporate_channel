package com.bank.tradechannel.domain.finance;

/** The domain's port to TI. The gateway package implements it (MQ, or a file in development). */
public interface TradeGateway {

  /**
   * Hands the request to TI's incoming queue. Returns where it went; throws {@link
   * GatewayUnavailableException} if it could not be delivered.
   */
  SendReceipt send(FinanceSubmission submission);

  /** Where the message was delivered, for the user and the log. */
  record SendReceipt(String destination) {}

  /** The message could not be handed over; nothing was sent. */
  final class GatewayUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GatewayUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
