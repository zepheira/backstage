package edu.mit.simile.backstage.data;

// used to contain other advisory metadata when the URL was dereferenced for its data,
// but now only the URL is needed. FIXME; replace by URL

import java.net.URL;

public class UnhostedDataLink extends DataLink {
    final public URL      url;
    
    public UnhostedDataLink( URL url2) {
        url = url2;
    }
}
