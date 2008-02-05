package edu.mit.simile.backstage.model.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Scriptable;
import org.openrdf.query.algebra.Var;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Exhibit;
import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.model.ui.facets.Facet;

abstract public class Collection {
    final protected Exhibit _exhibit;
    final protected String _id;
    final protected Set<CollectionListener> _listeners = new HashSet<CollectionListener>();
    
    final protected Set<Facet> _facets = new HashSet<Facet>();
    
    protected Collection(Exhibit exhibit, String id) {
        _exhibit = exhibit;
        _id = id;
    }

    public void configure(Scriptable config, BackChannel backChannel) {
        // nothing to do
    }
    
    public Var getAllItems(TupleQueryBuilder builder, Var defaultVar) {
        throw new NotImplementedException();
    }
    
    public Var getRestrictedItems(TupleQueryBuilder builder, Var defaultVar) {
        Var itemVar = getAllItems(builder, defaultVar);
        
        computeRestrictedItems(builder, itemVar, null);
        
        return itemVar;
    }
    
    public void addListener(CollectionListener listener) {
        _listeners.add(listener);
    }
    
    public void removeListener(CollectionListener listener) {
        _listeners.remove(listener);
    }
    
    public void addFacet(Facet facet, BackChannel backChannel) {
        _facets.add(facet);
        if (facet.hasRestrictions()) {
            updateEverything(backChannel);
        } else {
            updateOneFacet(facet, backChannel);
        }
    }
    
    public void removeFacet(Facet facet) {
        _facets.remove(facet);
    }
    
    protected void updateEverything(BackChannel backChannel) {
        for (Facet facet : _facets) {
            updateOneFacet(facet, backChannel);
        }
        fireOnItemsChanged(backChannel);
    }
    
    protected void updateOneFacet(Facet facet, BackChannel backChannel) {
        TupleQueryBuilder builder = new TupleQueryBuilder();
        Var itemVar = getAllItems(builder, null);
        
        computeRestrictedItems(builder, itemVar, facet);
        
        facet.update(builder, itemVar, backChannel);
    }
    
    protected void computeRestrictedItems(TupleQueryBuilder builder, Var itemVar, Facet exceptFacet) {
        for (Facet facet : _facets) {
            if (facet != exceptFacet) {
                facet.restrict(builder, itemVar);
            }
        }
    }
    
    protected void fireOnItemsChanged(BackChannel backChannel) {
        for (CollectionListener listener : _listeners) {
            listener.onItemsChanged(backChannel);
        }
    }
}
