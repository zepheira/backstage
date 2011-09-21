package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.log4j.Logger;

import edu.mit.simile.backstage.model.Exhibit;

public class ScriptableBackstage extends BackstageScriptableObject {
    private static final long serialVersionUID = -6840851588510351185L;
    
    @SuppressWarnings("unused")
	private static Logger _logger = Logger.getLogger(ScriptableBackstage.class);
    
    public static String getName() {
        return "Backstage";
    }
    
    public String getClassName() {
        return getName();
    }
    
    // ---------------------------------------------------------------------
    
    public Object jsFunction_createExhibit(Object requestO, String refererUrlSHA1, String id) throws MalformedURLException {
        HttpServletRequest request = (HttpServletRequest) unwrap(requestO);
        ExhibitCollection ec = getExhibitCollection(request);
        Exhibit exhibit = getModule().createExhibit(request, refererUrlSHA1);
        
        ec.setExhibit(id, exhibit);
        
        return wrap(exhibit, this);
    }
    
    public Object jsFunction_getExhibit(Object requestO, String id) throws MalformedURLException {
        HttpServletRequest request = (HttpServletRequest) unwrap(requestO);
        ExhibitCollection ec = getExhibitCollection(request);
        Exhibit exhibit = ec.getExhibit(id);
        
        return wrap(exhibit, this);
    }
    
    static private ExhibitCollection getExhibitCollection(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        ExhibitCollection isc = (ExhibitCollection) 
            session.getAttribute("exhibits");
        
        if (isc == null) {
            isc = new ExhibitCollection();
            session.setAttribute("exhibits", isc);
        }
        return isc;
    }
    
    static private class ExhibitCollection implements HttpSessionBindingListener {
        private Map<String, Exhibit> _sessions = new HashMap<String, Exhibit>();
        
        public Exhibit getExhibit(String id) {
            return _sessions.get(id);
        }
        
        public void setExhibit(String id, Exhibit exhibit) {
            _sessions.put(id, exhibit);
        }
        
        public void valueBound(HttpSessionBindingEvent event) {
            // nothing to do
        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            for (Exhibit exhibit : _sessions.values()) {
                exhibit.dispose();
            }
        }
        
    }
}
