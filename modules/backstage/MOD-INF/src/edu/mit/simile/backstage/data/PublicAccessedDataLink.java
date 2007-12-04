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

public class PublicAccessedDataLink extends AccessedDataLink {
    private static Logger _logger = Logger.getLogger(AccessedDataLink.class);

    public PublicAccessedDataLink(DataLink entry, Date expiresDate2, Date retrievedDate2, boolean broken2) {
        super(entry, expiresDate2, retrievedDate2, broken2);
    }

    @Override
    public void loadData(URL exhibitURL, Sail sail) throws Exception {
        if (broken) {
            return;
        }

        if (mimeType.equals("application/json")) {
            loadExhibitJSON(exhibitURL, url, charset, sail);
        }
    }

    static public void loadExhibitJSON(URL exhibitURL, URL dataURL, String charset, Sail sail) throws Exception {
        Properties properties = new Properties();
        
        BabelReader reader = new ExhibitJsonReader();
        InputStream is = dataURL.openStream();
        try {
            if (reader.takesReader()) {
                InputStreamReader isr = new InputStreamReader(is, charset);
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
