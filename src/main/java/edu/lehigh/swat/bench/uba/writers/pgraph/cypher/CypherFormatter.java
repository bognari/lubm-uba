package edu.lehigh.swat.bench.uba.writers.pgraph.cypher;

import edu.lehigh.swat.bench.uba.writers.graphml.Edge;
import edu.lehigh.swat.bench.uba.writers.graphml.Node;
import edu.lehigh.swat.bench.uba.writers.graphml.PropertyGraphFormatter;

import java.io.PrintStream;
import java.util.Map.Entry;

public class CypherFormatter implements PropertyGraphFormatter {

    private boolean firstNode = true, firstEdge = true;

    @Override
    public void formatNode(Node n, PrintStream output) {
        if (this.firstNode) {
            this.firstNode = false;
        } else {
            output.println();
        }

        output.print("CREATE (:");
        output.print(n.getProperties().get("type"));
        output.print(" { id:\"");
        output.print(n.getId());
        output.print('"');
        
        for (Entry<String, String> kvp : n.getProperties().entrySet()) {
            output.print(", ");
            output.print(kvp.getKey());
            output.print(" : \"");
            output.print(kvp.getValue());
            output.print('"');
        }
        output.print(" });");
    }

    @Override
    public void formatEdge(Edge e, PrintStream output) {
        if (this.firstEdge) {
            this.firstEdge = false;
        } else {
            output.println();
        }

        output.print("MATCH (a{id:\"");
        output.print(e.getSource());
        output.print("\"}) ");
        output.print("MATCH (b{id:\"");
        output.print(e.getTarget());
        output.print("\"}) ");
        output.print("MERGE (a)-[:");
        output.print(e.getLabel());
        output.print("]->(b);");
    }

    @Override
    public void newFile() {
        this.firstNode = true;
        this.firstEdge = true;
    }

}
