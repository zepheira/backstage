package edu.mit.simile.backstage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.mit.simile.backstage.data.AccessedDataLink;
import edu.mit.simile.backstage.data.NullAccessedDataLink;
import edu.mit.simile.backstage.data.PublicAccessedDataLink;
import edu.mit.simile.backstage.data.UnhostedDataLink;
import edu.mit.simile.backstage.model.data.UnhostedDatabase;

/**
 * A database trace consists of all databases (instantiations) that have the same identity.
 * Often there is only one database per trace. However, if the exhibit corresponding to a database
 * is edited by its author (in such a way that influences the data loaded into the database) while 
 * it is still being viewed by some users, then a new database instantiation is spawned to serve
 * new users (while old users keep getting served with the old database instantiation).
 * 
 * A database is deemed to have been changed when its data is deemed to have been changed. Changes
 * to its data are detected by examining expires/last-modified dates of the data links. 
 * 
 * @author dfhuynh
 */
public class DatabaseTrace {
    private static Logger _logger = Logger.getLogger(DatabaseTrace.class);
    private ExhibitIdentity        _identity;
    private AccessedDataLink _dataLink;
    
    private Set<UnhostedDatabase>  _oldDatabases = new HashSet<UnhostedDatabase>();
    private UnhostedDatabase       _latestDatabase;
    
    public DatabaseTrace(ExhibitIdentity identity) {
        _identity = identity;
        _dataLink = null;
    }
    
    public ExhibitIdentity getIdentity() {
        return _identity;
    }
    
    public UnhostedDatabase getDatabase(UnhostedDataLink dataLink) {
        boolean same = _latestDatabase != null && _dataLink.url.equals(dataLink.url);

        if (!same) {
            _dataLink = createAccessedDataLink(dataLink);
            
            _latestDatabase = new UnhostedDatabase(_identity, _dataLink);
            
            _oldDatabases.add(_latestDatabase);
        }
        
        _latestDatabase.addReference();
        
        return _latestDatabase;
    }

    protected AccessedDataLink createAccessedDataLink(UnhostedDataLink dataLink) {
        return new NullAccessedDataLink(
            dataLink,
            null,
            new Date(),
            false
        );
    }

    public void releaseDatabase(UnhostedDatabase database) {
        database.removeReference();
        if (database.getReferenceCount() == 0) {
            _oldDatabases.remove(database);
            if (database == _latestDatabase) {
                _latestDatabase = null;
            }
        }
    }
    
    public boolean isEmpty() {
        return _oldDatabases.size() == 0;
    }
}
