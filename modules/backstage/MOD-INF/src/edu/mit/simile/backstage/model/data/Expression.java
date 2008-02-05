package edu.mit.simile.backstage.model.data;

import org.mozilla.javascript.Scriptable;

public class Expression {
    static public Expression construct(Scriptable o) {
        Scriptable rootNode = (Scriptable) o.get("rootNode", o);
        return constructExpression(rootNode);
    }
    
    static public Expression constructExpression(Scriptable o) {
        String type = (String) o.get("type", o);
        if ("path".equals(type)) {
            return constructPath(o);
        }
        return null;
    }
    
    static protected Path constructPath(Scriptable o) {
        String rootVariable = (String) o.get("rootName", o);
        Path path = new Path(rootVariable);
        
        Scriptable segments = (Scriptable) o.get("segments", o);
        int length = ((Number) segments.get("length", segments)).intValue();
        for (int i = 0; i < length; i++) {
            Scriptable so = (Scriptable) segments.get(i, segments);
            
            String propertyID = (String) so.get("property", so);
            boolean forward = ((Boolean) so.get("forward", so)).booleanValue();
            boolean isArray = ((Boolean) so.get("isArray", so)).booleanValue();
            
            path.appendSegment(propertyID, forward, isArray);
        }
        
        return path;
    }
    
    public boolean isPath() {
        return false;
    }
}
