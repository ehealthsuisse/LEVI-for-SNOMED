package ch.ehealth.levi.core.compare;

public interface ProgressListener {
    void onProgress(String messageKey, Object... args);
}
