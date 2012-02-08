package edu.mit.simile.backstage;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.ExtendedProperties;

import edu.mit.simile.backstage.data.DataLink;
import edu.mit.simile.backstage.util.DataLoadingUtilities;
import edu.mit.simile.backstage.model.Exhibit;
import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.data.HostedDatabase;
import edu.mit.simile.backstage.model.data.StandaloneDiskHostedDatabase;
import edu.mit.simile.backstage.model.data.OnDiskHostedDatabase;
import edu.mit.simile.backstage.model.data.InMemHostedDatabase;

import edu.mit.simile.butterfly.ButterflyModuleImpl;

import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.Repository;

public class BackstageModule extends ButterflyModuleImpl {
    
    static Map<String, Database> s_linkDatabaseMap;
    static StandaloneDiskHostedDatabase s_standaloneDatabase;

    // The supported types of repositories. Could use enum but don't
    // know how to represent them via Rhino
    public static String REPOTYPE_DISK = "DISK";
    public static String REPOTYPE_MEM = "MEM";

    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);
        if (s_linkDatabaseMap == null) {
            s_linkDatabaseMap = new HashMap<String, Database>();
        }
    }

    public Database getDatabase(String url) {
        DataLink link;
        try {
            link = new DataLink(new URL(url));
        } catch (MalformedURLException e) {
            _logger.error("Invalid data link URL", e);
            return null;
        }
        return this.getDatabase(link);
    }

    public Repository createRepository(HttpServletRequest request, String repoType, String slug) throws Exception {
        ExtendedProperties properties = getProperties();
        String dbDir = properties.getString("backstage.databaseDir","databases");

        SailRepository repository = null;
        File thisDbDir = new File(new File(dbDir,repoType),slug);

        if (repoType.equals("mem")) {
            DataLoadingUtilities.RepoSailTuple rs = DataLoadingUtilities.createMemoryRepository(thisDbDir);
            repository = (SailRepository)rs.repository;
            rs = null;
        } else if (repoType.equals("disk")) {
            DataLoadingUtilities.RepoSailTuple rs = DataLoadingUtilities.createNativeRepository(thisDbDir);
            repository = (SailRepository)rs.repository;
            rs = null;
        } else {
            return null;
        }

        String lang = DataLoadingUtilities.contentTypeToLang(request.getContentType());
        if (lang == null) {
            throw new Exception("Unsupported content type");
        }

        DataLoadingUtilities.loadDataFromStream( (InputStream)request.getInputStream(),
                                                 request.getRequestURL().toString(),
                                                 lang, repository.getSail() );
        return repository;
    }

    public Database getDatabase(DataLink dataLink) {
        Database db = s_linkDatabaseMap.get(dataLink.url.toString());

        if (db == null) {
            // inspect the link to determine our repository type, relativizing
            // against our Butterfly mount point
            URI dbUri = null;
            URI mountUri = null;
            try {
                dbUri = new URI(dataLink.url.toString());
                mountUri = new URI(this.getMountPoint().getMountPoint()); // awkward!
            } catch (URISyntaxException e) {
                return null;
            }
            
            URI fullMountUri = dbUri.resolve(mountUri);
            String mountPath = dbUri.toString().substring(fullMountUri.toString().length());
            String[] mountPathSegs = mountPath.toString().split(File.separator);
            if (mountPathSegs.length != 3) {
                return null;
            }

            String repoType = mountPathSegs[1];
            if (repoType.equals("mem")) {
                db = new InMemHostedDatabase(dataLink);
            } else if (repoType.equals("disk")) {
                db = new OnDiskHostedDatabase(dataLink);
            } else {
                return null;
            }

            s_linkDatabaseMap.put(dataLink.url.toString(), db);
        }
        return db;
    }
    
    public void releaseDatabase(HostedDatabase database) {
        DataLink link = database.getDataLink();
        s_linkDatabaseMap.remove(link.url.toString());
    }
    
    public Database getStandaloneDatabase() {
    	if (s_standaloneDatabase == null) {
	    	ExtendedProperties properties = getProperties();
	    	String databaseString = properties.getString("backstage.hostedData.database");
	    	
	    	File database = (databaseString == null || databaseString.length() == 0) ? 
	    			new File("database") : new File(databaseString);
	    	
	    	s_standaloneDatabase = new StandaloneDiskHostedDatabase(database);
    	}
    	return s_standaloneDatabase;
    }
    
    /**
     * @param request
     * @param id
     * @return
     */
    public Exhibit createExhibit(HttpServletRequest request, String refererUrlSHA1) {
        ExhibitIdentity exhibitIdentity;
        
        try {
            exhibitIdentity = ExhibitIdentity.create(request, refererUrlSHA1);
        } catch (MalformedURLException e) {
            _logger.error("Failed to construct exhibit identity from request " + request.toString(), e);
            return null;
        }
        
        return new Exhibit(this, exhibitIdentity);
    }
}
