package edu.mit.simile.backstage.model.data;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;

import edu.mit.simile.backstage.util.DataLoadingUtilities;

public class HostedDatabase extends Database {
	public HostedDatabase(File sourceToLoad, File databaseDir) {
		boolean load = !databaseDir.exists();
		
		_repository = DataLoadingUtilities.createNativeRepository(databaseDir);
		_sail = ((SailRepository) _repository).getSail();
		
		if (load) {
			if (sourceToLoad == null) {
				_logger.warn("No hosted data source to load");
			} else if (!sourceToLoad.exists()) {
				_logger.warn("Hosted data source " + sourceToLoad.getAbsolutePath() + " does not exist");
			} else if (!sourceToLoad.canRead()) {
				_logger.warn("Hosted data source " + sourceToLoad.getAbsolutePath() + " is not readable");
			} else {
				try {
					if (sourceToLoad.isDirectory()) {
						DataLoadingUtilities.loadDataFromDir(sourceToLoad, _repository, true);
					} else {
						DataLoadingUtilities.loadDataFromFile(sourceToLoad, _repository);
					}
				} catch (Exception e) {
					_logger.error("Failed to load hosted data source at " + sourceToLoad.getAbsolutePath(), e);
				}
			}
		}
	}
	
	@Override
	public Repository getRepository() {
		return _repository;
	}
}
