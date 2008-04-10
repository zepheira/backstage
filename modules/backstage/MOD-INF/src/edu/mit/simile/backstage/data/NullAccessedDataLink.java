package edu.mit.simile.backstage.data;

import java.net.URL;
import java.util.Date;

import org.openrdf.sail.Sail;

public class NullAccessedDataLink extends AccessedDataLink {

    public NullAccessedDataLink(UnhostedDataLink entry, Date expiresDate2, Date retrievedDate2, boolean broken2) {
        super(entry, expiresDate2, retrievedDate2, broken2);
    }

    @Override
    public void loadData(URL exhibitURL, Sail sail) throws Exception {
        // do nothing
    }
}
