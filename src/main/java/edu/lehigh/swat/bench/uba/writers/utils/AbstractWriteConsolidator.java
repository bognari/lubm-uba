package edu.lehigh.swat.bench.uba.writers.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractWriteConsolidator implements WriteConsolidator {

    protected volatile boolean finished = false, cancelled = false, started = false;
    protected final AtomicLong queued = new AtomicLong(0);
    protected final AtomicLong consolidated = new AtomicLong(0);

    @Override
    public void run() {
        try {
            this.started = true;
            while (!this.cancelled) {
                try {
                    String nextFilename = nextFile();
                    if (nextFilename == null) {
                        if (this.finished) {
                            // All files consolidated, exit the loop
                            break;
                        }

                        // Wait for more work
                        Thread.sleep(100);
                        continue;
                    }

                    // Open and copy
                    InputStream input = openFile(nextFilename);
                    OutputStream output = getOutput(nextFilename);

                    writePreFile(output);
                    byte[] buffer = new byte[BufferSizes.OUTPUT_BUFFER_SIZE];
                    int read = 0;
                    do {
                        read = input.read(buffer);
                        if (read > 0) {
                            output.write(buffer, 0, read);
                        }
                    } while (read != -1);
                    writePostFile(output);

                    input.close();
                    System.out.println(String.format("Consolidated %s", nextFilename));
                    this.consolidated.incrementAndGet();

                    File f = new File(nextFilename);
                    f.delete();
                } catch (InterruptedException e) {
                    // Ignore and continue, if we've been told to cancel
                    // we'll see this soon enough
                }
            }

            this.cleanupOutputs();
            System.out.println("Finished write consolidation");
        } catch (IOException e) {
            throw new RuntimeException("Write consolidation failed", e);
        }
    }

    protected abstract String nextFile();

    protected abstract InputStream openFile(String filename) throws IOException;

    protected abstract OutputStream getOutput(String filename) throws IOException;

    protected void writePreFile(OutputStream output) throws IOException {

    }

    protected void writePostFile(OutputStream output) throws IOException {

    }

    protected abstract void cleanupOutputs() throws IOException;

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public void finish() {
        this.finished = true;
    }
    
    @Override
    public boolean wasStarted() {
        return this.started;
    }

}
