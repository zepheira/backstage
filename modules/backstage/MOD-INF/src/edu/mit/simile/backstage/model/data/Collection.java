package edu.mit.simile.backstage.model.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Scriptable;
import org.openrdf.query.algebra.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Exhibit;
import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.model.ui.facets.Facet;

abstract public class Collection {
    protected static Logger _logger = LoggerFactory.getLogger("backstage.collection");

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
    
    public void onFacetUpdated(Facet facet, BackChannel backChannel) {
        updateEverything(backChannel);
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
        
        try {
            facet.update(builder, itemVar, backChannel);
        } catch (ExpressionException e) {
            _logger.error("Failed to update facet", e);
        }
    }
    
    protected void computeRestrictedItems(TupleQueryBuilder builder, Var itemVar, Facet exceptFacet) {
        for (Facet facet : _facets) {
            if (facet != exceptFacet) {
                try {
                    facet.restrict(builder, itemVar);
                } catch (ExpressionException e) {
                    _logger.error("Failed to restrict facet", e);
                }
            }
        }
    }
    
    protected void fireOnItemsChanged(BackChannel backChannel) {
        for (CollectionListener listener : _listeners) {
            listener.onItemsChanged(backChannel);
        }
    }
}
