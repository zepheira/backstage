package edu.mit.simile.backstage.model.data;

import java.util.ArrayList;
import java.util.List;

public class Path extends Expression {
    static public class PathSegment {
        final public String     propertyID;
        final public boolean    forward;
        final public boolean    isArray;
        
        protected PathSegment(String propertyID, boolean forward, boolean isArray) {
            this.propertyID = propertyID;
            this.forward = forward;
            this.isArray = isArray;
        }
    }
    
    protected String            _rootVariable = null;
    protected List<PathSegment> _segments = new ArrayList<PathSegment>();
    
    public Path(String rootVariable) {
        _rootVariable = rootVariable;
    }
    
    @Override
    public boolean isPath() {
        return true;
    }
    
    public String getRootVariable() {
        return _rootVariable;
    }
    
    public int getSegmentCount() {
        return _segments.size();
    }
    
    public PathSegment getSegment(int i) {
        return _segments.get(i);
    }
    
    public PathSegment getLastSegment() {
        return _segments.size() > 0 ? _segments.get(_segments.size() - 1) : null;
    }
    
    public void appendSegment(String propertyID, boolean forward, boolean isArray) {
        _segments.add(new PathSegment(propertyID, forward, isArray));
    }
}
