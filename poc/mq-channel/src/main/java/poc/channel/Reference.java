package poc.channel;

/** One line in the JSON file. Optional values are empty strings, not null, so the file stays flat. */
public record Reference(
    String theirReference,
    Origin origin,
    Status status,
    String eBankMasterRef,
    String eBankEventRef,
    String masterRef,
    String eventRef,
    String lastMessage,
    String updatedAt) {}
