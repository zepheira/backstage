package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import edu.mit.simile.backstage.data.DataLink;
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
    static Map<ExhibitIdentity, ExhibitTrace>[] s_exhibitTracesMaps;
    
    @SuppressWarnings("unchecked")
    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);
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
     * @param request
     * @param id
     * @return
     */
    public InteractiveSession createInteractiveSession(HttpServletRequest request, String id) {
        ExhibitIdentity exhibitIdentity;
        
        try {
            exhibitIdentity = ExhibitIdentity.create(request);
        } catch (MalformedURLException e) {
            _logger.error("Failed to construct exhibit identity from request " + request.toString(), e);
            return null;
        }
        
        return new InteractiveSession(this, exhibitIdentity);
    }
    
    static private Map<ExhibitIdentity, ExhibitTrace> getExhibitTraceMap(ExhibitIdentity identity) {
        return s_exhibitTracesMaps[identity.hashCode() % TRACE_MAP_COUNT];
    }
}