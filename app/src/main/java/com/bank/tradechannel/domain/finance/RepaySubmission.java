package com.bank.tradechannel.domain.finance;

import com.bank.tradechannel.domain.txn.ChannelRefs;
import com.bank.tradechannel.domain.txn.Customer;
import java.time.Instant;

/**
 * A REPAY event's message (TFFSARPY).
 *
 * @param tiMasterRef TI's master reference of the finance, known since the CREATE event completed
 */
public record RepaySubmission(
    RepaymentRequest request,
    Customer customer,
    String tiMasterRef,
    ChannelRefs references,
    Instant createdAt)
    implements OutboundMessage {}
