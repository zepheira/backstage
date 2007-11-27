package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public class ScriptableBackstage extends BackstageScriptableObject {
    private static final long serialVersionUID = -6840851588510351185L;

    public static String getName() {
        return "Backstage";
    }
    
    public String getClassName() {
        return getName();
    }
    
    // ---------------------------------------------------------------------
    
    public Object jsFunction_createInteractiveSession(Object requestO, String id) throws MalformedURLException {
        HttpServletRequest request = (HttpServletRequest) unwrap(requestO);
        InteractiveSessionCollection isc = getInteractiveSessionCollection(request);
        InteractiveSession is = getModule().createInteractiveSession(request, id);
        
        isc.setInteractiveSession(id, is);
        
        return is;
    }
    
    public Object jsFunction_getInteractiveSession(Object requestO, String id) throws MalformedURLException {
        HttpServletRequest request = (HttpServletRequest) unwrap(requestO);
        InteractiveSessionCollection isc = getInteractiveSessionCollection(request);
        InteractiveSession is = isc.getInteractiveSession(id);
        
        return is;
    }
    
    private InteractiveSessionCollection getInteractiveSessionCollection(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        InteractiveSessionCollection isc = (InteractiveSessionCollection) 
            session.getAttribute("interactive-sessions");
        
        if (isc == null) {
            isc = new InteractiveSessionCollection();
            session.setAttribute("interactive-sessions", isc);
        }
        return isc;
    }
    
    static private class InteractiveSessionCollection implements HttpSessionBindingListener {
        private Map<String, InteractiveSession> _sessions = new HashMap<String, InteractiveSession>();
        
        public InteractiveSession getInteractiveSession(String id) {
            return _sessions.get(id);
        }
        
        public void setInteractiveSession(String id, InteractiveSession is) {
            _sessions.put(id, is);
        }
        
        public void valueBound(HttpSessionBindingEvent event) {
            // nothing to do
        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            for (InteractiveSession is : _sessions.values()) {
                is.dispose();
            }
        }
        
    }
}
