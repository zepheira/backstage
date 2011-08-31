package edu.mit.simile.backstage.data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openrdf.sail.Sail;

import edu.mit.simile.babel.BabelReader;
import edu.mit.simile.babel.exhibit.ExhibitJsonReader;
import edu.mit.simile.backstage.util.DataLoadingUtilities;

// not currently used

public class PublicAccessedDataLink extends AccessedDataLink {
    private static Logger _logger = Logger.getLogger(AccessedDataLink.class);

    public PublicAccessedDataLink(UnhostedDataLink entry, Date expiresDate2, Date retrievedDate2, boolean broken2) {
        super(entry, expiresDate2, retrievedDate2, broken2);
    }

    @Override
    public void loadData(URL exhibitURL, Sail sail) throws Exception {
        if (broken) {
            return;
        }

        loadExhibitJSON(exhibitURL, url, sail);
        //DataLoadingUtilities.loadDataFromURL(url, mimeType, sail);
    }

    static public void loadExhibitJSON(URL exhibitURL, URL dataURL, Sail sail) throws Exception {
        Properties properties = new Properties();
        
        BabelReader reader = new ExhibitJsonReader();
        InputStream is = dataURL.openStream();
        try {
            if (reader.takesReader()) {
                InputStreamReader isr = new InputStreamReader(is);
                try {
                    reader.read(isr, sail, properties, Locale.getDefault());
                } finally {
                    isr.close();
                }
            } else {
                
            }
        } finally {
            is.close();
        }
        
        _logger.info("Finished loading data from " + dataURL.toExternalForm());
    }
    
    static protected String makeIntoNamespace(String s) {
        if (s.endsWith("#")) {
            return s;
        } else if (s.endsWith("/")) {
            return s;
        } else {
            return s + "#";
        }
    }
}
