package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class InteractiveSession extends BackstageScriptableObject {

    private static final long serialVersionUID = -1105545561204629924L;
    
    final private ExhibitIdentity     m_exhibitIdentity;
    final private List<DataLink>      m_dataLinks = new LinkedList<DataLink>();
    
    private Exhibit m_exhibit;
    
    public InteractiveSession(ExhibitIdentity exhibitIdentity) {
        m_exhibitIdentity = exhibitIdentity;
    }
    
    public void dispose() {
        if (m_exhibit != null) {
            getModule().releaseExhibit(m_exhibit);
            m_exhibit = null;
        }
    }

    @Override
    public String getClassName() {
        return "InteractiveSession";
    }
    
    public Object jsFunction_getExhibit() throws MalformedURLException {
        if (m_exhibit == null) {
            m_exhibit = getModule().getExhibit(m_exhibitIdentity, m_dataLinks);
        }
        return wrap(m_exhibit, this);
    }

    public void jsFunction_addDataLink(String url, String mimeType, String charset) throws MalformedURLException {
        if (m_exhibit != null) {
            throw new InternalError("Cannot add more data link after exhibit already initialized");
        }
        
        DataLink dataLink = new DataLink(new URL(url), mimeType, charset);
        
        m_dataLinks.add(dataLink);
    }
    
    public String jsFunction_doIt(String id) {
        return id + "blah";
    }

}
