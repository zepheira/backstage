package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.mit.simile.backstage.data.DataLink;

public class InteractiveSession {
    private static Logger _logger = Logger.getLogger(Database.class);

    private static final long serialVersionUID = -1105545561204629924L;
    
    final private BackstageModule     _module;
    final private ExhibitIdentity     _exhibitIdentity;
    final private List<DataLink>      _dataLinks = new LinkedList<DataLink>();
    
    private Database _database;
    
    public InteractiveSession(BackstageModule module, ExhibitIdentity exhibitIdentity) {
        _module = module;
        _exhibitIdentity = exhibitIdentity;
    }
    
    public void dispose() {
        if (_database != null) {
            _logger.info("Disposing interaction session for " + _exhibitIdentity.toString());
            
            _module.releaseExhibit(_database);
            _database = null;
        }
    }

    public Database getDatabase() throws MalformedURLException {
        if (_database == null) {
            _database = _module.getExhibit(_exhibitIdentity, _dataLinks);
        }
        return _database;
    }

    public void addDataLink(String url, String mimeType, String charset) throws MalformedURLException {
        if (_database != null) {
            throw new InternalError("Cannot add more data link after exhibit already initialized");
        }
        
        DataLink dataLink = new DataLink(new URL(url), mimeType, charset);
        
        _dataLinks.add(dataLink);
    }
}
