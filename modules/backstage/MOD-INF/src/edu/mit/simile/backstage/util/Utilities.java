package edu.mit.simile.backstage.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

abstract public class Utilities {
    static public Date parseLastModifiedDate(String s) {
        try {
            return new SimpleDateFormat("MM/dd/yyyy H:m:s").parse(s);
        } catch (ParseException e) {
            return null;
        }
    }
}
