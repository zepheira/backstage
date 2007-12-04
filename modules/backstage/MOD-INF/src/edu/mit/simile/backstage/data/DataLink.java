/**
 * 
 */
package edu.mit.simile.backstage.data;

import java.net.URL;

public class DataLink {
    final public URL      url;
    final public String   mimeType;
    final public String   charset;
    
    public DataLink(
        URL     url2,
        String  mimeType2,
        String  charset2
    ) {
        url = url2;
        mimeType = mimeType2;
        charset = charset2;
    }
}