package com.bank.tradechannel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The sample messages in docs/samples/ (request, TI's reply, TI's response per product and event).
 * Tests read them from there so the documented samples and the test fixtures cannot drift apart.
 */
public final class Samples {
  public static final Path DIR = Path.of("../docs/samples");

  private Samples() {}

  /** A sample by its path below docs/samples/, for example "finance-standalone/create/...". */
  public static String read(String path) {
    try {
      return Files.readString(DIR.resolve(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
