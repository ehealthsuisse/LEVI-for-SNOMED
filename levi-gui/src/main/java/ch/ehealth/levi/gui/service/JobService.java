package ch.ehealth.levi.gui.service;

import ch.ehealth.levi.gui.model.JobResult;
import ch.ehealth.levi.gui.util.I18nUtil;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.ehealth.levi.core.compare.CompareManager;
import ch.ehealth.levi.core.Conf;
import ch.ehealth.levi.core.db.DbCreateConfig;
import ch.ehealth.levi.core.db.SctDatabaseCreator;

/**
 * Service for executing LEVI jobs asynchronously
 */
public class JobService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);
    
    /**
     * Creates a task for creating a SNOMED CT database from RF2 release files.
     *
     * @param dbCreateConfig configuration for the database creation
     */
    public Task<JobResult> createDatabaseTask(DbCreateConfig dbCreateConfig) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage(I18nUtil.get("job.progress.dbcreate"));

                JobResult result = new JobResult("db-create");

                try {
                    SctDatabaseCreator.create(dbCreateConfig,
                            (key, args) -> updateMessage(I18nUtil.get(key, args)));

                    result.setSuccessful(true);
                    result.setDatabaseCreated(dbCreateConfig.getDbName());
                } catch (Exception e) {
                    logger.error("Error creating SNOMED database", e);
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
     * Creates a task for running translation overview
     */
    public Task<JobResult> createOverviewTask(Conf conf) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage(I18nUtil.get("job.progress.overview"));
                
                JobResult result = new JobResult("overview");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));
                    
                    manager.runTranslationOverview(conf.getFilePathCurrent(), conf.getDestination());
                                        
                    result.setManager(manager);
                    result.setSuccessful(true);
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
                updateMessage(I18nUtil.get("job.progress.desc_add"));
                
                JobResult result = new JobResult("desc-add");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));
                    
                    manager.runDeltaDescAdditions(conf.getFilePathCurrent(), conf.getDestination());
                    
                    result.setAdditionsCount(manager.getLastAdditionsCount());
                    result.setChangesCount(manager.getLastChangesCount());
                    result.setCheckPassCount(manager.getLastCheckPassCount());
                    result.setCheckFailCount(manager.getLastCheckFailCount());
                    
                    result.setManager(manager);
                    result.setSuccessful(true);
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
                updateMessage(I18nUtil.get("job.progress.desc_inact"));
                
                JobResult result = new JobResult("desc-inact");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));

                    manager.runDeltaDescInactivations(conf.getFilePathCurrent(), conf.getDestination());
                    
                    result.setInactivationsCount(manager.getLastInactivationsCount());
                    
                    result.setManager(manager);
                    result.setSuccessful(true);
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
        return createTranslateDeltaTask(conf, null);
    }

    /**
     * Creates a task for running full delta generation, optionally reusing a
     * CompareManager that already has the current file's data loaded (e.g. from a
     * preceding not-published job). When a preloaded manager is provided the current
     * file is not read again, mirroring the not-published reuse behaviour.
     */
    public Task<JobResult> createTranslateDeltaTask(Conf conf, CompareManager preloadedManager) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage(I18nUtil.get("job.progress.delta"));
                
                JobResult result = new JobResult("translate-delta");
                
                try {
                    CompareManager manager = preloadedManager;
                    if (manager != null) {
                        manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));
                        manager.runGenerateDeltaReusingCurrent(conf.getDestination());
                    } else {
                        manager = new CompareManager(conf);
                        manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));
                        manager.runGenerateDelta(conf.getFilePathCurrent(), conf.getDestination());
                    }
                    
                    result.setAdditionsCount(manager.getLastAdditionsCount());
                    result.setChangesCount(manager.getLastChangesCount());
                    result.setReactivationsCount(manager.getLastReactivationsCount());
                    result.setInactivationsCount(manager.getLastInactivationsCount());
                    result.setCheckPassCount(manager.getLastCheckPassCount());
                    result.setCheckFailCount(manager.getLastCheckFailCount());
                    
                    result.setManager(manager);
                    result.setSuccessful(true);
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
                updateMessage(I18nUtil.get("job.progress.eszett"));
                
                JobResult result = new JobResult("eszett-check");
                
                try {
                    CompareManager manager = new CompareManager(conf);
                    manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));
                    
                    manager.runCheckEszettInExtension(conf.getDestination());
                                        
                    result.setSuccessful(true);
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
     * Creates a task for the translation rule check (e.g. French)
     */
    public Task<JobResult> createTranslationCheckTask(Conf conf, String languageCode) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage(I18nUtil.get("job.progress.translation_check"));

                JobResult result = new JobResult("translation-check");

                try {
                    // Point the French spelling checker at the configured lexicon (if any).
                    // The checker reads this property when it is constructed, which happens
                    // inside runTranslationCheck -> TranslationRuleCheckers.forLanguage.
                    String lexiconDir = conf.getLexiconDir();
                    if (lexiconDir != null && !lexiconDir.isBlank()) {
                        System.setProperty("levi.spelling.lexiconDir", lexiconDir);
                    }

                    CompareManager manager = new CompareManager(conf);
                    manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));

                    manager.runTranslationCheck(conf.getFilePathCurrent(), conf.getDestination(), languageCode);

                    result.setCheckPassCount(manager.getLastCheckPassCount());
                    result.setCheckUncertainCount(manager.getLastCheckUncertainCount());
                    result.setCheckFailCount(manager.getLastCheckFailCount());
                    result.setCheckRuleCount(manager.getLastCheckRuleCount());

                    result.setManager(manager);
                    result.setSuccessful(true);
                } catch (Exception e) {
                    logger.error("Error in translation check", e);
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
     * Creates a task for the French-vs-Swiss Snomed comparison (LexSync-SCT port).
     * The four RF2 file paths are chosen per run (via file chooser) and written
     * to the configured output directory.
     */
    public Task<JobResult> createSnomedComparisonTask(Conf conf,
            String frDescPath, String chDescPath, String frLangPath, String chLangPath) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage(I18nUtil.get("job.progress.snomed_comparison"));

                JobResult result = new JobResult("snomed-comparison");

                try {
                    CompareManager manager = new CompareManager(conf);
                    manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));

                    String destination = conf.getDestination();
                    if (destination != null && !destination.endsWith("/") && !destination.endsWith("\\")) {
                        destination += "/";
                    }

                    manager.runSnomedComparison(frDescPath, chDescPath, frLangPath, chLangPath, destination);

                    result.setCheckPassCount(manager.getLastCheckPassCount());
                    result.setCheckFailCount(manager.getLastCheckFailCount());

                    result.setManager(manager);
                    result.setSuccessful(true);
                } catch (Exception e) {
                    logger.error("Error in snomed comparison", e);
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
        return createNotPublishedTask(conf, null);
    }

    /**
     * Creates a task for finding not published translations, optionally reusing a
     * CompareManager that already has the current file's data loaded (e.g. from a
     * preceding translate-delta job). When a preloaded manager is provided, only
     * the previous file is loaded, avoiding a redundant second pass over the large XLS.
     */
    public Task<JobResult> createNotPublishedTask(Conf conf, CompareManager preloadedManager) {
        return new Task<JobResult>() {
            @Override
            protected JobResult call() throws Exception {
                long startTime = System.currentTimeMillis();
                updateMessage(I18nUtil.get("job.progress.not_published"));
                
                JobResult result = new JobResult("not-published");
                
                try {
                    if (preloadedManager != null) {
                        // Current file already loaded — only read the previous file.
                    	preloadedManager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));
                        preloadedManager.runDeltaNotPublishedTranslationsReusingCurrent(
                            conf.getFilePathPrevious(),
                            conf.getDestination()
                        );
                        result.setInactivationsCount(preloadedManager.getLastNotPublishedCount());
                        result.setManager(preloadedManager);
                    } else {
                        CompareManager manager = new CompareManager(conf);
                        manager.setProgressListener((key, args) -> updateMessage(I18nUtil.get(key, args)));
                        manager.runDeltaNotPublishedTranslations(
                            conf.getFilePathCurrent(),
                            conf.getFilePathPrevious(),
                            conf.getDestination()
                        );
                        result.setInactivationsCount(manager.getLastNotPublishedCount());
                        result.setManager(manager);
                    }
                                        
                    result.setSuccessful(true);
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