package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheManager;
import com.whirlycott.cache.Cacheable;

import edu.mit.simile.butterfly.ButterflyModuleImpl;

public class BackstageModule extends ButterflyModuleImpl {
    final static private int TRACE_MAP_COUNT = 32;
    
    static Cache s_interactiveSessions;
    
    /**
     * We use a farm of maps to avoid severe delays in locking a single map. This is an early
     * optimization but it seems like a good harmless idea. Each trace can take quite sometime
     * to determine whether to instantiate a new exhibit or not based on whether the data
     * is deemed to have changed. Determining whether the data has changed might require some
     * HTTP HEAD requests, which take time.
     */
    static Map<ExhibitIdentity, ExhibitTrace>[] s_exhibitTracesMaps;
    
    @SuppressWarnings("unchecked")
    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);
        if (s_interactiveSessions == null) {
            s_interactiveSessions = CacheManager.getInstance().getCache();
        }
        if (s_exhibitTracesMaps == null) {
            s_exhibitTracesMaps = new Map[TRACE_MAP_COUNT];
            for (int i = 0; i < TRACE_MAP_COUNT; i++) {
                s_exhibitTracesMaps[i] = new HashMap<ExhibitIdentity, ExhibitTrace>();
            }
        }
    }
    
    /**
     * This method is called once whenever an exhibit page is loaded. It is called from the
     * interactive session objects once all the data links have been specified.
     * 
     * @param identity
     * @param dataLinks
     * @return
     */
    public Exhibit getExhibit(ExhibitIdentity identity, List<DataLink> dataLinks) {
        Map<ExhibitIdentity, ExhibitTrace> exhibitTraces = getExhibitTraceMap(identity);
        synchronized (exhibitTraces) {
            ExhibitTrace exhibitTrace = exhibitTraces.get(identity);
            if (exhibitTrace == null) {
                exhibitTrace = new ExhibitTrace(identity);
                exhibitTraces.put(identity, exhibitTrace);
            }
            return exhibitTrace.getExhibit(dataLinks);
        }
    }
    
    public void releaseExhibit(Exhibit exhibit) {
        ExhibitIdentity identity = exhibit.getIdentity();
        Map<ExhibitIdentity, ExhibitTrace> exhibitTraces = getExhibitTraceMap(identity);
        synchronized (exhibitTraces) {
            ExhibitTrace exhibitFamily = exhibitTraces.get(identity);
            if (exhibitFamily != null) {
                exhibitFamily.releaseExhibit(exhibit);
                
                if (exhibitFamily.isEmpty()) {
                    exhibitTraces.remove(identity);
                }
            }
        }
    }
    
    /**
     * This method is called for almost every single request to get the interactive session.
     * So, be sure it is optimized. 
     * 
     * @param request
     * @param id
     * @return
     */
    public InteractiveSession getInteractiveSession(ServletRequest request, String id) {
        String keyString;
        if (request instanceof HttpServletRequest) {
            HttpSession session = ((HttpServletRequest) request).getSession(true);
            keyString = session.getId() + "-" + id;
        } else {
            keyString = request.getRemoteHost() + "-" + id;
        }
        
        InteractiveSessionKey key = new InteractiveSessionKey(keyString);
        synchronized (s_interactiveSessions) {
            InteractiveSession is = (InteractiveSession) s_interactiveSessions.retrieve(key);
            if (is == null) {
                ExhibitIdentity exhibitIdentity;
                
                try {
                    exhibitIdentity = ExhibitIdentity.create(request);
                } catch (MalformedURLException e) {
                    _logger.error("Failed to construct exhibit identity from request " + request.toString(), e);
                    return null;
                }
                
                is = new InteractiveSession(exhibitIdentity);
                s_interactiveSessions.store(key, is);
            }
            
            return is;
        }
    }
    
    static private Map<ExhibitIdentity, ExhibitTrace> getExhibitTraceMap(ExhibitIdentity identity) {
        return s_exhibitTracesMaps[identity.hashCode() % TRACE_MAP_COUNT];
    }
    
    static private class InteractiveSessionKey implements Cacheable {
        int m_hash;
        
        public InteractiveSessionKey(String key) {
            m_hash = key.hashCode();
        }
        
        final public int hashCode() {
            return m_hash;
        }

        final public boolean equals(Object o) {
            return this.hashCode() == o.hashCode();
        }

        public void onRemove(Object o) {
            ((InteractiveSession) o).dispose();
        }

        public void onRetrieve(Object o) {
            // do nothing
        }

        public void onStore(Object o) {
            // do nothing
        }
    }
}