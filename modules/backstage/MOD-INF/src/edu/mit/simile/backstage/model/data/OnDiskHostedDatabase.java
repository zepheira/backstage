package edu.mit.simile.backstage.model.data;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

import edu.mit.simile.backstage.data.DataLink;
import edu.mit.simile.backstage.util.DataLoadingUtilities;

public class OnDiskHostedDatabase extends HostedDatabase {

    public OnDiskHostedDatabase(DataLink datalink) {
        super(datalink);
    }

    protected String getRepoType() {
        return "disk";
    }

    synchronized protected DataLoadingUtilities.RepoSailTuple createRepo(File dbFile) {
        return DataLoadingUtilities.createNativeRepository(dbFile);
    }
}
