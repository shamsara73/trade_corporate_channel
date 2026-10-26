package com.bank.tradechannel.domain.finance;

import com.bank.tradechannel.domain.txn.ChannelRefs;
import com.bank.tradechannel.domain.txn.Customer;
import java.time.Instant;

/** The CREATE event's message (TFFINNEW): the request, for whom, and its references. */
public record FinanceSubmission(
    FinanceRequest request, Customer customer, ChannelRefs references, Instant createdAt)
    implements OutboundMessage {}
