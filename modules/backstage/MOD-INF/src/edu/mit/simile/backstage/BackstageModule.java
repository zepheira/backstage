package edu.mit.simile.backstage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.ExtendedProperties;

import edu.mit.simile.backstage.data.InMemHostedDataLink;
import edu.mit.simile.backstage.model.Exhibit;
import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.data.HostedDatabase;
import edu.mit.simile.backstage.model.data.InMemHostedDatabase;
import edu.mit.simile.butterfly.ButterflyModuleImpl;

public class BackstageModule extends ButterflyModuleImpl {
    
    static Map<URL, InMemHostedDatabase> s_linkDatabaseMap;
    static HostedDatabase						 s_hostedDatabase;
    
    @SuppressWarnings("unchecked")
    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);
        if (s_linkDatabaseMap == null) {
            s_linkDatabaseMap = new HashMap<URL, InMemHostedDatabase>();
        }
    }
    
    public Database getDatabase(ExhibitIdentity identity, InMemHostedDataLink dataLink) {
        InMemHostedDatabase db = s_linkDatabaseMap.get(dataLink.url);
        if (db == null) {
            db = new InMemHostedDatabase(identity, dataLink);
            s_linkDatabaseMap.put(dataLink.url, db);
        }
        return db;
    }
    
    public void releaseDatabase(InMemHostedDatabase database) {
        InMemHostedDataLink link = database.getDataLink();
        s_linkDatabaseMap.remove(link.url);
    }
    
    public Database getHostedDatabase() {
    	if (s_hostedDatabase == null) {
	    	ExtendedProperties properties = getProperties();
	    	String databaseString = properties.getString("backstage.hostedData.database");
	    	
	    	File database = (databaseString == null || databaseString.length() == 0) ? 
	    			new File("database") : new File(databaseString);
	    	
	    	s_hostedDatabase = new HostedDatabase(database);
    	}
    	return s_hostedDatabase;
    }
    
    /**
     * @param request
     * @param id
     * @return
     */
    public Exhibit createExhibit(HttpServletRequest request, String refererUrlSHA1) {
        ExhibitIdentity exhibitIdentity;
        
        try {
            exhibitIdentity = ExhibitIdentity.create(request, refererUrlSHA1);
        } catch (MalformedURLException e) {
            _logger.error("Failed to construct exhibit identity from request " + request.toString(), e);
            return null;
        }
        
        return new Exhibit(this, exhibitIdentity);
    }
}
