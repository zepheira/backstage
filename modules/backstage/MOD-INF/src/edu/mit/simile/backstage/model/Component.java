package edu.mit.simile.backstage.model;

import edu.mit.simile.backstage.model.data.Database;


public class Component {
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
    
    public Exhibit getExhibit() {
        return _context.getExhibit();
    }
    
    public Database getDatabase() {
        return _context.getDatabase();
    }
}
