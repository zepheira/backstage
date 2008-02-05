package edu.mit.simile.backstage.model.ui;

import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.data.Collection;

abstract public class Component {
    protected Context _context;
    protected String _id;
    
    protected Component(Context context, String id) {
        _context = new Context(context);
        _id = id;
    }
    
    public void dispose() {
        _context.dispose();
        _context = null;
        _id = null;
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
    
    public void configure(Scriptable config, BackChannel backChannel) {
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
