package com.bank.tradechannel.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * SQL must stay portable so that Oracle remains possible (ADR 0003, AGENTS.md conventions): vendor
 * functions and hints belong in SqlServerDialect only. ArchUnit sees classes, not SQL text, so this
 * test reads the main sources and changelogs of every module.
 */
class VendorSqlTest {
  static final String ALLOWED = "SqlServerDialect.java";

  static final List<Pattern> VENDOR_SQL =
      Stream.of(
              "\\bTOP\\s*\\(?\\s*\\d", // SQL Server row limit
              "\\bNOLOCK\\b|\\bREADPAST\\b|\\bUPDLOCK\\b|\\bHOLDLOCK\\b|\\bROWLOCK\\b", // table
              // hints
              "\\b(GETDATE|GETUTCDATE|SYSDATETIME|SYSUTCDATETIME|CURRENT_TIMESTAMP|SYSDATE|SYSTIMESTAMP)\\b", // time from the database
              "\\b(ISNULL|NVL|IIF|NEWID|NEWSEQUENTIALID|SCOPE_IDENTITY)\\s*\\(",
              "@@(ROWCOUNT|IDENTITY|ERROR)",
              "\\bROWNUM\\b",
              "\\bMERGE\\s+INTO\\b")
          .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
          .toList();

  static List<String> findings(Path file, String text) {
    List<String> out = new ArrayList<>();
    for (Pattern p : VENDOR_SQL) {
      Matcher m = p.matcher(text);
      while (m.find()) {
        out.add(file + ": " + m.group());
      }
    }
    return out;
  }

  @Test
  void noVendorSqlOutsideTheDialect() throws IOException {
    Path root = Path.of("..").toAbsolutePath().normalize();
    List<String> found = new ArrayList<>();
    try (Stream<Path> files = Files.walk(root)) {
      files
          .filter(Files::isRegularFile)
          .filter(f -> f.toString().contains("/src/main/"))
          .filter(f -> !f.toString().contains("/poc/") && !f.toString().contains("/target/"))
          .filter(f -> f.toString().endsWith(".java") || f.toString().endsWith(".sql"))
          .filter(f -> !f.getFileName().toString().equals(ALLOWED))
          .forEach(f -> found.addAll(findings(root.relativize(f), read(f))));
    }
    assertThat(found).as("vendor-specific SQL outside " + ALLOWED).isEmpty();
  }

  private static String read(Path file) {
    try {
      return Files.readString(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
