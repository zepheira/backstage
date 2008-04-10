package edu.mit.simile.backstage.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;

import edu.mit.simile.backstage.model.data.Expression;

abstract public class Utilities {
    //private static Logger _logger = Logger.getLogger(Utilities.class);
    
    static public Date parseLastModifiedDate(String s) {
        try {
            return new SimpleDateFormat("MM/dd/yyyy H:m:s").parse(s);
        } catch (ParseException e) {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    static public void getScriptableArrayElements(Scriptable o, Collection c) {
        int length = ((Number) o.get("length", o)).intValue();
        for (int i = 0; i < length; i++) {
            c.add(o.get(i, o));
        }
    }

	static public Boolean getBoolean(Scriptable scriptable, String name) {
		Object o = scriptable.get(name, scriptable);
		return (o instanceof Boolean) ? (Boolean) o : null;
	}

	static public Expression getExpression(Scriptable scriptable, String name) {
		Object o = scriptable.get(name, scriptable);
		if (o instanceof Scriptable) {
			return Expression.construct((Scriptable) o);
		} else {
			return null;
		}
	}

	static public String getString(Scriptable scriptable, String name) {
		Object o = scriptable.get(name, scriptable);
		return (o instanceof String) ? (String) o : null;
	}
	
	static public String valueToString(Value value) {
		return (value instanceof Literal) ? ((Literal) value).getLabel() : ((Resource) value).stringValue();
	}
	
}
