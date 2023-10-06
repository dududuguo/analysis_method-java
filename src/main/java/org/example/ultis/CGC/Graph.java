package org.example.ultis.CGC;

import org.example.ultis.Methods;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static org.example.ultis.CGC.CHA.Resolve;

public class Graph {

    public Set<Edge> BuildCallGraph(SootMethod method, CallGraph cg) {
        System.out.println("Debug: Graph.BuildCallGraph() called");

        // containing the methods to be processed
        LinkedList<SootMethod> workList = new LinkedList<>();
        workList.add(method);

        // call graph
        Set<Edge> CG = new HashSet<>();
        // reachable methods
        Set<SootMethod> RM = new HashSet<>();

        while (!workList.isEmpty()) {
            SootMethod m = workList.poll();

            if (RM.contains(m)) {
                continue;
            }
            RM.add(m);

            Iterator<Edge> edgesOut = cg.edgesOutOf(m);
            while (edgesOut.hasNext()) {
                Edge edge = edgesOut.next();
                Stmt stmt = edge.srcStmt();
                if (stmt != null && stmt.containsInvokeExpr()) {
                    Methods T = Resolve(stmt);
                    if (T != null) {
                        for (SootMethod target : T.getMethods()) {
                            if (!RM.contains(target)) {
                                workList.add(target);
                            }
                        }
                        CG.add(edge);
                    }
                }
            }
        }
        return CG;
    }

    public String toDotFormat(Set<Edge> edges) {
        StringBuilder builder = new StringBuilder();
        builder.append("digraph Graph {\n");
        builder.append("    node [shape=box];\n");

        for (Edge edge : edges) {
            // Get the source method signature
            String src = edge.getSrc().method().getSignature();
            // Get the target method signature
            String tgt = edge.getTgt().method().getSignature();

            // Replace the special characters in the method signature
            src = src.replaceAll("[<>:\"/\\-]", "_");
            tgt = tgt.replaceAll("[<>:\"/\\-]", "_");

            builder.append("    \"").append(src).append("\" -> \"").append(tgt).append("\";\n");
        }

        builder.append("}\n");
        return builder.toString();
    }

    public void writeDotFormat(String filename, String dotFormat) {
        Writer writer;
        try {
            writer = new BufferedWriter(
                    new FileWriter(filename)
            );
            writer.write(dotFormat);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
