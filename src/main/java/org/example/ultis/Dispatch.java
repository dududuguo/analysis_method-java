package org.example.ultis;

import soot.SootClass;
import soot.SootMethod;

/*
    This class is used to find (dispatch) the method in the class
    Look for the method by subSignature(name), and you'll keep looking for the parent class.
    If you find it, return it, if you don't find it, return null
 */
public class Dispatch {
    public static SootMethod dispatch(SootClass c, String subSignature) {
        while (c != null) {
            // If it's a constructor
            if (subSignature.contains("<init>")) {
                for (SootMethod constructor : c.getMethods()) {
                    if (constructor.isConstructor() && constructor.getSubSignature().equals(subSignature)) {
                        return constructor;
                    }
                }
            } else if (c.declaresMethod(subSignature)) {
                System.out.println("Debug: Found method in class: " + c.getName());
                return c.getMethod(subSignature);
            }
            c = c.hasSuperclass() ? c.getSuperclass() : null;
        }
        System.out.println("Error: Method not found for subSignature: " + subSignature);
        return null;
    }
}
