package edu.mit.simile.backstage.util;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

public class SailUtilities {
    static public String valueToString(Value o) {
        return (o instanceof Literal) ? 
                ((Literal) o).getLabel() : 
                ((o instanceof URI) ? ((URI) o).toString() : ((BNode) o).getID());        
    }
    
    static public Value getObject(SailRepositoryConnection sc, Resource subject, URI predicate) {
        try {
        	RepositoryResult<Statement> i = sc.getStatements(subject, predicate, null, true, (Resource[]) null);
            try {
                if (i.hasNext()) {
                    return i.next().getObject();
                }
            } finally {
                i.close();
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }
    
    static public Value getObject(SailConnection sc, Resource subject, URI predicate) {
        try {
            CloseableIteration<? extends Statement, SailException> i =
                sc.getStatements(subject, predicate, null, true);
            
            try {
                if (i.hasNext()) {
                    return i.next().getObject();
                }
            } finally {
                i.close();
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }
    
    static public String getStringObject(SailConnection sc, Resource subject, URI predicate) {
        try {
            CloseableIteration<? extends Statement, SailException> i =
                sc.getStatements(subject, predicate, null, true);
            
            try {
                if (i.hasNext()) {
                    Value v = i.next().getObject();
                    if (v instanceof Literal) {
                        return ((Literal) v).getLabel();
                    }
                }
            } finally {
                i.close();
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }
    
    static public String getStringObject(SailConnection sc, Resource subject, URI predicate, String defaultResult) {
        String s = getStringObject(sc, subject, predicate);
        return s != null ? s : defaultResult;
    }
    
    static public Value getSubject(SailConnection sc, Value object, URI predicate) {
        try {
            CloseableIteration<? extends Statement, SailException> i =
                sc.getStatements(null, predicate, object, true);
            
            try {
                if (i.hasNext()) {
                    return i.next().getSubject();
                }
            } finally {
                i.close();
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }
    
}
