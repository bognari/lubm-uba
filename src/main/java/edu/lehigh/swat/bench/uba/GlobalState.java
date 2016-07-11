package edu.lehigh.swat.bench.uba;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import edu.lehigh.swat.bench.uba.model.Ontology;
import edu.lehigh.swat.bench.uba.writers.ConsolidationMode;
import edu.lehigh.swat.bench.uba.writers.WriterType;
import edu.lehigh.swat.bench.uba.writers.graphml.GraphMLConsolidator;
import edu.lehigh.swat.bench.uba.writers.graphml.GraphMLNodesThenEdgesConsolidator;
import edu.lehigh.swat.bench.uba.writers.utils.WriteConsolidator;
import edu.lehigh.swat.bench.uba.writers.utils.WriterPool;

public class GlobalState {

    private final int numUniversities;

    /** user specified seed for the data generation */
    private final long baseSeed;

    /** starting index of the universities */
    private final int startIndex;

    /** univ-bench ontology url */
    private final String ontology;

    private final AtomicLong[] totalInstancesGenerated;
    private final AtomicLong[] totalPropertiesGenerated;

    private final WriterType writerType;
    private final File outputDir;
    private final boolean compress, quiet;
    private final ConsolidationMode consolidate;

    private final int threads;
    private final ExecutorService executorService;
    private final long executionTimeout;
    private final TimeUnit executionTimeoutUnit;
    private final AtomicLong errorCount = new AtomicLong(0);

    private final WriterPool writerPool;
    private final WriteConsolidator writeConsolidator;
    private final ExecutorService consolidatorService = Executors.newSingleThreadExecutor();

    public GlobalState(int univNum, long baseSeed, int startIndex, String ontologyUrl, WriterType type, File outputDir,
            ConsolidationMode consolidate, boolean compress, int threads, long executionTimeout,
            TimeUnit executionTimeoutUnit, boolean quiet) {
        this.numUniversities = univNum;
        this.baseSeed = baseSeed;
        this.startIndex = startIndex;
        this.ontology = ontologyUrl;
        this.writerType = type;
        this.outputDir = outputDir;
        this.consolidate = consolidate;
        this.compress = compress;
        this.quiet = quiet;
        this.executionTimeout = executionTimeout;
        this.executionTimeoutUnit = executionTimeoutUnit;

        this.totalInstancesGenerated = new AtomicLong[Ontology.CLASS_NUM];
        for (int i = 0; i < Ontology.CLASS_NUM; i++) {
            this.totalInstancesGenerated[i] = new AtomicLong(0l);
        }
        this.totalPropertiesGenerated = new AtomicLong[Ontology.PROP_NUM];
        for (int i = 0; i < Ontology.PROP_NUM; i++) {
            this.totalPropertiesGenerated[i] = new AtomicLong(0l);
        }

        if (threads <= 1) {
            this.executorService = Executors.newSingleThreadExecutor();
            this.threads = 1;
        } else {
            this.threads = threads;
            this.executorService = Executors.newFixedThreadPool(threads);
        }

        if (this.consolidationMode() == ConsolidationMode.Full) {
            // Set up background writer service appropriately
            this.writerPool = new WriterPool(this);
        } else {
            this.writerPool = null;
        }

        StringBuilder consolidatedFileName = new StringBuilder();
        consolidatedFileName.append(this.outputDir.getAbsolutePath());
        if (consolidatedFileName.charAt(consolidatedFileName.length() - 1) != File.separatorChar)
            consolidatedFileName.append(File.separatorChar);
        consolidatedFileName.append("Universities");
        consolidatedFileName.append(this.getFileExtension());
        if (this.compress) {
            consolidatedFileName.append(".gz");
        }

        switch (this.writerType) {
        case GRAPHML:
            this.writeConsolidator = new GraphMLConsolidator(consolidatedFileName.toString());
            break;
        case GRAPHML_NODESFIRST:
        case NEO4J_GRAPHML:
            this.writeConsolidator = new GraphMLNodesThenEdgesConsolidator(consolidatedFileName.toString());
            break;
        default:
            this.writeConsolidator = null;
        }
    }

    public long getBaseSeed() {
        return this.baseSeed;
    }

    public String getOntologyUrl() {
        return this.ontology;
    }

    public int getNumberUniversities() {
        return this.numUniversities;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public WriterType getWriterType() {
        return this.writerType;
    }

    public File getOutputDirectory() {
        return this.outputDir;
    }

    public boolean compressFiles() {
        return this.compress;
    }

    public ConsolidationMode consolidationMode() {
        return this.consolidate;
    }

    public boolean isQuietMode() {
        return this.quiet;
    }

    public int getThreads() {
        return this.threads;
    }

    public ExecutorService getExecutor() {
        return this.executorService;
    }

    public long getExecutionTimeout() {
        return this.executionTimeout;
    }

    public TimeUnit getExecutionTimeoutUnit() {
        return this.executionTimeoutUnit;
    }

    public void incrementTotalInstances(int classType) {
        this.totalInstancesGenerated[classType].incrementAndGet();
    }

    public void incrementTotalProperties(int propType) {
        this.totalPropertiesGenerated[propType].incrementAndGet();
    }

    public long getTotalInstances(int classType) {
        return this.totalInstancesGenerated[classType].get();
    }

    public long getTotalProperties(int propType) {
        return this.totalPropertiesGenerated[propType].get();
    }

    public void incrementErrorCount() {
        this.errorCount.incrementAndGet();
    }

    public boolean shouldContinue() {
        return this.errorCount.get() == 0;
    }

    /**
     * Gets the file extension for the configured writer type
     * 
     * @return File extension
     */
    public String getFileExtension() {
        // Extension
        switch (this.writerType) {
        case OWL:
            return ".owl";
        case DAML:
            return ".daml";
        case NTRIPLES:
            return ".nt";
        case TURTLE:
            return ".ttl";
        case GRAPHML:
        case GRAPHML_NODESFIRST:
        case NEO4J_GRAPHML:
            return ".graphml";
        default:
            throw new RuntimeException("Unknown writer type");
        }
    }

    public WriterPool getWriterPool() {
        return this.writerPool;
    }

    public WriteConsolidator getWriteConsolidator() {
        return this.writeConsolidator;
    }

    public void start() {

        if (this.writeConsolidator != null) {
            this.consolidatorService.submit(this.writeConsolidator);
            while (!this.writeConsolidator.wasStarted()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore and continue waiting
                }
            }
        }
    }

    public void finish() {
        if (this.consolidationMode() == ConsolidationMode.Full) {
            // Close the writer pool
            this.writerPool.close();
        }

        // Wait for write consolidation
        if (this.writeConsolidator != null) {
            this.writeConsolidator.finish();

            this.consolidatorService.shutdown();
            try {
                this.consolidatorService.awaitTermination(executionTimeout, executionTimeoutUnit);
            } catch (InterruptedException e) {
                throw new RuntimeException("Write consolidation failed to terminate", e);
            }
        }
    }
}
