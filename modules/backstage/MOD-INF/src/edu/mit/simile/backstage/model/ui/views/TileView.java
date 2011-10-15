package edu.mit.simile.backstage.model.ui.views;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.Var;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.model.data.CacheableQuery;
import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.util.DefaultScriptableObject;
import edu.mit.simile.backstage.util.ScriptableArrayBuilder;

public class TileView extends View {
    protected static Logger _logger = LoggerFactory.getLogger("backstage.views.tile-view");

    public TileView(Context context, String id) {
        super(context, id);
    }

    @Override
    public Scriptable getComponentState() {
        _logger.debug("> getComponentState");
        TupleQueryBuilder builder = new TupleQueryBuilder();
        Var itemVar = getCollection().getRestrictedItems(builder, null);

        String key = "tile-view-rendering:" + builder.getStringSerialization();
        _logger.debug("component: " + key);
        Scriptable result = (Scriptable)
        	_context.getDatabase().cacheAndRun(key, new ViewRenderingCacheableQuery(builder, itemVar));
        
        _logger.debug("< getComponentState");
        return result;
    }
    
    protected class ViewRenderingCacheableQuery extends CacheableQuery {
    	final TupleQueryBuilder _builder;
    	final Var _itemVar;
    	
    	ViewRenderingCacheableQuery(TupleQueryBuilder builder, Var itemVar) {
    		_builder = builder;
    		_itemVar = itemVar;
    	}
    	
		@Override
		protected Object internalRun() {
            _logger.debug("> internalRun");
	        Database database = _context.getDatabase();
	        
	        DefaultScriptableObject result = new DefaultScriptableObject();
	        ScriptableArrayBuilder itemIDs = new ScriptableArrayBuilder();
	        ScriptableArrayBuilder lenses = new ScriptableArrayBuilder();
	        
	        int count = 0;
	        
	        try {
	            SailRepositoryConnection connection = (SailRepositoryConnection)
	                database.getRepository().getConnection();
	            
	            try {
	                TupleQuery query = _builder.makeTupleQuery(_itemVar, connection);
	                TupleQueryResult queryResult = query.evaluate();
	                try {
	                    while (queryResult.hasNext()) {
	                        BindingSet bindingSet = queryResult.next();
	                        Value v = bindingSet.getValue(_itemVar.getName());
	                        if (v instanceof URI) {
	                            if (count < 20) {
	                            	String itemID = database.getItemId((URI) v);
	                                itemIDs.add(itemID);
	                                lenses.add(_context.generateLens(itemID));
	                            }
	                            count++;
	                        }
	                    }
	                } finally {
	                    queryResult.close();
	                }
	            } finally {
	                connection.close();
	            }
	        } catch (Exception e) {
	            _logger.error("Error querying for restricted items", e);
	        }
	        
	        result.put("count", result, count);
	        result.put("items", result, itemIDs.toArray());
	        result.put("lenses", result, lenses.toArray());
	    
            _logger.debug("< internalRun");
	        return result;
		}
    }
}
