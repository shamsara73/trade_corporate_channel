package com.bank.tradechannel.web;

import com.bank.tradechannel.domain.txn.Customer;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The corporate this proof of concept acts for, under {@code channel.customer}. With entitlements
 * (task T7) it comes from the signed-in user's corporate instead; it is never read from a form
 * (ground rule 8).
 */
@ConfigurationProperties("channel.customer")
public record CustomerProperties(String mnemonic, String name, String behalfOfBranch) {

  public Customer toCustomer() {
    return new Customer(mnemonic, name, behalfOfBranch);
  }
}
