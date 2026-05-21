package ch.ehealth.levi.gui.util;

import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A custom InputStream that blocks until the GUI provides input.
 * When the buffer is empty and a read is attempted, {@code onWaitingForInput}
 * is called on the JavaFX Application Thread so the UI can show an input prompt.
 *
 * Deliberately returns at most 1 byte per {@code read(byte[], off, len)} call
 * to prevent buffered readers (e.g. InputStreamReader / StreamDecoder) from
 * consuming past the current line and triggering the callback mid-read.
 */
public class GuiInputStream extends InputStream {

    private static final int EOF = -1;

    private final LinkedBlockingQueue<Integer> buffer = new LinkedBlockingQueue<>();
    private final Runnable onWaitingForInput;
    private volatile boolean closed = false;

    public GuiInputStream(Runnable onWaitingForInput) {
        this.onWaitingForInput = onWaitingForInput;
    }

    @Override
    public int read() throws IOException {
        if (closed) return EOF;
        Integer b = buffer.poll();          // non-blocking peek
        if (b == null) {
            // truly empty → notify the GUI and then block
            Platform.runLater(onWaitingForInput);
            try {
                b = buffer.take();          // blocks until provideInput() is called
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return EOF;
            }
        }
        return b;
    }

    /**
     * Returns at most 1 byte per call so that higher-level readers
     * (InputStreamReader, StreamDecoder) cannot look ahead past the '\n'
     * that ends the current Scanner.nextLine() call.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) return 0;
        int c = read();
        if (c == EOF) return EOF;
        b[off] = (byte) c;
        return 1;
    }

    /**
     * Feed a line of user input (the trailing newline is added automatically).
     * Safe to call from any thread.
     */
    public void provideInput(String line) {
        for (char c : (line + "\n").toCharArray()) {
            buffer.offer((int) c);
        }
    }

    @Override
    public void close() {
        closed = true;
        buffer.offer(EOF);  // unblock any thread waiting in read()
    }
}
