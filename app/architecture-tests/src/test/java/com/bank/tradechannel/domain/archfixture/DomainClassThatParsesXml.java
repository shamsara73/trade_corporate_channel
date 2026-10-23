package com.bank.tradechannel.domain.archfixture;

import org.w3c.dom.Document;

/**
 * A deliberate violation used by BoundaryViolationTest to prove the XML rule bites. Test code only.
 */
public final class DomainClassThatParsesXml {
  private DomainClassThatParsesXml() {}

  public static String rootName(Document document) {
    return document.getDocumentElement().getTagName();
  }
}
