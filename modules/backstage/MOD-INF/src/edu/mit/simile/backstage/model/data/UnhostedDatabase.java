package edu.mit.simile.backstage.model.data;

import java.util.LinkedList;
import java.util.List;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import edu.mit.simile.backstage.ExhibitIdentity;
import edu.mit.simile.backstage.data.AccessedDataLink;

public class UnhostedDatabase extends Database {
    final private ExhibitIdentity           _identity;
    final private List<AccessedDataLink>    _dataLinks;
    
    private int _referenceCount;
    
    public UnhostedDatabase(ExhibitIdentity identity, List<AccessedDataLink> dataLinks) {
        _identity = identity;
        _dataLinks = new LinkedList<AccessedDataLink>(dataLinks);
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
            _sail = new MemoryStore();
            _repository = new SailRepository(_sail);
            try {
                _repository.initialize();
                
                for (AccessedDataLink dataLink : _dataLinks) {
                    try {
                        dataLink.loadData(_identity.getURL(), _sail);
                    } catch (Exception e) {
                        _logger.error("Failed to load data into exhibit from " + dataLink.url.toExternalForm(), e);
                    }
                }
            } catch (RepositoryException e) {
                _logger.error("Failed to initialize repository", e);
            }
        }
        return _repository;
    }
}
