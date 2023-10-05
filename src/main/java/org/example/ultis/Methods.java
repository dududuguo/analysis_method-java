package org.example.ultis;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import soot.SootMethod;
import soot.jimple.Stmt;


import java.lang.reflect.Method;
import java.util.*;

@ToString
@Getter
@Setter
public class Methods {

    Set<SootMethod> methods = new HashSet<>();

    public void addMethod(SootMethod method) {
        methods.add(method);
    }

    public String printMethods() {
        StringBuilder sb = new StringBuilder();
        sb.append("Methods:\n");
        List<SootMethod> sortedMethods = new ArrayList<>(methods);
        sortedMethods.sort(Comparator.comparing(method -> method.getDeclaringClass().getName() + "." + method.getName()));

        for (SootMethod method : sortedMethods) {
            sb.append("\tClass: ").append(method.getDeclaringClass().getName()).append("\n");
            sb.append("\t\tMethod: ").append(method.getName()).append("\n");
            sb.append("\t\tSignature: ").append(method.getSignature()).append("\n");
            sb.append("\t\tReturn Type: ").append(method.getReturnType()).append("\n\n");
        }
        return sb.toString();
    }
}
