package edu.mit.simile.backstage.model.ui.facets;

import java.util.HashSet;
import java.util.Set;

import org.mozilla.javascript.Scriptable;
import org.openrdf.query.algebra.Var;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.model.data.Expression;

public class ListFacet extends Facet {
    protected Expression    _expression;
    protected Set<String>   _selection = new HashSet<String>();
    protected boolean       _selectMissing;
    
    protected TupleQueryBuilder _builder;
    protected Var               _itemVar;
    
    public ListFacet(Context context, String id) {
        super(context, id);
    }

    @Override
    public void configure(Scriptable config, BackChannel backChannel) {
        super.configure(config, backChannel);
        _expression = Expression.construct((Scriptable) config.get("expression", config));
        
        _collection.addFacet(this, backChannel);
    }

    @Override
    public boolean hasRestrictions() {
        return _selection.size() > 0 || _selectMissing;
    }

    @Override
    public void restrict(TupleQueryBuilder queryBuilder, Var itemVar) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update(TupleQueryBuilder queryBuilder, Var itemVar, BackChannel backChannel) {
        _builder = queryBuilder;
        _itemVar = itemVar;
        
        backChannel.addComponentChangingState(this);
    }
}
