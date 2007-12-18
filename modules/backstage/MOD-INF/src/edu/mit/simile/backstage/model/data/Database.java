package edu.mit.simile.backstage.model.data;

import info.aduna.iteration.CloseableIteration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.memory.MemoryStore;

import edu.mit.simile.babel.exhibit.ExhibitOntology;
import edu.mit.simile.backstage.ExhibitIdentity;
import edu.mit.simile.backstage.data.AccessedDataLink;
import edu.mit.simile.backstage.util.SailUtilities;


public class Database {
    private static final long serialVersionUID = -8804204966521106254L;
    private static Logger _logger = Logger.getLogger(Database.class);
    
    final private ExhibitIdentity           _identity;
    final private List<AccessedDataLink>    _dataLinks;
    
    private int         _referenceCount;
    private Sail        _sail;
    private Repository  _repository;
    
    /*
     * Cached computed information
     */
    private List<PropertyRecord> _propertyRecords;
    private List<TypeRecord>     _typeRecords;
    
    private Map<URI, String>     _typeUriToId = new HashMap<URI, String>();
    private Map<String, URI>     _typeIdToUri = new HashMap<String, URI>();
    
    private Map<URI, String>     _propertyUriToId = new HashMap<URI, String>();
    private Map<String, URI>     _propertyIdToUri = new HashMap<String, URI>();
    
    private Map<URI, String>     _itemUriToId = new HashMap<URI, String>();
    private Map<String, URI>     _itemIdToUri = new HashMap<String, URI>();
    private boolean              _abbreviatedItems = false;
    
    public Database(ExhibitIdentity identity, List<AccessedDataLink> dataLinks) {
        _identity = identity;
        _dataLinks = new LinkedList<AccessedDataLink>(dataLinks);
    }
    
    public ExhibitIdentity getIdentity() {
        return _identity;
    }
    
    public int getReferenceCount() {
        return _referenceCount;
    }
    
    public void addReference() {
        _referenceCount++;
    }
    
    public void removeReference() {
        _referenceCount--;
    }
    
    public List<PropertyRecord> getPropertyRecords() {
        computeCachedInformation();
        return _propertyRecords;
    }
    
    public List<TypeRecord> getTypeRecords() {
        computeCachedInformation();
        return _typeRecords;
    }
    
    private void computeCachedInformation() {
        if (_propertyRecords == null || _typeRecords == null) {
            getRepository();
            
            SailConnection sc = null;
            try {
                sc = _sail.getConnection();
            } catch (SailException e) {
                _logger.error("Failed to open sail connection in order to compute cached information", e);
            }
            
            if (sc != null) {
                try {
                    _propertyRecords = internalGetPropertyRecords(sc);
                    _typeRecords = internalGetTypeRecords(sc);
                } finally {
                    try {
                        sc.close();
                    } catch (SailException e) {
                        _logger.warn("Failed to close sail connection", e);
                    }
                }
            }
        }
    }
    
    private List<PropertyRecord> internalGetPropertyRecords(SailConnection sc) {
        List<PropertyRecord> records = new LinkedList<PropertyRecord>();
        
        CloseableIteration<? extends Statement, SailException> i;
        
        try {
            i = sc.getStatements(null, null, null, true);
        } catch (SailException e) {
            _logger.error("Failed to get all statements in order to get property records", e);
            return records;
        }
        
        Set<URI> predicates = new HashSet<URI>();
        try {
            while (i.hasNext()) {
                Statement s = i.next();
                predicates.add(s.getPredicate());
            }
        } catch (SailException e) {
            _logger.warn("Failed to iterate through statements", e);
        } finally {
            try {
                i.close();
            } catch (SailException e) {
                _logger.warn("Failed to close statement iterator", e);
            }
        }
        
        for (URI predicate : predicates) {
            PropertyRecord r = createPropertyRecord(predicate, sc);
            if (r != null) {
                records.add(r);
            }
        }
        
        return records;
    }
    
    private List<TypeRecord> internalGetTypeRecords(SailConnection sc) {
        List<TypeRecord> records = new LinkedList<TypeRecord>();
        
        getRepository();
        
        CloseableIteration<? extends Statement, SailException> i;
        
        try {
            i = sc.getStatements(null, RDF.TYPE, null, true);
        } catch (SailException e) {
            _logger.error("Failed to get all statements in order to get type records", e);
            return records;
        }
        
        Set<URI> types = new HashSet<URI>();
        try {
            while (i.hasNext()) {
                Statement s = i.next();
                types.add((URI) s.getObject());
            }
        } catch (SailException e) {
            _logger.warn("Failed to iterate through statements", e);
        } finally {
            try {
                i.close();
            } catch (SailException e) {
                _logger.warn("Failed to close statement iterator", e);
            }
        }
        
        for (URI type : types) {
            TypeRecord r = createTypeRecord(type, sc);
            if (r != null) {
                records.add(r);
            }
        }
        
        return records;
    }
    
    private PropertyRecord createPropertyRecord(URI predicate, SailConnection sc) {
        String id = getPropertyId(predicate, sc);
        String label = SailUtilities.getStringObject(sc, predicate, RDFS.LABEL, id);
        String valueType = SailUtilities.getStringObject(sc, predicate, ExhibitOntology.VALUE_TYPE, "text");
        Properties properties = new Properties();
        
        CloseableIteration<? extends Statement, SailException> i = null;
        try {
            i = sc.getStatements(predicate, null, null, true);
        } catch (SailException e) {
            _logger.error("Failed to get all statements in order to get property record", e);
            return null;
        }
        
        if (i != null) {
            try {
                while (i.hasNext()) {
                    Statement s = i.next();
                    URI p = s.getPredicate();
                    Value o = s.getObject();
                    
                    if (!p.equals(RDFS.LABEL) && !p.equals(ExhibitOntology.ID) && !p.equals(ExhibitOntology.VALUE_TYPE)) {
                        properties.put(p.getLocalName(), SailUtilities.valueToString(o));
                    }
                }
            } catch (SailException e) {
                _logger.warn("Failed to iterate through statements", e);
            } finally {
                try {
                    i.close();
                } catch (SailException e) {
                    _logger.warn("Failed to close statement iterator", e);
                }
            }
        }
        
        return new PropertyRecord(predicate, id, label, valueType, properties);
    }
    
    private TypeRecord createTypeRecord(URI type, SailConnection sc) {
        String id = getTypeId(type, sc);
        String label = SailUtilities.getStringObject(sc, type, RDFS.LABEL, id);
        Properties properties = new Properties();
        
        CloseableIteration<? extends Statement, SailException> i = null;
        try {
            i = sc.getStatements(type, null, null, true);
        } catch (SailException e) {
            _logger.error("Failed to get all statements in order to get type record", e);
            return null;
        }
        
        if (i != null) {
            try {
                while (i.hasNext()) {
                    Statement s = i.next();
                    URI p = s.getPredicate();
                    Value o = s.getObject();
                    
                    if (!p.equals(RDFS.LABEL) && !p.equals(ExhibitOntology.ID)) {
                        properties.put(p.getLocalName(), SailUtilities.valueToString(o));
                    }
                }
            } catch (SailException e) {
                _logger.warn("Failed to iterate through statements", e);
            } finally {
                try {
                    i.close();
                } catch (SailException e) {
                    _logger.warn("Failed to close statement iterator", e);
                }
            }
        }
        
        return new TypeRecord(type, id, label, properties);
    }
    
    private String getPropertyId(URI predicate, SailConnection sc) {
        if (_propertyUriToId.containsKey(predicate)) {
            return _propertyUriToId.get(predicate);
        }
        
        String idBase = SailUtilities.getStringObject(sc, predicate, ExhibitOntology.ID);
        if (idBase == null) {
            idBase = predicate.getLocalName();
        }
        
        String id = idBase;
        int i = 0;
        while (_propertyIdToUri.containsKey(id)) {
            i++;
            id = idBase + i;
        }
        
        _propertyUriToId.put(predicate, id);
        _propertyIdToUri.put(id, predicate);
        
        return id;
    }
    
    private String getTypeId(URI uri, SailConnection sc) {
        if (_typeUriToId.containsKey(uri)) {
            return _typeUriToId.get(uri);
        }
        
        String idBase = SailUtilities.getStringObject(sc, uri, ExhibitOntology.ID);
        if (idBase == null) {
            idBase = uri.getLocalName();
        }
        
        String id = idBase;
        int i = 0;
        while (_typeIdToUri.containsKey(id)) {
            i++;
            id = idBase + i;
        }
        
        _typeUriToId.put(uri, id);
        _typeIdToUri.put(id, uri);
        
        return id;
    }
    
    private String getItemId(URI uri, SailConnection sc) {
        if (_itemUriToId.containsKey(uri)) {
            return _itemUriToId.get(uri);
        }
        
        String idBase = SailUtilities.getStringObject(sc, uri, ExhibitOntology.ID);
        if (idBase == null) {
            idBase = SailUtilities.getStringObject(sc, uri, RDFS.LABEL);
        }
        if (idBase == null) {
            idBase = uri.getLocalName();
        }
        
        String id = idBase;
        int i = 0;
        while (_itemIdToUri.containsKey(id)) {
            i++;
            id = idBase + i;
        }
        
        _itemUriToId.put(uri, id);
        _itemIdToUri.put(id, uri);
        
        return id;
    }
    
    synchronized public Repository getRepository() {
        if (_repository == null) {
            _sail = new MemoryStore();
            _repository = new SailRepository(_sail);
            try {
                _repository.initialize();
                
                for (AccessedDataLink dataLink : _dataLinks) {
                    try {
                        dataLink.loadData(_identity.getURL(), _sail);
                    } catch (Exception e) {
                        _logger.error("Failed to load data into exhibit from " + dataLink.url.toExternalForm(), e);
                    }
                }
            } catch (RepositoryException e) {
                _logger.error("Failed to initialize repository", e);
            }
        }
        return _repository;
    }
    
    public String getItemId(URI uri) {
        abbreviateItems();
        return _itemUriToId.get(uri);
    }
    
    public URI getItemURI(String id) {
        abbreviateItems();
        return _itemIdToUri.get(id);
    }
    
    synchronized void abbreviateItems() {
        if (_abbreviatedItems) {
            return;
        }
        _abbreviatedItems = true;
        
        getRepository();
        
        SailConnection sc = null;
        try {
            sc = _sail.getConnection();
        } catch (SailException e) {
            _logger.error("Failed to open sail connection in order to compute cached information", e);
        }
        
        if (sc != null) {
            try {
                CloseableIteration<? extends Statement, SailException> i;
                
                try {
                    i = sc.getStatements(null, RDF.TYPE, null, true);
                } catch (SailException e) {
                    _logger.error("Failed to get all statements in order to abbreviate items", e);
                    return;
                }
                
                try {
                    while (i.hasNext()) {
                        Statement s = i.next();
                        Resource r = s.getSubject();
                        if (r instanceof URI) {
                            getItemId((URI) r, sc);
                        }
                    }
                } catch (SailException e) {
                    _logger.warn("Failed to iterate through statements", e);
                } finally {
                    try {
                        i.close();
                    } catch (SailException e) {
                        _logger.warn("Failed to close statement iterator", e);
                    }
                }
            } finally {
                try {
                    sc.close();
                } catch (SailException e) {
                    _logger.warn("Failed to close sail connection", e);
                }
            }
        }
    }
    
    static public class PropertyRecord {
        final public URI    uri;
        final public String id;
        final public String label;
        final public String valueType;
        final private Properties _properties;
        
        private PropertyRecord(URI uri, String id, String label, String valueType, Properties properties) {
            this.uri = uri;
            this.id = id;
            this.label = label;
            this.valueType = valueType;
            this._properties = properties;
        }
        
        public String getProperty(String id) {
            return _properties.getProperty(id);
        }
    }
    
    static public class TypeRecord {
        final public URI    uri;
        final public String id;
        final public String label;
        final private Properties _properties;
        
        private TypeRecord(URI uri, String id, String label, Properties properties) {
            this.uri = uri;
            this.id = id;
            this.label = label;
            this._properties = properties;
        }
        
        public String getProperty(String id) {
            return _properties.getProperty(id);
        }
    }
}
