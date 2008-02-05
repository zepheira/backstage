package edu.mit.simile.backstage.model.ui.facets;

import org.mozilla.javascript.Scriptable;
import org.openrdf.query.algebra.Var;

import edu.mit.simile.backstage.model.data.Collection;
import edu.mit.simile.backstage.model.ui.Component;
import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.TupleQueryBuilder;

abstract public class Facet extends Component {
    protected Collection _collection;
    
    protected Facet(Context context, String id) {
        super(context, id);
    }

    @Override
    public void configure(Scriptable config, BackChannel backChannel) {
        super.configure(config, backChannel);
        _collection = _context.getExhibit().getCollection(_context.getStringProperty("collectionID"));
    }
    
    @Override
    public void dispose() {
        _collection.removeFacet(this);
        _collection = null;
        
        super.dispose();
    }
    
    abstract public boolean hasRestrictions();
    
    abstract public void restrict(TupleQueryBuilder queryBuilder, Var itemVar);
    
    abstract public void update(TupleQueryBuilder queryBuilder, Var itemVar, BackChannel backChannel);
}
