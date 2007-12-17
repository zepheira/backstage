package edu.mit.simile.backstage.model;

import edu.mit.simile.backstage.model.data.Database;


public class Context {
    final protected Context _parent;
    final protected Exhibit _exhibit;
    
    public Context(Context parent) {
        _parent = parent;
        _exhibit = parent.getExhibit();
    }
    
    public Context(Exhibit exhibit) {
        _parent = null;
        _exhibit = exhibit;
    }
    
    public Exhibit getExhibit() {
        return _exhibit;
    }
    
    public Database getDatabase() {
        return _exhibit.getDatabase();
    }
}
