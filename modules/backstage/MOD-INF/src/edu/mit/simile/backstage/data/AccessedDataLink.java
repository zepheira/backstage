/**
 * 
 */
package edu.mit.simile.backstage.data;

import java.net.URL;
import java.util.Date;

import org.openrdf.sail.Sail;

abstract public class AccessedDataLink extends UnhostedDataLink {
    final public Date    expiresDate;
    final public Date    retrievedDate;
    final public boolean broken;
    
    public AccessedDataLink(UnhostedDataLink entry, Date expiresDate2, Date retrievedDate2, boolean broken2) {
        super(entry.url);
        expiresDate = expiresDate2;
        retrievedDate = retrievedDate2;
        broken = broken2;
    }
    
    abstract public void loadData(URL exhibitURL, Sail sail) throws Exception;
}
