package com.bank.tradechannel.domain.finance;

import java.time.Instant;

/** A finance request on its way to TI, with everything the message needs. */
public record FinanceSubmission(
    FinanceRequest request, Customer customer, ChannelReferences references, Instant createdAt) {}
