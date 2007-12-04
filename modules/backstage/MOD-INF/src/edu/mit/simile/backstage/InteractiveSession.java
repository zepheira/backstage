package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.mit.simile.backstage.data.DataLink;

public class InteractiveSession {
    private static Logger _logger = Logger.getLogger(Exhibit.class);

    private static final long serialVersionUID = -1105545561204629924L;
    
    final private BackstageModule     _module;
    final private ExhibitIdentity     _exhibitIdentity;
    final private List<DataLink>      _dataLinks = new LinkedList<DataLink>();
    
    private Exhibit m_exhibit;
    
    public InteractiveSession(BackstageModule module, ExhibitIdentity exhibitIdentity) {
        _module = module;
        _exhibitIdentity = exhibitIdentity;
    }
    
    public void dispose() {
        if (m_exhibit != null) {
            _logger.info("Disposing interaction session for " + _exhibitIdentity.toString());
            
            _module.releaseExhibit(m_exhibit);
            m_exhibit = null;
        }
    }

    public Exhibit getExhibit() throws MalformedURLException {
        if (m_exhibit == null) {
            m_exhibit = _module.getExhibit(_exhibitIdentity, _dataLinks);
        }
        return m_exhibit;
    }

    public void addDataLink(String url, String mimeType, String charset) throws MalformedURLException {
        if (m_exhibit != null) {
            throw new InternalError("Cannot add more data link after exhibit already initialized");
        }
        
        DataLink dataLink = new DataLink(new URL(url), mimeType, charset);
        
        _dataLinks.add(dataLink);
    }
}
