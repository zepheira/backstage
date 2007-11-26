package edu.mit.simile.backstage;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;

public class ScriptableBackstage extends BackstageScriptableObject {
    private static final long serialVersionUID = -6840851588510351185L;

    public static String getName() {
        return "Backstage";
    }
    
    public String getClassName() {
        return getName();
    }
    
    // ---------------------------------------------------------------------
    
    public Object jsFunction_getInteractiveSession(Object requestO, String id) throws MalformedURLException {
        HttpServletRequest request = (HttpServletRequest) unwrap(requestO);
        
        return getModule().getInteractiveSession(request, id);
    }
    
}
