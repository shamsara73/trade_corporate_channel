package com.bank.tradechannel.domain.txn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * A message from TI, already read by the gateway; the domain never sees XML (ground rule 5). Amounts
 * are as TI sent them: in minor units (ASSUMPTION(Q35)).
 */
public sealed interface TiInbound permits TiInbound.Reply, TiInbound.Outward, TiInbound.Unreadable {

  /** The message as received, kept for audit. */
  String raw();

  /** TI's immediate ServiceResponse to a message we sent, found by our CorrelationId. */
  record Reply(String operation, String status, String correlationId, List<String> details, String raw)
      implements TiInbound {

    public boolean succeeded() {
      return "SUCCEEDED".equals(status);
    }
  }

  /**
   * An outward gateway message (Service GATEWAY), for example TFFINACK when the back office completed
   * an event, matched by the echoed eBankMasterRef and eBankEventRef.
   */
  record Outward(
      String operation,
      String messageName,
      Kind kind,
      Optional<String> eBankMasterRef,
      Optional<String> eBankEventRef,
      Optional<String> tiMasterRef,
      Optional<String> tiEventRef,
      Optional<String> bankComment,
      Optional<String> amountMinor,
      Optional<String> amountCurrency,
      Optional<String> outstandingMinor,
      Optional<String> outstandingCurrency,
      Optional<LocalDate> maturityDate,
      Optional<String> masterStatus,
      List<Attachment> attachments,
      String raw)
      implements TiInbound {}

  /** Not a message we can read; kept so nothing is lost. */
  record Unreadable(String reason, String raw) implements TiInbound {}

  /** What an outward message reports, from the last three letters of its name. */
  enum Kind {
    ACK,
    REJ,
    CRT,
    OTHER
  }

  /** A document TI attached, decoded from base64. */
  record Attachment(
      String documentId, String description, String mimeType, byte[] content, String reference) {}
}
