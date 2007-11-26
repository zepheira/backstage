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

/**
 * An exhibit trace consists of all exhibits (instantiations) that have the same identity.
 * Often there is only one exhibit per trace. However, if an exhibit is edited by its author while 
 * it is still being viewed by some users, then a new exhibit instantiation is spawned to serve
 * new users (while old users keep getting served with the old exhibit instantiation).
 * 
 * An exhibit is deemed to have been changed when its data is deemed to have been changed. Changes
 * to its data are detected by examining expires/last-modified dates of the data links. 
 * 
 * @author dfhuynh
 */
public class ExhibitTrace {
    private ExhibitIdentity          _identity;
    private List<AccessedDataLink> _dataLinks;
    
    private Set<Exhibit>        _oldExhibits = new HashSet<Exhibit>();
    private Exhibit             _latestExhibit;
    
    public ExhibitTrace(ExhibitIdentity identity) {
        _identity = identity;
        _dataLinks = new LinkedList<AccessedDataLink>();
    }
    
    public ExhibitIdentity getIdentity() {
        return _identity;
    }
    
    public Exhibit getExhibit(List<DataLink> dataLinks) {
        boolean same = _latestExhibit != null && _dataLinks.size() == dataLinks.size();
        
        if (same) {
            Date now = new Date();
            
            for (int i = 0; i < _dataLinks.size(); i++) {
                AccessedDataLink myDataLink = _dataLinks.get(i);
                DataLink theirDataLink = dataLinks.get(i);
                
                if (!myDataLink.url.equals(theirDataLink.url) ||
                    !myDataLink.mimeType.equals(theirDataLink.mimeType) ||
                    !myDataLink.charset.equals(theirDataLink.charset)) {
                    
                    same = false;
                    break;
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
                _dataLinks.add(createDatedDataLink(dataLinks.get(i)));
            }
            
            _latestExhibit = new Exhibit(_identity, _dataLinks);
            
            _oldExhibits.add(_latestExhibit);
        }
        
        _latestExhibit.addReference();
        
        return _latestExhibit;
    }
    
    public void releaseExhibit(Exhibit exhibit) {
        exhibit.removeReference();
        if (exhibit.getReferenceCount() == 0) {
            _oldExhibits.remove(exhibit);
            if (exhibit == _latestExhibit) {
                _latestExhibit = null;
            }
        }
    }
    
    public boolean isEmpty() {
        return _oldExhibits.size() == 0;
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
    
    static private AccessedDataLink createDatedDataLink(DataLink dataLink) {
        try {
            URLConnection connection = dataLink.url.openConnection();
            
            connection.setRequestProperty("Connection", "Close");
            connection.setUseCaches(true);
            connection.connect();
            
            long expires = connection.getExpiration();
            
            AccessedDataLink datedDataLink = new AccessedDataLink(
                dataLink, 
                (expires != 0) ? new Date(expires) : null,
                new Date(),
                false
            );
            
            return datedDataLink;
        } catch (IOException e) {
            return new AccessedDataLink(
                dataLink, 
                null,
                new Date(),
                true
            );
        }
    }
}
