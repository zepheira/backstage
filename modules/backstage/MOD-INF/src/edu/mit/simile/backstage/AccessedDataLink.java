/**
 * 
 */
package edu.mit.simile.backstage;

import java.util.Date;

class AccessedDataLink extends DataLink {
    final public Date    expiresDate;
    final public Date    retrievedDate;
    final public boolean broken;
    
    public AccessedDataLink(DataLink entry, Date expiresDate2, Date retrievedDate2, boolean broken2) {
        super(entry.url, entry.mimeType, entry.charset);
        expiresDate = expiresDate2;
        retrievedDate = retrievedDate2;
        broken = broken2;
    }
}