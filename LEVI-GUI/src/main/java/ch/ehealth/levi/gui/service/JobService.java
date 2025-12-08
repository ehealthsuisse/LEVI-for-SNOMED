package ch.ehealth.levi.gui.service;

import ch.ehealth.levi.gui.model.JobResult;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import translation.check.CompareManager;
import translation.check.Conf;

/**
 * Service for executing LEVI jobs asynchronously
 */
public class JobService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);
    
    /**
     * Creates a task for running translation overview
     */
    public Task<JobResult> createOverviewTask(Conf conf) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage("Starting translation overview...");
                updateProgress(0, 100);
                
                JobResult result = new JobResult("overview");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    updateMessage("Reading file...");
                    updateProgress(30, 100);
                    
                    manager.runTranslationOverview(conf.getFilePathCurrent(), conf.getDestination());
                    
                    updateMessage("Creating overview...");
                    updateProgress(90, 100);
                    
                    result.setSuccessful(true);
                    updateProgress(100, 100);
                } catch (Exception e) {
                    logger.error("Error in translation overview", e);
                    result.setSuccessful(false);
                    result.setErrorMessage(e.getMessage());
                    throw e;
                } finally {
                    result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                }
                
                return result;
            }
        };
    }
    
    /**
     * Creates a task for running description additions
     */
    public Task<JobResult> createDescAdditionsTask(Conf conf) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage("Starting description additions...");
                updateProgress(0, 100);
                
                JobResult result = new JobResult("desc-add");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    updateMessage("Reading file...");
                    updateProgress(30, 100);
                    
                    manager.runDeltaDescAdditions(conf.getFilePathCurrent(), conf.getDestination());
                    
                    updateMessage("Generating additions delta...");
                    updateProgress(90, 100);
                    
                    result.setSuccessful(true);
                    updateProgress(100, 100);
                } catch (Exception e) {
                    logger.error("Error in description additions", e);
                    result.setSuccessful(false);
                    result.setErrorMessage(e.getMessage());
                    throw e;
                } finally {
                    result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                }
                
                return result;
            }
        };
    }
    
    /**
     * Creates a task for running description inactivations
     */
    public Task<JobResult> createDescInactivationsTask(Conf conf) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage("Starting description inactivations...");
                updateProgress(0, 100);
                
                JobResult result = new JobResult("desc-inact");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    updateMessage("Reading file...");
                    updateProgress(30, 100);
                    
                    manager.runDeltaDescInactivations(conf.getFilePathCurrent(), conf.getDestination());
                    
                    updateMessage("Generating inactivations delta...");
                    updateProgress(90, 100);
                    
                    result.setSuccessful(true);
                    updateProgress(100, 100);
                } catch (Exception e) {
                    logger.error("Error in description inactivations", e);
                    result.setSuccessful(false);
                    result.setErrorMessage(e.getMessage());
                    throw e;
                } finally {
                    result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                }
                
                return result;
            }
        };
    }
    
    /**
     * Creates a task for running full delta generation
     */
    public Task<JobResult> createTranslateDeltaTask(Conf conf) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage("Starting full delta generation...");
                updateProgress(0, 100);
                
                JobResult result = new JobResult("translate-delta");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    updateMessage("Reading file...");
                    updateProgress(20, 100);
                    
                    manager.runGenerateDelta(conf.getFilePathCurrent(), conf.getDestination());
                    
                    updateMessage("Generating complete delta...");
                    updateProgress(90, 100);
                    
                    result.setSuccessful(true);
                    updateProgress(100, 100);
                } catch (Exception e) {
                    logger.error("Error in translate delta", e);
                    result.setSuccessful(false);
                    result.setErrorMessage(e.getMessage());
                    throw e;
                } finally {
                    result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                }
                
                return result;
            }
        };
    }
    
    /**
     * Creates a task for running eszett check
     */
    public Task<JobResult> createEszettCheckTask(Conf conf) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage("Starting eszett check...");
                updateProgress(0, 100);
                
                JobResult result = new JobResult("eszett-check");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    updateMessage("Checking eszett in extension...");
                    updateProgress(50, 100);
                    
                    manager.runCheckEszettInExtension(conf.getDestination());
                    
                    updateMessage("Generating results...");
                    updateProgress(90, 100);
                    
                    result.setSuccessful(true);
                    updateProgress(100, 100);
                } catch (Exception e) {
                    logger.error("Error in eszett check", e);
                    result.setSuccessful(false);
                    result.setErrorMessage(e.getMessage());
                    throw e;
                } finally {
                    result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                }
                
                return result;
            }
        };
    }
    
    /**
     * Creates a task for finding not published translations
     */
    public Task<JobResult> createNotPublishedTask(Conf conf) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage("Finding not published translations...");
                updateProgress(0, 100);
                
                JobResult result = new JobResult("not-published");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    updateMessage("Reading current file...");
                    updateProgress(30, 100);
                    
                    updateMessage("Reading previous file...");
                    updateProgress(50, 100);
                    
                    manager.runDeltaNotPublishedTranslations(
                        conf.getFilePathCurrent(), 
                        conf.getFilePathPrevious(), 
                        conf.getDestination()
                    );
                    
                    updateMessage("Finding unpublished translations...");
                    updateProgress(90, 100);
                    
                    result.setSuccessful(true);
                    updateProgress(100, 100);
                } catch (Exception e) {
                    logger.error("Error finding not published translations", e);
                    result.setSuccessful(false);
                    result.setErrorMessage(e.getMessage());
                    throw e;
                } finally {
                    result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                }
                
                return result;
            }
        };
    }
}
