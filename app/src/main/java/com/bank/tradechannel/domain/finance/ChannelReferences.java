package com.bank.tradechannel.domain.finance;

/**
 * The channel's references, as the bank defines them (2026-10-23).
 *
 * @param eBankMasterRef the channel's transaction number, constant for every event of the master
 * @param eBankEventRef the channel's number for this event (issue, claim, ...)
 * @param theirRef the message reference, unique for every message delivered; also the CorrelationId
 *     of the envelope
 */
public record ChannelReferences(String eBankMasterRef, String eBankEventRef, String theirRef) {}
