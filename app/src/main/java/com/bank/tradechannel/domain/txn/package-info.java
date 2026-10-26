/**
 * The transaction-event model shared by all products: a transaction is a TI master; each instruction
 * on it (create, repay, amend, ...) is an event with its own status; messages to TI wait in the
 * outbox and messages from TI are matched back to their event.
 */
package com.bank.tradechannel.domain.txn;
