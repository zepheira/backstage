package edu.mit.simile.backstage.util;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.NativeArray;

public class ScriptableArrayBuilder {
    private List<Object> _list = new ArrayList<Object>();

    public void add(Object o) {
        _list.add(o);
    }
    
    public void remove(int i) {
        _list.remove(i);
    }
    
    public NativeArray toArray() {
        return new NativeArray(_list.toArray());
    }
    
    public int size() {
        return _list.size();
    }
}