package edu.mit.simile.backstage.model.data;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

import edu.mit.simile.backstage.ExhibitIdentity;
import edu.mit.simile.backstage.data.DataLink;
import edu.mit.simile.backstage.util.DataLoadingUtilities;

// In the original Backstage, the term "Unhosted" indicated that the data was loaded
// just-in-time from an URL. We're removing that capability for Exhibit3 Staged mode
// and instead requiring data be pre-uploaded to avoid the inherrent security problems
// but are still reusing much of the code.

// Originally, HostedDatabase was a single standalone disk based Sesame repository,
// but as all supported modes are now "hosted", this class has been changed to
// provide a generic abstraction for both individual in-memory and on-disk stores.

public abstract class HostedDatabase extends Database {
    final private DataLink    _dataLink;
    
    private int _referenceCount;
    
    public HostedDatabase(DataLink dataLink) {
        _dataLink = dataLink;
    }
    
    public DataLink getDataLink() {
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

    protected abstract DataLoadingUtilities.RepoSailTuple createRepo(File f);

    protected abstract String getRepoType();

    synchronized public Repository getRepository() {
        if (_repository == null) {
            String dbDir = System.getProperty("backstage.databaseDir","databases");
            String dbUrl = _dataLink.url.toString();
            String dbName = dbUrl.substring(dbUrl.lastIndexOf("/")+1);

            File dbFile = new File(new File(dbDir,getRepoType()),dbName);

            DataLoadingUtilities.RepoSailTuple rs = createRepo(dbFile);
            _repository = rs.repository;
            _sail = rs.sail; // set inherited internal sail
            rs = null;
        }
        return _repository;
    }
}
