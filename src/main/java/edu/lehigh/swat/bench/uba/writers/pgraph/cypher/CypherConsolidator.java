package edu.lehigh.swat.bench.uba.writers.pgraph.cypher;

import edu.lehigh.swat.bench.uba.writers.utils.AbstractWriteConsolidator;
import edu.lehigh.swat.bench.uba.writers.utils.BufferSizes;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.GZIPOutputStream;

public class CypherConsolidator extends AbstractWriteConsolidator {
    
    private byte[] PRE_FILE = "\n".getBytes(StandardCharsets.UTF_8);
    private byte[] EDGE_HEADER = "".getBytes(StandardCharsets.UTF_8);
    private byte[] EDGE_FOOTER = "".getBytes(StandardCharsets.UTF_8);
    private byte[] NODE_HEADER = "CREATE INDEX ON :e(id);\n".getBytes(StandardCharsets.UTF_8);
    private byte[] NODE_FOOTER = "".getBytes(StandardCharsets.UTF_8);

    private final Queue<String> files = new LinkedList<>();
    private final String nodeFilename, edgeFilename;
    private OutputStream nodeOutput, edgeOutput;
    private boolean firstFile = true;

    public CypherConsolidator(String nodeFilename, String edgeFilename) {
        this.nodeFilename = nodeFilename;
        this.edgeFilename = edgeFilename;
    }

    @Override
    public void addFile(String file) {
        synchronized (this.files) {
            this.files.add(file);
            this.queued.incrementAndGet();
        }
    }

    @Override
    protected String nextFile() {
        synchronized (this.files) {
            return this.files.poll();
        }
    }

    @Override
    protected OutputStream getOutput(String filename) throws IOException {
        if (filename.contains("edges.cql")) {
            if (this.edgeOutput == null) {
                if (filename.endsWith(".gz")) {
                    this.edgeOutput = new GZIPOutputStream(new FileOutputStream(this.edgeFilename),
                            BufferSizes.GZIP_BUFFER_SIZE);
                } else {
                    this.edgeOutput = new BufferedOutputStream(new FileOutputStream(this.edgeFilename),
                            BufferSizes.OUTPUT_BUFFER_SIZE);
                }
                this.edgeOutput.write(EDGE_HEADER);
                this.firstFile = true;
            }
            return this.edgeOutput;
        } else {
            if (this.nodeOutput == null) {
                if (filename.endsWith(".gz")) {
                    this.nodeOutput = new GZIPOutputStream(new FileOutputStream(this.nodeFilename),
                            BufferSizes.GZIP_BUFFER_SIZE);
                } else {
                    this.nodeOutput = new BufferedOutputStream(new FileOutputStream(this.nodeFilename),
                            BufferSizes.OUTPUT_BUFFER_SIZE);
                }
                this.nodeOutput.write(NODE_HEADER);
                this.firstFile = true;
            }
            return this.nodeOutput;
        }
    }
    
    @Override
    protected void writePreFile(OutputStream output) throws IOException {
        if (!this.firstFile) {
            output.write(PRE_FILE);
        }
        this.firstFile = false;
    }

    @Override
    protected void cleanupOutputs() throws IOException {
        try {
            if (this.nodeOutput != null) {
                try {
                    this.nodeOutput.write(NODE_FOOTER);
                    this.nodeOutput.close();
                } finally {
                    this.nodeOutput = null;
                }
            }
        } finally {
            if (this.edgeOutput != null) {
                try {
                    this.edgeOutput.write(EDGE_FOOTER);
                    this.edgeOutput.close();
                } finally {
                    this.edgeOutput = null;
                }
            }
        }
    }

}
