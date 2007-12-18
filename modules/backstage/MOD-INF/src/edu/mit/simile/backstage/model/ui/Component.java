package edu.mit.simile.backstage.model.ui;

import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.data.Collection;

abstract public class Component {
    final protected Context _context;
    final protected String _id;
    
    protected Component(Context context, String id) {
        _context = context;
        _id = id;
    }
    
    public String getID() {
        return _id;
    }
    
    public Context getContext() {
        return _context;
    }
    
    public Collection getCollection() {
        String collectionID = _context.getStringProperty("collectionID");
        
        return _context.getExhibit().getCollection(collectionID != null ? collectionID : "default");
    }
    
    public void configure(Scriptable config) {
        try {
            String collectionID = (String) config.get("collectionID", config);
            if (collectionID != null) {
                _context.setProperty("collectionID", collectionID);
            }
        } catch (Exception e) {
            // ignore
        }
    }
    
    public Scriptable getComponentState() {
        return null;
    }
}
