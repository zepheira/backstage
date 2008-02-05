package edu.mit.simile.backstage.model;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.ui.Component;
import edu.mit.simile.backstage.util.ScriptableArrayBuilder;

public class BackChannel {
    protected List<Component> _componentsChangingState = new ArrayList<Component>();
    protected ScriptableArrayBuilder _componentUpdates = new ScriptableArrayBuilder();
    
    public boolean hasComponentsChangingState() {
        return _componentsChangingState.size() > 0;
    }
    
    public boolean hasComponentUpdates() {
        return _componentUpdates.size() > 0;
    }
    
    public NativeArray getComponentStateArray() {
        ScriptableArrayBuilder states = new ScriptableArrayBuilder();
        for (Component c : _componentsChangingState) {
            Scriptable state = c.getComponentState();
            if (state != null) {
                state.put("id", state, c.getID());
                states.add(state);
            }
        }
        return states.toArray();
    }
    
    public NativeArray getComponentUpdateArray() {
        return _componentUpdates.toArray();
    }
    
    public void addComponentChangingState(Component c) {
        _componentsChangingState.remove(c); // remove any previous occurence
        _componentsChangingState.add(c);
    }
    
    public void addComponentUpdate(Component c, Object o) {
        _componentUpdates.add(o);
    }
}
