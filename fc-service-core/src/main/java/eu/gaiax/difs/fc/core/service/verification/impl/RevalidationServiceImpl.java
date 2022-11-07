package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.service.verification.RevalidationService;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Revalidates all active SDs against the composite schema.
 */
@Slf4j
public class RevalidationServiceImpl implements RevalidationService {

  private static final String REVALIDATOR_THREAD_NAME = "revalidator";
  private static final String MANAGER_THREAD_NAME = "revalidationManager";

  /**
   * The number of worker threads to use for revalidating SDs.
   */
  @Value("${federated-catalogue.revalidation-service.worker-count:5}")
  private int workerCount;

  /**
   * The number of hashes to fetch at a time.
   */
  @Value("${federated-catalogue.revalidation-service.batch-size:100}")
  private int batchSize;

  /**
   * The time (in ms) to sleep between checks for changes.
   */
  @Value("${federated-catalogue.revalidation-service.sleeptime:1000}")
  private int managerSleepTime;

  /**
   * The total number of parallel instances of the catalogue that are running.
   */
  @Value("${federated-catalogue.instance-count:3}")
  private int instanceCount;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private SelfDescriptionStore sdStore;

  @Autowired
  private VerificationService verificationService;

  private BlockingQueue<String> taskQueue;
  private ExecutorService executorService;
  private Thread managementThread;

  /**
   * Set to true by requesters, set to false by the manager when done processing.
   */
  private int workingOnChunk = -1;
  /**
   * Set to true by requesters, set to false by the manager when the precessing is restarted.
   */
  private AtomicBoolean restart = new AtomicBoolean(false);
  /**
   * Set to true by requesters.
   */
  private AtomicBoolean shutdown = new AtomicBoolean(false);

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public void setWorkerCount(int workerCount) {
    this.workerCount = workerCount;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  private void handleTask(final String sdhash) {
    ContentAccessor content = sdStore.getSDFileByHash(sdhash);
    try {
      verificationService.verifySelfDescriptionAgainstCompositeSchema(content);
    } catch (VerificationException ex) {
      log.info("SD {} is no longer valid", sdhash);
      sdStore.changeLifeCycleStatus(sdhash, SelfDescriptionStatus.REVOKED);
    }
    final var finalTaskQueue = taskQueue;
    if (finalTaskQueue != null && finalTaskQueue.size() < 0.5 * batchSize) {
      notifyManager();
    }
  }

  private void manage() {
    log.info("Revalidation manager starting.");
    String lastHash = null;
    boolean sleepAfter = true;
    while (!shutdown.get()) {
      if (restart.get()) {
        log.info("Processing revalidation restart.");
        restart.set(false);
        workingOnChunk = -1;
        taskQueue.clear();
      }
      if (workingOnChunk < 0) {
        workingOnChunk = findChunkForWork();
      }
      if (workingOnChunk >= 0) {
        if (taskQueue.size() < 0.5 * batchSize) {
          // Fetch more hashes.
          List<String> activeSdHashes = sdStore.getActiveSdHashes(lastHash, batchSize, instanceCount, workingOnChunk);
          log.debug("Fetched {} hashes for chunk {} of {}", activeSdHashes.size(), workingOnChunk, instanceCount);
          if (activeSdHashes.isEmpty()) {
            log.info("Finished revalidating.");
            workingOnChunk = -1;
            lastHash = null;
          } else {
            taskQueue.addAll(activeSdHashes);
            lastHash = activeSdHashes.get(activeSdHashes.size() - 1);
          }
        }
      }
      if (sleepAfter && managerSleepTime > 0) {
        synchronized (managementThread) {
          try {
            managementThread.wait(managerSleepTime);
          } catch (InterruptedException ex) {
            log.warn("Revalidation manager was interrupted.");
          }
        }
      }
    }
    log.info("Revalidation manager exiting.");
  }

  /**
   * Starts the revalidation process when it is not started yet, restarts the process when it is already running. It
   * does this by resetting the times on the chunk table to 2000-01-01T00:00:00Z
   */
  @Override
  public void startValidating() {
    if (taskQueue == null) {
      setup();
    }
    log.debug("Sending Start signal to revalidation manager.");
    resetChunkTableTimes();
    restart.set(true);
    notifyManager();
  }

  @Override
  public boolean isWorking() {
    return workingOnChunk >= 0;
  }

  private void notifyManager() {
    final Thread localManagementThread = managementThread;
    if (localManagementThread == null) {
      return;
    }
    synchronized (localManagementThread) {
      managementThread.notify();
    }
  }

  /**
   * Sets up the RevalidationService so it is ready for work. This does not actually start the revalidation process yet.
   */
  @Override
  public synchronized void setup() {
    shutdown.set(false);
    if (taskQueue != null) {
      return;
    }
    checkChunkTable();
    taskQueue = new ArrayBlockingQueue<>(batchSize * 2);
    executorService = createProcessors(workerCount, taskQueue, this::handleTask, REVALIDATOR_THREAD_NAME);
    managementThread = new Thread(this::manage, MANAGER_THREAD_NAME);
    managementThread.start();
  }

  /**
   * Clean up the revalidationService. If there are running tasks they will complete, but any queued tasks will not.
   */
  @Override
  public synchronized void cleanup() {
    shutdown.set(true);
    if (managementThread == null) {
      return;
    }
    notifyManager();
    executorService.shutdown();
    try {
      if (executorService.awaitTermination(2, TimeUnit.SECONDS)) {
        return;
      }
    } catch (InterruptedException ex) {
      log.error("Interrupted while waiting for shutdown.", ex);
      Thread.currentThread().interrupt();
    }
    executorService.shutdownNow();
    taskQueue = null;
    executorService = null;
    managementThread = null;
  }

  private int findChunkForWork() {
    log.debug("Searching for chunk to work on...");
    int chunkId = -1;
    try ( Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      final String query = "update revalidatorchunks set lastcheck=now() where chunkid="
          + "(select chunkid from revalidatorchunks where lastcheck < ("
          + "select updatetime from schemafiles where type=:schematype order by updatetime desc limit 1"
          + ") order by chunkid limit 1)"
          + " returning chunkid";
      List<Integer> result = session.createNativeQuery(query)
          .setParameter("schematype", SchemaStore.SchemaType.SHAPE.ordinal())
          .getResultList();
      if (result.isEmpty()) {
        log.debug("No chunk found.");
      } else {
        chunkId = result.get(0);
        log.debug("Found chunk {}.", chunkId);
      }
      transaction.commit();
    } catch (Exception exc) {
      log.warn("Exception while searching for work.", exc);
      chunkId = -1;
    }
    return chunkId;
  }

  private void checkChunkTable() {
    log.debug("Checking chunk table...");
    try ( Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.createNativeQuery("lock table revalidatorchunks").executeUpdate();
      Object maxChunkObject = session.createNativeQuery("select max(chunkid) from revalidatorchunks").getSingleResult();
      int maxChunk = maxChunkObject == null ? -1 : (Integer) maxChunkObject;
      if (maxChunk + 1 < instanceCount) {
        int firstChunkId = maxChunk + 1;
        int lastChunkId = instanceCount - 1;
        log.debug("Adding chunks {} to {} to chunk table", firstChunkId, lastChunkId);
        session.createNativeQuery("insert into revalidatorchunks (chunkid) select generate_series(:firstchunkid, :lastchunkid)")
            .setParameter("firstchunkid", firstChunkId)
            .setParameter("lastchunkid", lastChunkId)
            .executeUpdate();
      }
      if (maxChunk >= instanceCount) {
        log.debug("Removing chunks >= {} from chunk table", instanceCount);
        session.createNativeQuery("delete from revalidatorchunks where chunkid >= :instancecount")
            .setParameter("instancecount", instanceCount)
            .executeUpdate();
      }
      transaction.commit();
    }
    log.debug("Checking chunk table done.");
  }

  private void resetChunkTableTimes() {
    log.debug("Resetting chunk table times...");
    try ( Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.createNativeQuery("lock table revalidatorchunks").executeUpdate();
      session.createNativeQuery("update revalidatorchunks set lastcheck=:lastcheck")
          .setParameter("lastcheck", Instant.parse("2000-01-01T00:00:00Z"))
          .executeUpdate();
      transaction.commit();
    }
    log.debug("Resetting chunk table times done.");
  }

  private static <T> ExecutorService createProcessors(int threadCount, BlockingQueue<T> queue, Consumer<T> consumer, String name) {
    ThreadFactory factory = new BasicThreadFactory.Builder().namingPattern(name + "-%d").build();
    ExecutorService result = Executors.newFixedThreadPool(threadCount, factory);
    for (int i = 0; i < threadCount; i++) {
      result.submit(new Processor(queue, consumer, name));
    }
    return result;
  }

  private static void shutdownProcessors(ExecutorService executorService, BlockingQueue<?> queue, long timeout, TimeUnit timeUnit) {
    if (executorService != null) {
      executorService.shutdown();
      queue.clear();
      try {
        executorService.shutdownNow();
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
          log.debug("executoreService did not terminate in time");
        }
      } catch (InterruptedException ie) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * A processor thread that blocks on queue.take, waiting for tasks to process. Stops working when it is interrupted.
   *
   * @param <T> The type of the tasks the processor processes.
   */
  private static class Processor<T> implements Runnable {

    private final BlockingQueue<T> queue;
    private final Consumer<T> consumer;
    private final String name;

    private Processor(BlockingQueue<T> queue, Consumer<T> consumer, String name) {
      if (queue == null) {
        throw new IllegalArgumentException("queue must be non-null");
      }
      if (consumer == null) {
        throw new IllegalArgumentException("handler must be non-null");
      }
      if (name == null || name.isEmpty()) {
        this.name = getClass().getName();
      } else {
        this.name = name;
      }
      this.queue = queue;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      log.debug("starting {}-Thread", name);
      while (!Thread.currentThread().isInterrupted()) {
        T event;
        try {
          event = queue.take();
          consumer.accept(event);
        } catch (InterruptedException ex) {
          log.trace("{} interrupted", name, ex);
          Thread.currentThread().interrupt();
          break;
        } catch (Exception ex) {
          log.warn("Exception while executing {}", name, ex);
        }
      }
      log.debug("exiting {}-Thread", name);
    }
  }

}
