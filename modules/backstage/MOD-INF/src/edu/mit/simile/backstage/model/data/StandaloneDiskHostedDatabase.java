package edu.mit.simile.backstage.model.data;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.backstage.util.DataLoadingUtilities;

public class StandaloneDiskHostedDatabase extends Database {
    protected static Logger _logger = LoggerFactory.getLogger("backstage.hosted-database");
    
	public StandaloneDiskHostedDatabase(File databaseDir) {
		_repository = DataLoadingUtilities.createNativeRepository(databaseDir);
		_sail = ((SailRepository) _repository).getSail();
	}
	
	@Override
	public Repository getRepository() {
		return _repository;
	}
}
