package com.bank.tradechannel.domain.txn;

/**
 * The corporate the request is sent for. It comes from the signed-in user's corporate, never from a
 * form field (ground rule 8). Until entitlements exist (task T7) it is configured.
 *
 * @param mnemonic TI customer mnemonic, max 20
 * @param name name and address as sent in DebitParty/NameAddress
 * @param behalfOfBranch TI branch, max 8
 */
public record Customer(String mnemonic, String name, String behalfOfBranch) {}
