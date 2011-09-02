package edu.mit.simile.backstage.model.data;

import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.net.URLEncoder;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import org.openrdf.sail.SailException;
import org.openrdf.sail.SailConnection;

import edu.mit.simile.backstage.ExhibitIdentity;
import edu.mit.simile.backstage.data.AccessedDataLink;

// In the original Backstage, the term "Unhosted" indicated that the data was loaded
// just-in-time from an URL. We're removing that capability for Exhibit3 Staged mode
// and instead requiring data be pre-uploaded to avoid the inherrent security problems
// with that approach, but are still reusing much of the code.  There is a "Hosted"
// mode but it uses a global database and the implications of that aren't understood
// at this time.

public class UnhostedDatabase extends Database {
    final private ExhibitIdentity                    _identity;
    final private AccessedDataLink    _dataLink;
    
    private int _referenceCount;
    
    public UnhostedDatabase(ExhibitIdentity identity, AccessedDataLink dataLink) {
        _identity = identity;
        _dataLink = dataLink;
    }
    
    public ExhibitIdentity getIdentity() {
        return _identity;
    }
    
    public int getReferenceCount() {
        return _referenceCount;
    }
    
    public void addReference() {
        _referenceCount++;
    }
    
    public void removeReference() {
        _referenceCount--;
    }
    
    synchronized public Repository getRepository() {
        if (_repository == null) {
            String dbDir = System.getProperty("backstage.databaseDir","databases");
            String dbUrl = _dataLink.url.toString();
            String dbName = dbUrl.substring(dbUrl.lastIndexOf("/")+1);
            File fullDbDir = new File(dbDir,dbName);
            _sail = new MemoryStore(new File(dbDir,dbName));
            _logger.error("created new MemoryStore for "+dbName+" = "+_sail);
            _repository = new SailRepository(_sail);
            try {
                _repository.initialize();
            } catch (RepositoryException e) {
                _logger.error("Failed to initialize repository", e);
            }
        }
        return _repository;
    }
}
