package edu.mit.simile.backstage.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.ParseErrorListener;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.nativerdf.NativeStore;

import edu.mit.simile.babel.BabelReader;
import edu.mit.simile.babel.exhibit.ExhibitJsonReader;

public class DataLoadingUtilities {
    private static Logger _logger = Logger.getLogger(DataLoadingUtilities.class);
    
    final static public String s_bnodePrefix = "urn:bnode:";
    
    static public Repository createMemoryRepository() {
        try {
            Repository r = new SailRepository(new MemoryStore());
            r.initialize();
            return r;
        } catch (Exception e) {
            _logger.error(e);
            return null;
        }
    }

    static public Repository createNativeRepository(File dir) {
        try {
            Sail sail = new NativeStore();
            sail.setDataDir(dir);
            ((NativeStore) sail).setTripleIndexes("spoc,posc,opsc");
            Repository r = new SailRepository(sail);
            r.initialize();
            return r;
        } catch (Exception e) {
            _logger.error(e);
            return null;
        }
    }
    
    static public String fileToModelLang(File file) {
        return filenameToLang(file.getName());
    }

    static public String urlToModelLang(URL url, String contentType) {
        return urlToModelLang(url.getPath(), contentType);
    }

    static public String urlToModelLang(String url, String contentType) {
        String lang = null;

        if (contentType != null) {
            lang = contentTypeToLang(contentType);
        }
        if (lang == null) {
            lang = filenameToLang(url);
        }

        if (_logger.isDebugEnabled()) _logger.debug(url + " -> " + lang);
        return lang;
    }

    static public String contentTypeToLang(String contentType) {
        String lang = null;
        if ("application/rss+xml".equals(contentType) || "application/atom+xml".equals(contentType)) {
            lang = "RSS";
        } else if ("application/rdf+xml".equals(contentType) || "text/xml".equals(contentType)) {
            lang = "RDFXML";
        } else if ("application/n3".equals(contentType) || "text/rdf+n3".equals(contentType)
                || "application/turtle".equals(contentType) || "application/x-turtle".equals(contentType)) {
            lang = "N3";
        } else if ("application/json".equals(contentType)) {
            lang = "Exhibit/JSON";
        }
        if (_logger.isDebugEnabled()) _logger.debug(contentType + " -> " + lang);
        return lang;
    }
    
    static public String filenameToLang(String filename) {
        String contentType = URLConnection.guessContentTypeFromName(filename);
        String lang = null;

        if (contentType != null) {
            lang = contentTypeToLang(contentType);
        }

        if (lang == null) {
            if (filename.endsWith(".gz")) {
                filename = filename.substring(0, filename.length() - ".gz".length());
            }
            if (filename.endsWith(".n3")) {
                lang = "N3";
            } else if (filename.endsWith(".turtle")) {
                lang = "TURTLE";
            } else if (filename.endsWith(".ntriples")) {
                lang = "NTRIPLES";
            } else if (filename.endsWith(".rss")) {
                lang = "RSS";
            } else if (filename.endsWith(".rdf") 
                    || filename.endsWith(".rdfs") 
                    || filename.endsWith(".xml")
                    || filename.endsWith(".owl")) {
                lang = "RDFXML";
            } else if (filename.endsWith(".json") 
                    || filename.endsWith(".js")) {
            	lang = "Exhibit/JSON";
            }
        }

        return lang;
    }

    static public RDFFormat langToFormat(String lang) {
        return RDFFormat.valueOf(lang);
    }

    static public String uriToFilename(String uri) {
        return uri.replace(':', '_').replace('/', '_');
    }
    
    static public void loadDataFromDir(File dir, Sail sail, boolean forgiving) throws Exception {
        if (!dir.exists()) {
            throw new FileNotFoundException("Cannot load data from " + dir.getAbsolutePath());
        }
        
        _logger.info("Loading data from dir " + dir.getAbsolutePath());

        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (!file.isHidden()) {
                if (file.isDirectory()) {
                    loadDataFromDir(file, sail, forgiving);
                } else {
                    if (forgiving) {
                        try {
                            loadDataFromFile(file, sail);
                        } catch (Exception e) {
                            _logger.warn("Failed to load data from " + file.getCanonicalPath(), e);
                        }
                    } else {
                        loadDataFromFile(file, sail);
                    }
                }
            }
        }
    }

    static public InputStream getStreamForFile(File file) throws Exception {
        InputStream stream = new FileInputStream(file);
        String name = file.getName();
        if (name.endsWith(".gz")) {
            stream = new GZIPInputStream(stream);
        }
        return stream;
    }
    
    static public void loadDataFromFile(File file, Sail sail) throws Exception {
        String lang = fileToModelLang(file);
        if (lang != null) {
        	_logger.info("Loading data from file " + file.getAbsolutePath());
        	
            InputStream fis = getStreamForFile(file);
            try {
                loadDataFromStream(fis, file.toURL().toExternalForm(), lang, sail);
            } catch (Exception e) {
                throw new RuntimeException("Error loading data from file: " + file + " " + e.getMessage());
            } finally {
                fis.close();
            }
        } else {
            throw new ModelReadFromFileException("Unknown data format in " + file.getAbsolutePath());
        }
    }

    static public void loadDataFromConnection(URLConnection conn, URL url, String lang, String contentType, Sail sail) throws Exception {
        if (lang != null) {
            InputStream stream = conn.getInputStream();
            try {
                loadDataFromStream(stream, url.toExternalForm(), lang, sail);
            } catch (Exception e) {
                throw new RuntimeException("Error loading data from URL: " + url + " " + e.getMessage());
            } finally {
                stream.close();
            }
        } else {
            throw new ModelReadFromFileException("Unknown data format in " + url.toExternalForm());
        }
    }

    static public void loadDataFromURL(URL url, String contentType, Sail sail) throws Exception {
        String lang = urlToModelLang(url, contentType);
        if (lang != null) {
            URLConnection conn = url.openConnection();
            
            //setRequestHeaders(conn, LongwellUtilities.getLabel() + "/" + LongwellUtilities.getVersion());
            
            conn.connect();
            InputStream stream = conn.getInputStream();
            try {
                loadDataFromStream(stream, url.toExternalForm(), lang, sail);
            } catch (Exception e) {
                throw new RuntimeException("Error loading data from URL: " + url + " " + e.getMessage());
            } finally {
                stream.close();
            }
        } else {
            throw new ModelReadFromFileException("Unknown data format in " + url.toExternalForm());
        }
    }

    static public void setRequestHeaders(URLConnection conn, String ua) {
        conn.setRequestProperty("User-Agent", ua);
        conn.setRequestProperty("Accept", "application/rdf+xml, text/rdf+n3");
    }

    static public void loadDataFromStream(InputStream stream, String sourceURL, String lang, Sail sail) throws Exception {
        Repository r = createMemoryRepository();

        lang = lang.toLowerCase();
        if ("exhibit/json".equals(lang)) {
            Properties properties = new Properties();
            
            BabelReader reader = new ExhibitJsonReader();
            try {
                if (reader.takesReader()) {
                    InputStreamReader isr = new InputStreamReader(stream);
                  	reader.read(isr, sail, properties, Locale.getDefault());
                } else {
                   	reader.read(stream, sail, properties, Locale.getDefault());
                }
            } finally {
                stream.close();
            }
        } else {
            RDFParser parser = null;
            if ("rdfxml".equals(lang)) {
                parser = new RDFXMLParser(r.getValueFactory());
            } else if ("n3".equals(lang) || "turtle".equals(lang)) {
                parser = new TurtleParser(r.getValueFactory());
            } else if ("ntriples".equals(lang)) {
                parser = new NTriplesParser(r.getValueFactory());
            }
            
            try {
	            SailConnection c = null;
	            try {
	                c = sail.getConnection();
	                BNodeConverterStatementHandler handler = new BNodeConverterStatementHandler(c);
	
	                parser.setRDFHandler(handler);
	                parser.setParseErrorListener(new LoggingParseErrorListener(sourceURL));
	                parser.setVerifyData(false);
	                parser.setStopAtFirstError(false);
	
	                parser.parse(stream, sourceURL);
	                
	                c.commit();
	
	                _logger.info("Read " + handler.m_count + " statements from '" + sourceURL + "'");
	            } catch (RepositoryException e) {
	                if (c != null) c.rollback();
	            } finally {
	                if (c != null) c.close();
	            }
	        } catch (Exception e) {
	            throw new ModelReadFromFileException("Failed to read data from '" + sourceURL + "'", e);
	        } finally {
	            stream.close();
	        }
        }
    }
    
    static class ModelReadFromFileException extends IOException {
        private static final long serialVersionUID = -5802084055919147883L;

        ModelReadFromFileException(String s) {
            super(s);
        }

        ModelReadFromFileException(String s, Throwable e) {
            super(s);
            initCause(e);
        }
    }

    static class BNodeConverterStatementHandler implements RDFHandler {

        SailConnection m_connection;
        
        long m_count;

        URI m_uri = null;

        Map<String,URI> m_bnodeIDToURI = new HashMap<String,URI>();

        BNodeConverterStatementHandler(SailConnection conn) throws RepositoryException {
            m_connection = conn;
        }

        public void handleStatement(Statement st, Resource context) throws RDFHandlerException {
            Resource s = st.getSubject();
            URI p = st.getPredicate();
            Value o = st.getObject();
            if (s instanceof BNode) {
                String sid = ((BNode) s).getID();
                s = (URI) m_bnodeIDToURI.get(sid);
                if (s == null) {
                    s = addBNode(sid);
                }
            } else {
                m_uri = (URI) s;
            }

            if (o instanceof BNode) {
                String oid = ((BNode) o).getID();
                o = (URI) m_bnodeIDToURI.get(oid);
                if (o == null) {
                    o = addBNode(oid);
                }
            }

            try {
                m_connection.addStatement(s, p, o);
                m_count++;
            } catch (SailException e) {
                _logger.error(e);
			}
        }

        URI addBNode(String bnode) {
            URI uri = new URIImpl(s_bnodePrefix
                    + (m_uri != null ? m_uri.toString() + ":" : "") + System.currentTimeMillis() + ":" + bnode);

            m_bnodeIDToURI.put(bnode, uri);

            return uri;
        }

        public void startRDF() throws RDFHandlerException {
        }

        public void endRDF() throws RDFHandlerException {
            try {
                m_connection.commit();
            } catch (SailException e) {
                try {
                    m_connection.rollback();
                } catch (Exception ee) {
                    // ignore
                }
                throw new RDFHandlerException(e);
            }
        }

        public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {
            // 
        }

        public void handleComment(String arg0) throws RDFHandlerException {
            // 
        }

        public void handleStatement(Statement arg0) throws RDFHandlerException {
            handleStatement(arg0, null);
        }
    }
        
    static class LoggingParseErrorListener implements ParseErrorListener {
        String m_source;

        LoggingParseErrorListener(File file) {
            m_source = file.getAbsolutePath();
        }

        LoggingParseErrorListener(URL url) {
            m_source = url.toExternalForm();
        }

        LoggingParseErrorListener(String source) {
            m_source = source;
        }

        public void warning(String msg, int line, int column) {
            _logger.warn("Warning: " + msg + " at " + m_source + " [" + line + "," + column + "]");
        }

        public void error(String msg, int line, int column) {
            _logger.error("Error: " + msg + " at " + m_source + " [" + line + "," + column + "]");
        }

        public void fatalError(String msg, int line, int column) {
            _logger.error("Fatal error: " + msg + " at " + m_source + " [" + line + "," + column + "]");
        }
    }
}
