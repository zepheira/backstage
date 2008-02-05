package edu.mit.simile.backstage.model.data;

import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Exhibit;

public class TypeBasedCollection extends Collection {

    protected TypeBasedCollection(Exhibit exhibit, String id) {
        super(exhibit, id);
    }

    @Override
    public void configure(Scriptable config, BackChannel backChannel) {
        // TODO Auto-generated method stub
        
    }

}
