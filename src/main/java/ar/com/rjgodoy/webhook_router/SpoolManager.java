package ar.com.rjgodoy.webhook_router;

import static ar.com.rjgodoy.webhook_router.filter.Configuration.DEFAULT_QUEUE;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Manages the lifecycle of jobs within a filesystem-based spooling system.
 * <p>
 * This class provides a thread-safe API for transitioning jobs between states
 * (pending, processing, processed, failed) within a queue-centric directory
 * structure (`/spool/{queueName}/{state}/`). It uses atomic file operations
 * to ensure consistency and supports fanning out jobs via hard links for
 * efficient, parallel processing.
 * <p>
 * The manager is stateless; its methods operate directly on the filesystem,
 * making it robust against application restarts.
 */
public final class SpoolManager {

  private static final String INDEX_FILENAME = ".index";

  private final Path spoolRoot;

  /**
   * Defines the valid states of a job and their corresponding directory names.
   */
  public enum State {
    PENDING("pending"), PROCESSING("processing"), PROCESSED("processed"), FAILED("failed");

    @Getter
    private final String directoryName;

    State(String directoryName) {
      this.directoryName = directoryName;
    }

  }

  public List<String> getAllQueues() throws IOException {
    return Files.list(spoolRoot)
        .filter(Files::isDirectory)
        .map(path -> path.getFileName().toString())
        .toList();
  }

  private Stream<Path> getQueueSubdirectories(String queueName) {
    return Stream.of(State.values())
        .map(state -> getPathForState(queueName, state))
        .filter(Files::isDirectory);
  }

  /**
   * Creates a new manager for the given spool directory.
   *
   * @param spoolRoot The root directory of the spool.
   * @throws NullPointerException if spoolRoot is null.
   */
  public SpoolManager(File spoolRoot) {
    this.spoolRoot = Objects.requireNonNull(spoolRoot, "Spool root file cannot be null.").toPath();
  }

  /**
   * Atomically claims a pending job, moving it to the 'processing' state.
   * <p>
   * Transition: `pending` -> `processing`
   *
   * @param queueName The name of the queue where the job resides.
   * @param fileName The unique filename of the job to claim.
   * @throws IOException if a file system error occurs.
   * @return {@code true} if the move was successful, {@code false} if the the job file does not
   *         exist in the 'pending' state.
   */
  public boolean claim(String queueName, String fileName) throws IOException {
    Path sourceDir = getPathForState(queueName, State.PENDING);
    Path targetDir = getPathForState(queueName, State.PROCESSING);
    return moveJobAtomically(sourceDir.resolve(fileName), targetDir.resolve(fileName));
  }

  /**
   * Atomically marks a processing job as complete, moving it to the 'processed' state.
   * <p>
   * Transition: `processing` -> `processed`
   *
   * @param queueName The name of the queue where the job resides.
   * @param fileName The unique filename of the job to complete.
   * @throws IOException if a file system error occurs.
   * @return {@code true} if the move was successful, {@code false} if the the job file does not
   *         exist in the 'processing' state.
   */
  public boolean complete(String queueName, String fileName) throws IOException {
    Path sourceDir = getPathForState(queueName, State.PROCESSING);
    Path targetDir = getPathForState(queueName, State.PROCESSED);
    return moveJobAtomically(sourceDir.resolve(fileName), targetDir.resolve(fileName));
  }

  /**
   * Atomically marks a processing job as failed, moving it to the 'failed' state.
   * <p>
   * Transition: `processing` -> `failed`
   *
   * @param queueName The name of the queue where the job resides.
   * @param fileName The unique filename of the job to mark as failed.
   * @throws IOException if a file system error occurs.
   * @return {@code true} if the move was successful, {@code false} if the the job file does not
   *         exist in the 'processing' state.
   */
  public boolean fail(String queueName, String fileName) throws IOException {
    Path sourceDir = getPathForState(queueName, State.PROCESSING);
    Path targetDir = getPathForState(queueName, State.FAILED);
    return moveJobAtomically(sourceDir.resolve(fileName), targetDir.resolve(fileName));
  }

  /**
   * Creates a hard link to a job currently in the 'processing' state of a source queue and places
   * the link in the 'pending' state of a target queue. This is the primary mechanism for fanning
   * out a single job to multiple queues.
   * <p>
   * Transition: `processing` (in `sourceQueueName`) -> `pending` (in `targetQueueName`)
   *
   * @param sourceQueueName The queue where the original job is being processed.
   * @param fileName The filename of the job to fan out.
   * @param targetQueueName The destination queue for the new job instance (hard link).
   * @throws IOException if a file system error occurs.
   * @return {@code true} if the copy was successful, {@code false} if the the job file does not
   *         exist in the 'processing' state, or the job already exists in the target queue in any
   *         state.
   */
  public boolean fanOut(String sourceQueueName, String fileName, String targetQueueName)
      throws IOException {
    Path sourceFile = getPathForState(sourceQueueName, State.PROCESSING).resolve(fileName);
    Path linkTargetDir = getPathForState(targetQueueName, State.PENDING);
    Path linkPath = linkTargetDir.resolve(fileName);

    if (Files.exists(sourceFile) && getQueueSubdirectories(targetQueueName)
        .noneMatch(dir -> Files.exists(dir.resolve(fileName)))) {
      Files.createDirectories(linkTargetDir);
      Files.createLink(linkPath, sourceFile);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Enqueues a new job by creating a hard link from the sourceFile into the default queue's
   * 'pending' state.
   *
   * @param sourceFile The source File to link. Must exist and be a regular file.
   * @throws IOException if an I/O error occurs (e.g., permission issues, source not on the same
   *         filesystem for hard linking, etc.).
   * @throws FileNotFoundException if the sourceFile does not exist or is not a regular file.
   * @return {@code true} if the link was created, {@code false} if a file with the target name
   *         already exists in the default queue's pending directory.
   */
  public boolean enqueue(File sourceFile) throws IOException {
    if (!sourceFile.exists() || !sourceFile.isFile()) {
      throw new FileNotFoundException(
          "Source file not found or is not a regular file: " + sourceFile.getAbsolutePath());
    }

    String fileName = sourceFile.getName();
    Path pendingDir = getPathForState(DEFAULT_QUEUE, State.PENDING);
    Files.createDirectories(pendingDir);

    Path targetPath = pendingDir.resolve(fileName);

    if (getQueueSubdirectories(DEFAULT_QUEUE)
        .noneMatch(dir -> Files.exists(dir.resolve(fileName)))
        && moveJobAtomically(sourceFile.toPath(), targetPath)) {
      Path indexFilePath = spoolRoot.resolve(DEFAULT_QUEUE).resolve(INDEX_FILENAME);
      Files.write(indexFilePath, List.of(fileName), StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
      return true;
    } else {
      return false;
    }
  }


  /**
   * Atomically moves a file from a source path to a target path, creating the target directory if
   * it does not exist.
   *
   * @return {@code true} if the move was successful, {@code false} if the source file did not
   *         exist.
   * @throws IOException if any other I/O error occurs.
   */
  private boolean moveJobAtomically(Path source, Path target) throws IOException {
    if (!Files.exists(source)) {
      return false;
    }

    Files.createDirectories(target.getParent());
    Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    return true;
  }

  /**
   * Constructs the full path for a given queue and state.
   */
  private Path getPathForState(String queueName, State state) {
    return spoolRoot.resolve(queueName).resolve(state.getDirectoryName());
  }

  public List<String> readIndex() throws IOException {
    Path indexFilePath = spoolRoot.resolve(DEFAULT_QUEUE).resolve(INDEX_FILENAME);
    if (Files.exists(indexFilePath)) {
      return Files.readAllLines(indexFilePath);
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Discovers all jobs currently in the 'pending' state for a specific queue.
   *
   * @param queueName The name of the queue to scan.
   * @return A stream of filenames for each pending job. The stream must be closed by the caller
   *         (e.g., by using a try-with-resources block).
   * @throws IOException if a file system error occurs.
   */
  public List<File> discoverPending(String queueName) throws IOException {
    Path pendingDir = getPathForState(queueName, State.PENDING);
    if (!Files.isDirectory(pendingDir)) {
      return Collections.emptyList();
    }
    return Files.list(pendingDir).map(path -> path.toFile()).toList();
  }


  public static List<File> sort(List<File> files, List<String> index) throws IOException {
    record Pair(File file, int index) {};
    return files.stream()
        .map(f->new Pair(f, index.indexOf(f.getName())))
        .filter(pair->pair.index>=0)
        .sorted(Comparator.comparing(Pair::index))
        .map(Pair::file).toList();
  }

}