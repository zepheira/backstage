package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class InteractiveSession {

    private static final long serialVersionUID = -1105545561204629924L;
    
    final private BackstageModule     m_module;
    final private ExhibitIdentity     m_exhibitIdentity;
    final private List<DataLink>      m_dataLinks = new LinkedList<DataLink>();
    
    private Exhibit m_exhibit;
    
    public InteractiveSession(BackstageModule module, ExhibitIdentity exhibitIdentity) {
        m_module = module;
        m_exhibitIdentity = exhibitIdentity;
    }
    
    public void dispose() {
        if (m_exhibit != null) {
            m_module.releaseExhibit(m_exhibit);
            m_exhibit = null;
        }
    }

    public Exhibit getExhibit() throws MalformedURLException {
        if (m_exhibit == null) {
            m_exhibit = m_module.getExhibit(m_exhibitIdentity, m_dataLinks);
        }
        return m_exhibit;
    }

    public void addDataLink(String url, String mimeType, String charset) throws MalformedURLException {
        if (m_exhibit != null) {
            throw new InternalError("Cannot add more data link after exhibit already initialized");
        }
        
        DataLink dataLink = new DataLink(new URL(url), mimeType, charset);
        
        m_dataLinks.add(dataLink);
    }
}
