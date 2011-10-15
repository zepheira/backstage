package edu.mit.simile.backstage.model;

import java.util.Properties;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.backstage.model.data.CacheableQuery;
import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.ui.lens.Lens;
import edu.mit.simile.backstage.model.ui.lens.LensRegistry;
import edu.mit.simile.backstage.util.DefaultScriptableObject;
import edu.mit.simile.backstage.util.SailUtilities;
import edu.mit.simile.backstage.util.Utilities;


public class Context {
    protected static Logger _logger = LoggerFactory.getLogger("backstage.context");
	
    protected Context _parent;
    protected Exhibit _exhibit;
    protected Properties _properties = new Properties();
    protected LensRegistry	_lensRegistry;
    
    public Context(Context parent) {
        _parent = parent;
        _exhibit = parent.getExhibit();
        _lensRegistry = new LensRegistry(parent != null ? parent._lensRegistry : null);
    }
    
    public Context(Exhibit exhibit) {
        _parent = null;
        _exhibit = exhibit;
        _lensRegistry = new LensRegistry(null);
    }
    
    public void dispose() {
        _parent = null;
        _exhibit = null;
        _properties.clear();
        _properties = null;
        _lensRegistry = null;
    }
    
    public Exhibit getExhibit() {
        return _exhibit;
    }
    
    public Database getDatabase() {
        return _exhibit.getDatabase();
    }
    
    public Object getProperty(String name) {
        Object o = _properties.getProperty(name);
        if (o == null && _parent != null) {
            o = _parent.getProperty(name);
        }
        return o;
    }
    
    public String getStringProperty(String name) {
        return (String) getProperty(name);
    }
    
    public void setProperty(String name, Object value) {
        _properties.put(name, value);
    }
    
    public void configure(Scriptable config, BackChannel backChannel) {
        _logger.debug("> configure");
    	String id = Utilities.getString(config, "id");
    	if (id != null) {
    		_exhibit.setContext(id, this);
    	}
    	
    	Object o = config.get("lensRegistry", config);
    	if (o != null) {
    		_lensRegistry.configure((Scriptable) o, backChannel);
            _logger.debug("configuring lens registry: " + _lensRegistry);
    	}
        _logger.debug("< configure");
    }
    
    public Scriptable generateLens(String itemID) {
        _logger.debug("> generateLens");
    	String key = "lens-rendering:" + itemID;
        _logger.debug("itemID: " + key);
        Scriptable result = (Scriptable)
        	getDatabase().cacheAndRun(key, new LensRenderingCacheableQuery(itemID));
        
        _logger.debug("< generateLens");
        return result;
    }
    
    protected class LensRenderingCacheableQuery extends CacheableQuery {
    	final String _itemID;
    	
		LensRenderingCacheableQuery(String itemID) {
			_itemID = itemID;
		}
		
		@Override
		protected Object internalRun() {
	        DefaultScriptableObject result = new DefaultScriptableObject();
	        result.put("itemID", result, _itemID);
	        
	        String typeId = "Item";
	        try {
	            Database database = getDatabase();
	            URI itemURI = database.getItemURI(_itemID);
	            
	            result.put("itemURI", result, itemURI.toString());
	            
	            SailConnection connection = database.getSail().getConnection();
	            try {
	            	Value type = SailUtilities.getObject(connection, itemURI, RDF.TYPE);
	            	if (type instanceof URI) {
	            		typeId = database.getTypeId((URI) type);
	            	}
	            } finally {
	                connection.close();
	            }
	            	
	        	Lens lens = _lensRegistry.getLens(typeId);
	        	
	            SailRepositoryConnection connection2 = (SailRepositoryConnection) database.getRepository().getConnection();
	            try {
	            	lens.render(itemURI, result, database, connection2);
	            } finally {
	                connection2.close();
	            }
	        } catch (Exception e) {
	            _logger.error("Error generating lens for " + _itemID, e);
	            result.put("error", result, e.toString());
	        }
	        
	        result.put("itemType", result, typeId);
	        
	        return result;
		}
    }
}
