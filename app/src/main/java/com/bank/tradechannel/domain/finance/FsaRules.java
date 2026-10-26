package com.bank.tradechannel.domain.finance;

import com.bank.tradechannel.domain.txn.EventStatus;
import com.bank.tradechannel.domain.txn.EventType;
import com.bank.tradechannel.domain.txn.TradeEvent;
import com.bank.tradechannel.domain.txn.TradeTxn;
import com.bank.tradechannel.domain.txn.TxnStatus;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The rules of a Finance Standalone transaction. Messages are keys of {@code messages.properties}.
 *
 * <ul>
 *   <li>FSA-1: a REPAY event may start only when the CREATE event is COMPLETED (TI booked the
 *       finance), the transaction is LIVE and something is outstanding.
 *   <li>FSA-2: only one event may be with TI at a time (ground rule 7).
 *   <li>FSA-3: the repayment is in the finance currency, its principal does not exceed what is
 *       outstanding, and a FINAL repayment repays all of it.
 *   <li>FSA-4: the value date is not before the finance start date.
 * </ul>
 */
public final class FsaRules {
  private FsaRules() {}

  /** Empty if a REPAY event may start; otherwise the reason (a message key). */
  public static Optional<String> whyNoRepay(TradeTxn txn, List<TradeEvent> events) {
    boolean createCompleted =
        events.stream()
            .anyMatch(e -> e.type() == EventType.CREATE && e.status() == EventStatus.COMPLETED);
    if (!createCompleted || txn.status() == TxnStatus.PENDING) {
      return Optional.of("fsa.repay.notYet");
    }
    if (txn.status() != TxnStatus.LIVE) {
      return Optional.of("fsa.repay.closed");
    }
    if (events.stream().anyMatch(e -> e.status().inFlight())) {
      return Optional.of("fsa.repay.inFlight");
    }
    if (txn.outstanding().map(o -> o.signum() <= 0).orElse(true)) {
      return Optional.of("fsa.repay.nothingOutstanding");
    }
    return Optional.empty();
  }

  /** Field errors (field name to message key); empty if the repayment is acceptable. */
  public static Map<String, String> checkRepayment(TradeTxn txn, RepaymentRequest r) {
    Map<String, String> errors = new LinkedHashMap<>();
    if (!r.principal().currencyCode().equals(txn.currencyCode())) {
      errors.put("principal", "fsa.repay.currency");
    }
    BigDecimal outstanding = txn.outstanding().orElse(BigDecimal.ZERO);
    if (r.principal().amount().compareTo(outstanding) > 0) {
      errors.put("principal", "fsa.repay.tooMuch");
    } else if (r.action() == RepaymentAction.FINAL
        && r.principal().amount().compareTo(outstanding) != 0) {
      errors.put("principal", "fsa.repay.finalMustClear");
    }
    if (r.valueDate().isBefore(txn.startDate())) {
      errors.put("valueDate", "fsa.repay.beforeStart");
    }
    return errors;
  }
}
