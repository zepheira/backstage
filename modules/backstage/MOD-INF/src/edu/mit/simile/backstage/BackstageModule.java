package edu.mit.simile.backstage;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.ExtendedProperties;

import edu.mit.simile.backstage.data.UnhostedDataLink;
import edu.mit.simile.backstage.model.Exhibit;
import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.data.HostedDatabase;
import edu.mit.simile.backstage.model.data.UnhostedDatabase;
import edu.mit.simile.butterfly.ButterflyModuleImpl;

public class BackstageModule extends ButterflyModuleImpl {
    final static private int TRACE_MAP_COUNT = 32;
    
    /**
     * We use a farm of maps to avoid severe delays in locking a single map. This is an early
     * optimization but it seems like a good harmless idea. Each trace can take quite sometime
     * to determine whether to instantiate a new exhibit or not based on whether the data
     * is deemed to have changed. Determining whether the data has changed might require some
     * HTTP HEAD requests, which take time.
     */
    static Map<ExhibitIdentity, DatabaseTrace>[] s_databaseTracesMaps;
    static HostedDatabase						 s_hostedDatabase;
    
    @SuppressWarnings("unchecked")
    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);
        if (s_databaseTracesMaps == null) {
            s_databaseTracesMaps = new Map[TRACE_MAP_COUNT];
            for (int i = 0; i < TRACE_MAP_COUNT; i++) {
                s_databaseTracesMaps[i] = new HashMap<ExhibitIdentity, DatabaseTrace>();
            }
        }
    }
    
    public Database getDatabase(ExhibitIdentity identity, List<UnhostedDataLink> dataLinks) {
        Map<ExhibitIdentity, DatabaseTrace> databaseTraces = getDatabaseTraceMap(identity);
        synchronized (databaseTraces) {
            DatabaseTrace databaseTrace = databaseTraces.get(identity);
            if (databaseTrace == null) {
                databaseTrace = new DatabaseTrace(identity);
                databaseTraces.put(identity, databaseTrace);
            }
            return databaseTrace.getDatabase(dataLinks);
        }
    }
    
    public void releaseDatabase(UnhostedDatabase database) {
        ExhibitIdentity identity = database.getIdentity();
        Map<ExhibitIdentity, DatabaseTrace> databaseTraces = getDatabaseTraceMap(identity);
        synchronized (databaseTraces) {
            DatabaseTrace databaseTrace = databaseTraces.get(identity);
            if (databaseTrace != null) {
                databaseTrace.releaseDatabase(database);
                
                if (databaseTrace.isEmpty()) {
                    databaseTraces.remove(identity);
                }
            }
        }
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
    
    static private Map<ExhibitIdentity, DatabaseTrace> getDatabaseTraceMap(ExhibitIdentity identity) {
        return s_databaseTracesMaps[Math.abs(identity.hashCode()) % TRACE_MAP_COUNT];
    }
}
