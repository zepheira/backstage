package edu.mit.simile.backstage.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.mit.simile.backstage.BackstageModule;
import edu.mit.simile.backstage.ExhibitIdentity;
import edu.mit.simile.backstage.data.UnhostedDataLink;
import edu.mit.simile.backstage.model.data.Collection;
import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.data.UnhostedDatabase;
import edu.mit.simile.backstage.model.ui.Component;

public class Exhibit {
    private static Logger _logger = Logger.getLogger(Database.class);

    private static final long serialVersionUID = -1105545561204629924L;
    
    final private BackstageModule     	_module;
    final private ExhibitIdentity     	_exhibitIdentity;
    
    private List<UnhostedDataLink>    	_dataLinks = new LinkedList<UnhostedDataLink>();
    private Database 					_database;
    
    final private Map<String, Collection> 	_collectionMap = new HashMap<String, Collection>();
    final private Map<String, Component> 	_componentMap = new HashMap<String, Component>();
    final private Map<String, Context>		_contextMap = new HashMap<String, Context>();
    
    private Context _context;
    
    public Exhibit(BackstageModule module, ExhibitIdentity exhibitIdentity) {
        _module = module;
        _exhibitIdentity = exhibitIdentity;
        _context = new Context(this);
    }
    
    public void dispose() {
        if (_database != null) {
            _logger.info("Disposing interaction session for " + _exhibitIdentity.toString());
            
            if (_dataLinks != null) { // unhosted
            	_module.releaseDatabase((UnhostedDatabase) _database);
            }
            _database = null;
        }
    }

    public Database getDatabase() {
        if (_database == null) {
        	if (_dataLinks != null) {
        		_database = _module.getDatabase(_exhibitIdentity, _dataLinks);
        	} else {
        		_database = _module.getHostedDatabase();
        	}
        }
        return _database;
    }
    
    public Context getContext() {
        return _context;
    }
    
    public Collection getCollection(String id) {
        return _collectionMap.get(id);
    }

    public Collection getDefaultCollection() {
        return _collectionMap.get("default");
    }
    
    public void setCollection(String id, Collection collection) {
        _collectionMap.put(id, collection);
    }
    
    public Component getComponent(String id) {
        return _componentMap.get(id);
    }

    public void setComponent(String id, Component component) {
        _componentMap.put(id, component);
    }
    
    public List<Component> getAllComponents() {
        return new ArrayList<Component>(_componentMap.values());
    }
    
    public void setContext(String id, Context context) {
        _contextMap.put(id, context);
    }
    
    public Context getContext(String id) {
        return _contextMap.get(id);
    }
    
    public void addDataLink(String url, String mimeType, String charset) throws MalformedURLException {
        if (_database != null) {
            throw new InternalError("Cannot add more data link after exhibit already initialized");
        } else if (_dataLinks == null) {
        	throw new InternalError("Cannot add data link to an exhibit with hosted data");
        }
        
        UnhostedDataLink dataLink = new UnhostedDataLink(new URL(url), mimeType, charset);
        
        _dataLinks.add(dataLink);
    }
    
    public void addHostedDataLink() {
        if (_database != null) {
            throw new InternalError("Cannot add more data link after exhibit already initialized");
        }
    	if (_dataLinks != null) {
    		if (_dataLinks.size() > 0) {
    			throw new InternalError("Cannot hosted data link when there is already another data link");
    		}
        	_dataLinks.clear();
        	_dataLinks = null;
    	}
    }
}
