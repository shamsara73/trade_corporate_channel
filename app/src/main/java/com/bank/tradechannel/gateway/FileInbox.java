package com.bank.tradechannel.gateway;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File mode's stand-in for TI's outgoing queue: every {@code *.xml} put in the inbox directory is read
 * in name order as a message from TI and then moved to {@code processed/}. Drop TI's real messages
 * (for example a captured TFFINACK) here to replay them.
 */
public final class FileInbox {
  private static final Logger LOG = LoggerFactory.getLogger(FileInbox.class);

  private final Path directory;
  private final InboundRouter router;

  public FileInbox(Path directory, InboundRouter router) {
    this.directory = directory;
    this.router = router;
  }

  /** Reads what is waiting; returns how many files were taken. */
  public int poll() {
    if (!Files.isDirectory(directory)) {
      return 0;
    }
    List<Path> files;
    try (Stream<Path> listing = Files.list(directory)) {
      files = listing.filter(f -> f.toString().endsWith(".xml")).sorted().toList();
    } catch (IOException e) {
      LOG.warn("cannot list {}: {}", directory, e.getMessage());
      return 0;
    }
    for (Path file : files) {
      try {
        router.accept(Files.readString(file, StandardCharsets.UTF_8), "file:" + file.getFileName());
        Path done = directory.resolve("processed");
        Files.createDirectories(done);
        Files.move(file, done.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        LOG.warn("cannot read {}: {}", file, e.getMessage());
      }
    }
    return files.size();
  }
}
