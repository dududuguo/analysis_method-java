package org.example.ultis.CGC;


import org.example.ultis.Methods;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.example.ultis.Dispatch.dispatch;


// Class Hierarchy Analysis
public class CHA {
    Methods T = new Methods();

    public void Solve() {
        // init soot
        writeToFile();
        CallGraph cg = Scene.v().getCallGraph();
        System.out.println("INFO: Application edge size are "+cg.size());

        // Solve
        for (Edge edge : cg) {
            Stmt stmt = edge.srcStmt();
            if (stmt != null) {
                Resolve(stmt);
            }
        }

        // Print result
        System.out.println(T.printMethods());
    }

    void Resolve(Stmt cs) {
        if (cs.containsInvokeExpr()) {
            InvokeExpr invokeExpr = cs.getInvokeExpr();
            SootMethod method = invokeExpr.getMethod();
            String signature = method.getSubSignature();


            // Only consider methods in the exampleCHA package
            if (!method.getDeclaringClass().getName().startsWith("org.example.sourceFile.exampleCHA")) {
                return;
            }


            if (invokeExpr instanceof StaticInvokeExpr) {
                T.addMethod(method);
            } else if (invokeExpr instanceof SpecialInvokeExpr) {
                SootClass C = method.getDeclaringClass();
                SootMethod dispatchedMethod = dispatch(C, signature);
                if (dispatchedMethod != null) {
                    T.addMethod(dispatchedMethod);
                } else {
                    System.out.println("Error: SpecialInvokeExpr returned null for: " + signature);
                }
            } else if (invokeExpr instanceof VirtualInvokeExpr) {
                SootClass baseClass = method.getDeclaringClass();
                List<SootClass> subClasses = Scene.v().getActiveHierarchy().getSubclassesOfIncluding(baseClass);
                for (SootClass subclass : subClasses) {
                    SootMethod dispatchedMethod = dispatch(subclass, signature);
                    if (dispatchedMethod != null) {
                        T.addMethod(dispatchedMethod);
                    }
                }
            }
        }
    }

    void writeToFile() {
        String ClassPath = "target\\classes";
        String sootClassPath = ClassPath + ";" + Scene.v().getSootClassPath();
        Scene.v().setSootClassPath(sootClassPath);
        String className = "org.example.sourceFile.exampleCHA.TestCHA";

        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_whole_program(true);                             // Enable full program analysis mode
        Options.v().setPhaseOption("cg.spark", "on");       // Enable Spark to call graph
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);

        SootClass sootClass = Scene.v().loadClassAndSupport(className);
        sootClass.setApplicationClass();
        Scene.v().loadBasicClasses();
        Scene.v().loadNecessaryClasses();
        Scene.v().setEntryPoints(Collections.singletonList(Scene.v().getMainMethod()));
        Scene.v().setMainClass(sootClass);
        PackManager.v().runPacks();

        String filename = "src\\main\\java\\org\\example\\sourceFile\\jimpleSrc\\" + sootClass.getName();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename + ".txt"))) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (sootMethod.hasActiveBody()) {
                    writer.write("Method: " + sootMethod.getName() + "\n");
                    JimpleBody body = (JimpleBody) sootMethod.getActiveBody();
                    for (Unit u : body.getUnits()) {
                        writer.write(u.toString() + "\n");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
