package poc.channel;

/** One line in the JSON file. Optional values are empty strings, not null, so the file stays flat. */
public record Reference(
    String theirReference,
    Origin origin,
    Status status,
    String eBankMasterRef,
    String eBankEventRef,
    /** TheirRef of the last message the channel sent for this reference; unique per delivery. */
    String messageRef,
    String masterRef,
    String eventRef,
    String lastMessage,
    String updatedAt) {}
