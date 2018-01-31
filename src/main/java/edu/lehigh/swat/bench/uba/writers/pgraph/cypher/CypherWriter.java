package edu.lehigh.swat.bench.uba.writers.pgraph.cypher;

import edu.lehigh.swat.bench.uba.GeneratorCallbackTarget;
import edu.lehigh.swat.bench.uba.writers.graphml.SegregatedFormattingPropertyGraphWriter;

public class CypherWriter extends SegregatedFormattingPropertyGraphWriter {

    public CypherWriter(GeneratorCallbackTarget callbackTarget) {
        super(callbackTarget, new CypherFormatter());
    }

}
