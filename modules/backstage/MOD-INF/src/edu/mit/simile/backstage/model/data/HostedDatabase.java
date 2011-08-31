package edu.mit.simile.backstage.model.data;

import java.io.File;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;

import edu.mit.simile.backstage.util.DataLoadingUtilities;

public class HostedDatabase extends Database {
    protected static Logger _logger = Logger.getLogger(HostedDatabase.class);
    
	public HostedDatabase(File databaseDir) {
		_repository = DataLoadingUtilities.createNativeRepository(databaseDir);
		_sail = ((SailRepository) _repository).getSail();
	}
	
	@Override
	public Repository getRepository() {
		return _repository;
	}
}
