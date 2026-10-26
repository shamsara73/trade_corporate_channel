package com.bank.tradechannel.domain.txn;

/**
 * The channel's references on a message, as the bank defines them (2026-10-23).
 *
 * @param eBankMasterRef the channel's transaction number, constant for every event of the master
 * @param eBankEventRef the channel's number for this event (create, repay, ...)
 * @param theirRef the message reference, unique for every message delivered; also the
 *     CorrelationId of the envelope
 */
public record ChannelRefs(String eBankMasterRef, String eBankEventRef, String theirRef) {}
