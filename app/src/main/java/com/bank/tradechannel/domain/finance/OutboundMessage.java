package com.bank.tradechannel.domain.finance;

import com.bank.tradechannel.domain.txn.ChannelRefs;

/** A message to TI, one type per event; the gateway turns it into TI's XML. */
public sealed interface OutboundMessage permits FinanceSubmission, RepaySubmission {
  ChannelRefs references();
}
