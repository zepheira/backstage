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
                DataLoadingUtilities.RepoSailTuple rs = DataLoadingUtilities.createNativeRepository(databaseDir);
		_repository = rs.repository;
		_sail = rs.sail;
                rs = null;
	}
	
	@Override
	public Repository getRepository() {
		return _repository;
	}
}
