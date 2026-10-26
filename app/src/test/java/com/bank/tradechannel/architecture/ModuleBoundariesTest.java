package com.bank.tradechannel.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The package rule of docs/architecture/README.md and ground rule 5 of AGENTS.md. The application
 * is one deployable, but its packages keep module-like boundaries so it can be split later.
 */
class ModuleBoundariesTest {
  static final String ROOT = "com.bank.tradechannel";

  static JavaClasses production;

  @BeforeAll
  static void importClasses() {
    production =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
  }

  /** Only the gateway package knows XML (ground rule 5, ADR 0005). */
  static final ArchRule ONLY_GATEWAY_KNOWS_XML =
      noClasses()
          .that()
          .resideInAPackage(ROOT + "..")
          .and()
          .resideOutsideOfPackage(ROOT + ".gateway..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("javax.xml..", "jakarta.xml..", "org.w3c.dom..", "org.xml.sax..")
          .because("only the gateway package may know XML")
          .allowEmptyShould(true);

  static final ArchRule DOMAIN_IS_FREE_OF_INFRASTRUCTURE =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              ROOT + ".persistence..",
              ROOT + ".gateway..",
              ROOT + ".web..",
              ROOT + ".jobs..",
              "org.springframework.web..",
              "org.springframework.jdbc..",
              "java.sql..",
              "javax.sql..",
              "jakarta.servlet..")
          .because("the domain knows only its own ports and the catalogue")
          .allowEmptyShould(true);

  static final ArchRule CATALOGUE_DEPENDS_ON_NOTHING =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".catalogue..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              ROOT + ".domain..",
              ROOT + ".persistence..",
              ROOT + ".gateway..",
              ROOT + ".web..",
              ROOT + ".jobs..")
          .allowEmptyShould(true);

  static final ArchRule GATEWAY_SEES_ONLY_THE_CATALOGUE =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".gateway..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              ROOT + ".domain..", ROOT + ".persistence..", ROOT + ".web..", ROOT + ".jobs..")
          .allowEmptyShould(true);

  static final ArchRule PERSISTENCE_DOES_NOT_REACH_UP =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(ROOT + ".gateway..", ROOT + ".web..", ROOT + ".jobs..")
          .allowEmptyShould(true);

  static final ArchRule WEB_AND_JOBS_ARE_INDEPENDENT =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".web..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(ROOT + ".jobs..")
          .orShould()
          .dependOnClassesThat()
          .resideInAPackage(ROOT + ".gateway..")
          .because("pages save to the outbox; only background jobs talk to TI")
          .allowEmptyShould(true);

  @Test
  void onlyGatewayKnowsXml() {
    ONLY_GATEWAY_KNOWS_XML.check(production);
  }

  @Test
  void domainIsFreeOfInfrastructure() {
    DOMAIN_IS_FREE_OF_INFRASTRUCTURE.check(production);
  }

  @Test
  void catalogueDependsOnNothing() {
    CATALOGUE_DEPENDS_ON_NOTHING.check(production);
  }

  @Test
  void gatewaySeesOnlyTheCatalogue() {
    GATEWAY_SEES_ONLY_THE_CATALOGUE.check(production);
  }

  @Test
  void persistenceDoesNotReachUp() {
    PERSISTENCE_DOES_NOT_REACH_UP.check(production);
  }

  @Test
  void webDoesNotTalkToTiDirectly() {
    WEB_AND_JOBS_ARE_INDEPENDENT.check(production);
  }
}
