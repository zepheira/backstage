package edu.mit.simile.backstage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.mit.simile.backstage.data.AccessedDataLink;
import edu.mit.simile.backstage.data.NullAccessedDataLink;
import edu.mit.simile.backstage.data.PublicAccessedDataLink;
import edu.mit.simile.backstage.data.UnhostedDataLink;
import edu.mit.simile.backstage.model.data.UnhostedDatabase;

/**
 * A database trace consists of all databases (instantiations) that have the same identity.
 * Often there is only one database per trace. However, if the exhibit corresponding to a database
 * is edited by its author (in such a way that influences the data loaded into the database) while 
 * it is still being viewed by some users, then a new database instantiation is spawned to serve
 * new users (while old users keep getting served with the old database instantiation).
 * 
 * A database is deemed to have been changed when its data is deemed to have been changed. Changes
 * to its data are detected by examining expires/last-modified dates of the data links. 
 * 
 * @author dfhuynh
 */
public class DatabaseTrace {
    private ExhibitIdentity        _identity;
    private List<AccessedDataLink> _dataLinks;
    
    private Set<UnhostedDatabase>  _oldDatabases = new HashSet<UnhostedDatabase>();
    private UnhostedDatabase       _latestDatabase;
    
    public DatabaseTrace(ExhibitIdentity identity) {
        _identity = identity;
        _dataLinks = new LinkedList<AccessedDataLink>();
    }
    
    public ExhibitIdentity getIdentity() {
        return _identity;
    }
    
    public UnhostedDatabase getDatabase(List<UnhostedDataLink> dataLinks) {
        boolean same = _latestDatabase != null && _dataLinks.size() == dataLinks.size();
        
        if (same) {
            Date now = new Date();
            
            for (int i = 0; i < _dataLinks.size(); i++) {
                AccessedDataLink myDataLink = _dataLinks.get(i);
                UnhostedDataLink theirDataLink = dataLinks.get(i);
                
                if (!myDataLink.url.equals(theirDataLink.url) ||
                    !myDataLink.mimeType.equals(theirDataLink.mimeType) ||
                    !myDataLink.charset.equals(theirDataLink.charset)) {
                    
                    same = false;
                    break;
                }
                
                if (myDataLink.broken) {
                    /*
                     * We don't want a broken data link to keep forcing new instantiations,
                     * which is taxing on our server if many users are viewing that exhibit.
                     */
                    continue;
                }
                
                if (myDataLink.expiresDate != null) {
                    if (myDataLink.expiresDate.before(now)) {
                        same = false;
                        break;
                    } else {
                        continue; // optimistic: we won't check "last-modified" if "expires" is OK
                    }
                }
                
                Date lastModified = getLastModified(myDataLink.url);
                if (lastModified == null || lastModified.after(myDataLink.retrievedDate)) {
                    same = false;
                    break;
                }
            }
            
        }
        
        if (!same) {
            _dataLinks.clear();
            for (int i = 0; i < dataLinks.size(); i++) {
                _dataLinks.add(createAccessedDataLink(dataLinks.get(i)));
            }
            
            _latestDatabase = new UnhostedDatabase(_identity, _dataLinks);
            
            _oldDatabases.add(_latestDatabase);
        }
        
        _latestDatabase.addReference();
        
        return _latestDatabase;
    }
    
    public void releaseDatabase(UnhostedDatabase database) {
        database.removeReference();
        if (database.getReferenceCount() == 0) {
            _oldDatabases.remove(database);
            if (database == _latestDatabase) {
                _latestDatabase = null;
            }
        }
    }
    
    public boolean isEmpty() {
        return _oldDatabases.size() == 0;
    }
    
    static private Date getLastModified(URL url) {
        try {
            URLConnection connection = url.openConnection();
            
            connection.setRequestProperty("Connection", "Close");
            connection.setUseCaches(true);
            
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                httpConnection.setRequestMethod("HEAD");
            }
            
            connection.connect();
            
            long lastModified = connection.getLastModified();
            
            return (lastModified != 0) ? new Date(lastModified) : null;
        } catch (IOException e) {
            return null;
        }
    }
    
    protected AccessedDataLink createAccessedDataLink(UnhostedDataLink dataLink) {
        String protocol = dataLink.url.getProtocol();
        if (protocol.equals("http") || protocol.equals("https") || protocol.equals("ftp")) {
            return createPublicAccessedDataLink(dataLink);
        } else {
            return new NullAccessedDataLink(
                dataLink, 
                null,
                new Date(),
                false
            );
        }
    }
    
    protected PublicAccessedDataLink createPublicAccessedDataLink(UnhostedDataLink dataLink) {
        try {
            URLConnection connection = dataLink.url.openConnection();
            
            connection.setRequestProperty("Connection", "Close");
            connection.setUseCaches(true);
            connection.connect();
            
            long expires = connection.getExpiration();
            
            return new PublicAccessedDataLink(
                dataLink, 
                (expires != 0) ? new Date(expires) : null,
                new Date(),
                false
            );
        } catch (IOException e) {
            return new PublicAccessedDataLink(
                dataLink, 
                null,
                new Date(),
                true
            );
        }
    }}
