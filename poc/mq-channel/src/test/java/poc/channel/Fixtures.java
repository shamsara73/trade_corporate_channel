package poc.channel;

final class Fixtures {
  private Fixtures() {}

  /** Shape follows ASSUMPTION(POC-2): one element per mapping field. Values are invented. */
  static String outward(String name, String theirRef, String eBankMaster, String comment) {
    return """
        <?xml version="1.0"?>
        <%s xmlns="urn:poc">
          <MessageName>%s</MessageName>
          <eBankMasterRef>%s</eBankMasterRef>
          <eBankEventRef>%s-01</eBankEventRef>
          <MasterRef>IGT000123</MasterRef>
          <EventRef>ISS001</EventRef>
          <InstructingPartyRef>%s</InstructingPartyRef>
          <BankComment>%s</BankComment>
        </%s>
        """
        .formatted(name, name, eBankMaster, eBankMaster, theirRef, comment, name);
  }
}
