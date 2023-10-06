package org.example.ultis.CGC;


import org.example.ultis.Methods;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.List;

import static org.example.ultis.Dispatch.dispatch;
import static org.example.ultis.Writer.writeToFile;

// Class Hierarchy Analysis
public class CHA {

    public void Solve() {
        Methods T = new Methods();

        // init soot
        writeToFile();
        CallGraph cg = Scene.v().getCallGraph();
        System.out.println("INFO: Application edge size are " + cg.size());

        // Solve
        for (Edge edge : cg) {
            Stmt stmt = edge.srcStmt();
            if (stmt != null) {
                Methods temp = Resolve(stmt);
                // union
                if (temp != null) {
                    for (SootMethod s : temp.getMethods()) {
                        if (s != null)
                            T.addMethod(s);
                    }
                }
            }
        }

        // Print result
        System.out.println(T.printMethods());
    }

    static Methods Resolve(Stmt cs) {
        Methods T = new Methods();
        if (cs.containsInvokeExpr()) {
            InvokeExpr invokeExpr = cs.getInvokeExpr();
            SootMethod method = invokeExpr.getMethod();
            String signature = method.getSubSignature();


            // Only consider methods in the exampleCHA package
            if (!method.getDeclaringClass().getName().startsWith("org.example.sourceFile.exampleCHA")) {
                return null;
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
        return T;
    }
}
