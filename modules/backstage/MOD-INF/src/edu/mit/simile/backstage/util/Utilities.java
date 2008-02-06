package edu.mit.simile.backstage.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.mozilla.javascript.Scriptable;

abstract public class Utilities {
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
}
