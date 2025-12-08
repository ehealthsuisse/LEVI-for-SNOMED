package ch.ehealth.levi.gui.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class to hold job execution results
 */
public class JobResult {
    
    private String jobType;
    private int additionsCount;
    private int changesCount;
    private int inactivationsCount;
    private int reactivationsCount;
    private int errorsCount;
    private int warningsCount;
    private long executionTimeMs;
    private boolean successful;
    private String errorMessage;
    
    private List<ResultEntry> additions = new ArrayList<>();
    private List<ResultEntry> changes = new ArrayList<>();
    private List<ResultEntry> inactivations = new ArrayList<>();
    private List<ResultEntry> reactivations = new ArrayList<>();
    private List<LogEntry> errors = new ArrayList<>();
    private List<LogEntry> warnings = new ArrayList<>();
    
    public JobResult(String jobType) {
        this.jobType = jobType;
        this.successful = true;
    }
    
    // Getters and setters
    public String getJobType() {
        return jobType;
    }
    
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
    
    public int getAdditionsCount() {
        return additionsCount;
    }
    
    public void setAdditionsCount(int additionsCount) {
        this.additionsCount = additionsCount;
    }
    
    public int getChangesCount() {
        return changesCount;
    }
    
    public void setChangesCount(int changesCount) {
        this.changesCount = changesCount;
    }
    
    public int getInactivationsCount() {
        return inactivationsCount;
    }
    
    public void setInactivationsCount(int inactivationsCount) {
        this.inactivationsCount = inactivationsCount;
    }
    
    public int getReactivationsCount() {
        return reactivationsCount;
    }
    
    public void setReactivationsCount(int reactivationsCount) {
        this.reactivationsCount = reactivationsCount;
    }
    
    public int getErrorsCount() {
        return errorsCount;
    }
    
    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
    }
    
    public int getWarningsCount() {
        return warningsCount;
    }
    
    public void setWarningsCount(int warningsCount) {
        this.warningsCount = warningsCount;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<ResultEntry> getAdditions() {
        return additions;
    }
    
    public void setAdditions(List<ResultEntry> additions) {
        this.additions = additions;
    }
    
    public List<ResultEntry> getChanges() {
        return changes;
    }
    
    public void setChanges(List<ResultEntry> changes) {
        this.changes = changes;
    }
    
    public List<ResultEntry> getInactivations() {
        return inactivations;
    }
    
    public void setInactivations(List<ResultEntry> inactivations) {
        this.inactivations = inactivations;
    }
    
    public List<ResultEntry> getReactivations() {
        return reactivations;
    }
    
    public void setReactivations(List<ResultEntry> reactivations) {
        this.reactivations = reactivations;
    }
    
    public List<LogEntry> getErrors() {
        return errors;
    }
    
    public void setErrors(List<LogEntry> errors) {
        this.errors = errors;
    }
    
    public List<LogEntry> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<LogEntry> warnings) {
        this.warnings = warnings;
    }
    
    /**
     * Entry for result tables
     */
    public static class ResultEntry {
        private String conceptId;
        private String descriptionId;
        private String term;
        private String language;
        private String type;
        private String status;
        private String notes;
        
        public ResultEntry() {
        }
        
        public ResultEntry(String conceptId, String term, String language, String type) {
            this.conceptId = conceptId;
            this.term = term;
            this.language = language;
            this.type = type;
        }
        
        // Getters and setters
        public String getConceptId() {
            return conceptId;
        }
        
        public void setConceptId(String conceptId) {
            this.conceptId = conceptId;
        }
        
        public String getDescriptionId() {
            return descriptionId;
        }
        
        public void setDescriptionId(String descriptionId) {
            this.descriptionId = descriptionId;
        }
        
        public String getTerm() {
            return term;
        }
        
        public void setTerm(String term) {
            this.term = term;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getNotes() {
            return notes;
        }
        
        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
    
    /**
     * Entry for log/error tables
     */
    public static class LogEntry {
        private String severity;
        private String conceptId;
        private String message;
        
        public LogEntry(String severity, String conceptId, String message) {
            this.severity = severity;
            this.conceptId = conceptId;
            this.message = message;
        }
        
        // Getters and setters
        public String getSeverity() {
            return severity;
        }
        
        public void setSeverity(String severity) {
            this.severity = severity;
        }
        
        public String getConceptId() {
            return conceptId;
        }
        
        public void setConceptId(String conceptId) {
            this.conceptId = conceptId;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
