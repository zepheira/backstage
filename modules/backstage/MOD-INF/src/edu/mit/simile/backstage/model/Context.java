package edu.mit.simile.backstage.model;

import java.util.Properties;

import edu.mit.simile.backstage.model.data.Database;


public class Context {
    final protected Context _parent;
    final protected Exhibit _exhibit;
    final protected Properties _properties = new Properties();
    
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
    
    public Object getProperty(String name) {
        Object o = _properties.getProperty(name);
        if (o == null && _parent != null) {
            o = _parent.getProperty(name);
        }
        return o;
    }
    
    public String getStringProperty(String name) {
        return (String) getProperty(name);
    }
    
    public void setProperty(String name, Object value) {
        _properties.put(name, value);
    }
}
