package edu.mit.simile.backstage.model.data;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

import edu.mit.simile.backstage.ExhibitIdentity;
import edu.mit.simile.backstage.data.InMemHostedDataLink;

// In the original Backstage, the term "InMemHosted" indicated that the data was loaded
// just-in-time from an URL. We're removing that capability for Exhibit3 Staged mode
// and instead requiring data be pre-uploaded to avoid the inherrent security problems
// with that approach, but are still reusing much of the code.  There is a "Hosted"
// mode but it uses a global database and the implications of that aren't understood
// at this time.

public class InMemHostedDatabase extends Database {
    final private InMemHostedDataLink    _dataLink;
    
    private int _referenceCount;
    
    public InMemHostedDatabase(InMemHostedDataLink dataLink) {
        _dataLink = dataLink;
    }
    
    public InMemHostedDataLink getDataLink() {
        return _dataLink;
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

    synchronized public void setRepository(Repository repo) {
        // break encapsulation to simplify data upload integration.
        // defined here instead of Database since the breakage would
        // be a lot more serious for a HostedDatabase
        _repository = repo;

        // all repos should be sail repos, but our Java master must be served
        try {
            SailRepository sr = (SailRepository)repo;
            _sail = sr.getSail();
        } catch (ClassCastException e) {
            // pass
        }
    }

    synchronized public Repository getRepository() {
        if (_repository == null) {
            String dbDir = System.getProperty("backstage.databaseDir","databases");
            String dbUrl = _dataLink.url.toString();
            String dbName = dbUrl.substring(dbUrl.lastIndexOf("/")+1);
            File dbFile = new File(dbDir,dbName);

            _sail = new MemoryStore(dbFile);
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
