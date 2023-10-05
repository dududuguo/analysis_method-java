package org.example.CIFI;

import org.example.ultis.Pointer;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.JimpleBody;
import soot.jimple.NewExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/*
    1. using Soot to be Jimple
    2. soot enable Spark to call graph
    3. using call graph and edges to pointer analysis
    4. no matter order due to in-sensitive
    5.  class soot.jimple.internal.JIdentityStmt	: 	r1 := @parameter0: java.lang.String[]
        class soot.jimple.internal.JAssignStmt	    : 	$r0 = new org.example.sourceFile.test
        class soot.jimple.internal.JInvokeStmt	    : 	specialinvoke $r0.<org.example.sourceFile.test: void <init>()>()
        class soot.jimple.internal.JInvokeStmt	    : 	virtualinvoke $r0.<org.example.sourceFile.test: void foo()>()
        class soot.jimple.internal.JAssignStmt	    : 	$r0.<org.example.sourceFile.test: int aa> = 30
        class soot.jimple.internal.JAssignStmt	    : 	$r0.<org.example.sourceFile.test: int bb> = 30
        class soot.jimple.internal.JReturnVoidStmt	: 	return
        class soot.jimple.internal.JIdentityStmt	: 	r1 := @parameter0: java.lang.String[]
        class soot.jimple.internal.JAssignStmt	    : 	$r0 = new org.example.sourceFile.test
        class soot.jimple.internal.JInvokeStmt	    : 	specialinvoke $r0.<org.example.sourceFile.test: void <init>()>()
        class soot.jimple.internal.JInvokeStmt	    : 	virtualinvoke $r0.<org.example.sourceFile.test: void foo()>()
        class soot.jimple.internal.JAssignStmt	    : 	$r0.<org.example.sourceFile.test: int aa> = 30
        class soot.jimple.internal.JAssignStmt	    : 	$r0.<org.example.sourceFile.test: int bb> = 30
        class soot.jimple.internal.JReturnVoidStmt	: 	return
        class soot.jimple.internal.JIdentityStmt	: 	r0 := @this: org.example.sourceFile.test
        class soot.jimple.internal.JInvokeStmt	    : 	specialinvoke r0.<java.lang.Object: void <init>()>()
        class soot.jimple.internal.JAssignStmt	    : 	r0.<org.example.sourceFile.test: int aa> = 10
        class soot.jimple.internal.JAssignStmt	    : 	r0.<org.example.sourceFile.test: int bb> = 20
        class soot.jimple.internal.JReturnVoidStmt	: 	return
 */
public class PA_CIFI {

    // worklist ={<value, linklist>}
    private Map<Value, LinkedList<Pointer>> workList = new LinkedHashMap<>();
    // Call Graph
    private CallGraph cg;
    // Used to keep track of all edges
    private Map<Value, Set<Value>> PFG = new HashMap<>();

    // pointer analysis
    public void solve() {
        // compiler java file to be Jimple and write to file
        writeToFile();

        // CallGraph
        cg = Scene.v().getCallGraph();

        // traverse all edge found src and tgt edge
        for (Edge edge : cg) {
            SootMethod srcMethod = edge.src();
            SootMethod tgtMethod = edge.tgt();

            // ignore any forgot package or include package
            if (isNotPackage(srcMethod, tgtMethod)) {
                continue;
            }
            JimpleBody body = (JimpleBody) srcMethod.getActiveBody();
            for (Unit u : body.getUnits()) {
                if (u instanceof AssignStmt) {
                    Value l = ((AssignStmt) u).getLeftOp();
                    Value r = ((AssignStmt) u).getRightOp();
                    if (r instanceof NewExpr) {                 // new statements
                        LinkedList<Pointer> list = new LinkedList<>();
                        // UUID: unique name for pts
                        Pointer pts = new Pointer(UUID.randomUUID().toString());
                        pts.add(r);
                        list.add(pts);
                        workList.put(l, list);  // new rules
                    } else if (l instanceof InstanceFieldRef && !(r instanceof InstanceFieldRef)) {
                        AddEdge(r, l);          // Store rules
                    } else if (r instanceof InstanceFieldRef) { // x = y.f statements
                        AddEdge(r, l);          // load rules
                    } else {                    // x=y statements
                        AddEdge(l, r);
                    }
                }
            }
        }

        // fix the fix-point
        while (!workList.isEmpty()) {
            Map.Entry<Value, LinkedList<Pointer>> entry = workList.entrySet().iterator().next();
            Value n = entry.getKey();
            LinkedList<Pointer> pointers = entry.getValue();

            // calculate delta: found new info about pointer
            Set<Value> delta = computeDelta(n, pointers);

            Propagate(n, delta);

            workList.remove(n);
        }

        printResults();
    }

    /*
        The newly discovered pointer information, namely delta, is propagated to other variables.
        Done by updating the pointer flow graph (PFG) and the workList (workList).
        1. Check whether the new pointer information is empty
        2. Incorporate the new pointer information.
        3. Propagate new pointer information.
     */
    void Propagate(Value n, Set<Value> pts) {
        if (!pts.isEmpty()) {
            union(n, pts);

            Set<Value> targets = PFG.getOrDefault(n, new HashSet<>());
            for (Value s : targets) {
                // Adds the new pointer information to the pointer collection for s and adds it to the workList
                updateWorkList(s, pts);
            }
        }
    }

    void union(Value n, Set<Value> pts) {
        Set<Value> existingPts = PFG.getOrDefault(n, new HashSet<>());
        existingPts.addAll(pts);
        PFG.put(n, existingPts);
    }

    /*
         Adds the new pointer information to the pointer collection for s and adds it to the workList
     */
    void updateWorkList(Value s, Set<Value> pts) {
        // Gets the current set of Pointers to s.
        LinkedList<Pointer> existingPointers = workList.getOrDefault(s, new LinkedList<>());

        // The new pointer info pts is added to the pointer set of s.
        for (Pointer pointer : existingPointers) {
            Set<Value> pointerSet = pointer.getPointerSet();
            pointerSet.addAll(pts);
        }

        // If s is not in the workList, add it to the workList.
        if (!workList.containsKey(s)) {
            Pointer newPointer = new Pointer(UUID.randomUUID().toString());
            newPointer.getPointerSet().addAll(pts);
            existingPointers.add(newPointer);
            workList.put(s, existingPointers);
        }
    }

    /*
     * Check whether s points to t.
     * If not, add this edge, and then, if the pointer set of s is not empty,
     * add t and its pointer set to the workList.
     *
     */
    private void AddEdge(Value s, Value t) {
        // 1. 检查s是否指向t
        Set<Value> targets = PFG.getOrDefault(s, new HashSet<>());
        if (!targets.contains(t)) {
            // 2. 如果不是，添加这个边
            targets.add(t);
            PFG.put(s, targets);

            // 3. 如果s的指针集不为空，将t及其指针集添加到workList中
            LinkedList<Pointer> sPointers = workList.get(s);
            if (sPointers != null && !sPointers.isEmpty()) {
                LinkedList<Pointer> tPointers = workList.getOrDefault(t, new LinkedList<>());
                tPointers.addAll(sPointers);  // 将s的指针集添加到t的指针集中
                workList.put(t, tPointers);
            }
        }
    }

    private boolean isNotPackage(SootMethod srcMethod, SootMethod tgtMethod) {
        return !srcMethod.getDeclaringClass().getPackageName().startsWith("org.example") &&
                !tgtMethod.getDeclaringClass().getPackageName().startsWith("org.example");
    }

    /*
        Calculate the newly discovered pointer information.
        new Pointers the entries in the workList
        compared to the current set of known Pointers.
     */
    Set<Value> computeDelta(Value n, LinkedList<Pointer> pointersFromWorkList) {
        // Get the pointer set pts(n)
        Set<Value> knownPointers = PFG.getOrDefault(n, new HashSet<>());

        // Get the set of Pointers to n in the workList
        Set<Value> pointersInWorkList = new HashSet<>();
        for (Pointer pointer : pointersFromWorkList) {
            pointersInWorkList.addAll(pointer.getPointerSet());
        }

        // Calculate the difference between two sets.
        pointersInWorkList.removeAll(knownPointers);

        return pointersInWorkList;
    }

    // setup soot env and using special file
    void writeToFile() {
        String ClassPath = "target\\classes";
        String sootClassPath = ClassPath + ";" + Scene.v().getSootClassPath();
        Scene.v().setSootClassPath(sootClassPath);
        String className = "org.example.sourceFile.test";

        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_whole_program(true);                             // Enable full program analysis mode
        Options.v().setPhaseOption("cg.spark", "on");       // Enable Spark to call graph
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        // ignore some packages
        Options.v().set_exclude(Arrays.asList(
                "java.*",
                "sun.*",
                "javax.*",
                "jdk.*"
        ));

        SootClass sootClass = Scene.v().loadClassAndSupport(className);
        sootClass.setApplicationClass();
        Scene.v().loadBasicClasses();
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();

        String filename="src\\main\\java\\org\\example\\sourceFile\\jimpleSrc\\"+sootClass.getName();
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

    // print result
    public void printResults() {
        for (Map.Entry<Value, Set<Value>> entry : PFG.entrySet()) {
            Value variable = entry.getKey();
            Set<Value> pointsToSet = entry.getValue();

            System.out.println(variable + " may point to: " + pointsToSet);
        }
    }
}