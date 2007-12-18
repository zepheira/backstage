package edu.mit.simile.backstage.model.data;

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Scriptable;
import org.openrdf.query.algebra.Var;

import edu.mit.simile.backstage.model.Exhibit;
import edu.mit.simile.backstage.model.TupleQueryBuilder;

abstract public class Collection {
    final protected Exhibit _exhibit;
    final protected String _id;
    
    protected Collection(Exhibit exhibit, String id) {
        _exhibit = exhibit;
        _id = id;
    }

    public void configure(Scriptable config) {
        // nothing to do
    }
    
    public Var getAllItems(TupleQueryBuilder builder, Var defaultVar) {
        throw new NotImplementedException();
    }
    
    public Var getRestrictedItems(TupleQueryBuilder builder, Var defaultVar) {
        throw new NotImplementedException();
    }
}
