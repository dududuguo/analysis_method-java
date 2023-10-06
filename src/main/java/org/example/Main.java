package org.example;

import org.example.CIFI.PA_CIFI;
import org.example.ultis.CGC.Graph;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.Writer;
import java.util.Set;

import static org.example.ultis.Writer.writeToFile;

public class Main {
    public static void main(String[] args) {
        // compiler java file to be Jimple and write to file
        writeToFile();

        // call graph base on soot
        CallGraph cg = Scene.v().getCallGraph();
        Graph graph = new Graph();

        // build call graph
        SootMethod method = Scene.v().getMainMethod();
        Set<Edge> edges = graph.BuildCallGraph(method, cg);

        // write dot format to file
        String dotFormat = graph.toDotFormat(edges);
        String filename = "src\\main\\java\\org\\example\\sourceFile\\dotFiles\\input.dot";
        graph.writeDotFormat(filename, dotFormat);

        //pointer analysis
        PA_CIFI pa_cifi = new PA_CIFI(cg);
        pa_cifi.solve();
    }
}
