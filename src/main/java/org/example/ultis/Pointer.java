package org.example.ultis;

import soot.Unit;
import soot.Value;

import java.util.HashSet;
import java.util.Set;

public class Pointer {
    private Set<Value> list = new HashSet<>();
    String name;

    public Pointer(String name) {
        this.name = name;
    }

    public void add(Value value) {
        list.add(value);
    }

    public void remove(Value value) {
        list.remove(value);
    }

    public Set<Value> getPointerSet() {
        return list;
    }
}
