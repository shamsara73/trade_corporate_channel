package com.bank.tradechannel.domain.finance;

import java.util.Optional;
import java.util.UUID;

/** What the customer asked for in each FSA event, kept per event. */
public interface FsaDetailStore {
  void saveCreate(UUID eventId, FinanceRequest request);

  Optional<FinanceRequest> create(UUID eventId);

  void saveRepay(UUID eventId, RepaymentRequest request);

  Optional<RepaymentRequest> repay(UUID eventId);
}
