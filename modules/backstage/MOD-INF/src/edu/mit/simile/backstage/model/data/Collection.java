package edu.mit.simile.backstage.model.data;

import edu.mit.simile.backstage.model.Exhibit;

abstract public class Collection {
    final protected Exhibit _exhibit;
    final protected String _id;
    
    protected Collection(Exhibit exhibit, String id) {
        _exhibit = exhibit;
        _id = id;
    }

}
