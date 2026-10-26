package com.bank.tradechannel.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** T0 acceptance: a deliberate boundary violation fails the architecture tests. */
class BoundaryViolationTest {

  @Test
  void xmlInTheDomainFailsTheXmlRule() {
    JavaClasses fixture =
        new ClassFileImporter().importPackages("com.bank.tradechannel.domain.archfixture");
    assertThatThrownBy(() -> ModuleBoundariesTest.ONLY_GATEWAY_KNOWS_XML.check(fixture))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("DomainClassThatParsesXml")
        .hasMessageContaining("org.w3c.dom.Document");
  }

  @Test
  void vendorSqlOutsideTheDialectIsFound() {
    assertThat(
            VendorSqlTest.findings(
                Path.of("Repo.java"), "jdbc.sql(\"SELECT TOP 10 * FROM outbox WITH (NOLOCK)\")"))
        .hasSize(2);
    assertThat(
            VendorSqlTest.findings(
                Path.of("Repo.java"), "jdbc.sql(\"SELECT id FROM outbox WHERE seq = ?\")"))
        .isEmpty();
  }
}
