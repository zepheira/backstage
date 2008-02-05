package edu.mit.simile.backstage.model.ui.views;

import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.data.Collection;
import edu.mit.simile.backstage.model.data.CollectionListener;
import edu.mit.simile.backstage.model.ui.Component;

abstract public class View extends Component {
    protected Collection _collection;
    protected CollectionListener _listener;

    public View(Context context, String id) {
        super(context, id);
    }

    @Override
    public void configure(Scriptable config, BackChannel backChannel) {
        super.configure(config, backChannel);
        
        _collection = _context.getExhibit().getCollection(_context.getStringProperty("collectionID"));
        _listener = new CollectionListener() {
            public void onItemsChanged(BackChannel backChannel) {
                backChannel.addComponentChangingState(View.this);
            }
        };
        _collection.addListener(_listener);
        
        backChannel.addComponentChangingState(this);
    }
    
    @Override
    public void dispose() {
        _collection.removeListener(_listener);
        _listener = null;
        _collection = null;
        
        super.dispose();
    }
}
