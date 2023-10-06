package org.example.ultis;

import soot.*;
import soot.jimple.JimpleBody;
import soot.options.Options;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

public class Writer {
    public static void writeToFile() {
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
