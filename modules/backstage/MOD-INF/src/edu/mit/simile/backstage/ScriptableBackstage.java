package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.net.URL;

public class ScriptableBackstage extends BackstageScriptableObject {
    private static final long serialVersionUID = -6840851588510351185L;

    public static String getName() {
        return "Backstage";
    }
    
    public String getClassName() {
        return getName();
    }
    
    // ---------------------------------------------------------------------

    public Object jsFunction_getExhibit(String urlString, String lastModified) throws MalformedURLException {
        return getModule().getExhibit(new URL(urlString), lastModified);
    }
    
}
