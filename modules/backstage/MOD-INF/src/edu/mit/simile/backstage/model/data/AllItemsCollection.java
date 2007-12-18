package edu.mit.simile.backstage.model.data;

import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.Exhibit;

public class AllItemsCollection extends Collection {

    public AllItemsCollection(Exhibit exhibit, String id) {
        super(exhibit, id);
    }

    @Override
    public void configure(Scriptable config) {
        super.configure(config);
        // nothing to do
    }
}
