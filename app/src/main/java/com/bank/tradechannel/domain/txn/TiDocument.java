package com.bank.tradechannel.domain.txn;

import java.time.Instant;
import java.util.UUID;

/**
 * A document TI attached to an outward message (for example the debit/credit advice of a finance).
 * The content is kept apart and read only when downloaded.
 */
public record TiDocument(
    UUID id,
    UUID eventId,
    String documentId,
    String description,
    String mimeType,
    String documentRef,
    Instant receivedAt) {}
