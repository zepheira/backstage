package edu.mit.simile.backstage.model.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;

import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.model.data.Database.PropertyRecord;

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
    
    @Override
    public ExpressionResult computeOutput(
        Database                database, 
        TupleQueryBuilder       builder, 
        Map<String, ValueExpr>  variableValues,
        Map<String, String>     variableTypes
    ) throws ExpressionException {
        String      rootName = _rootVariable != null ? _rootVariable : "value";
        ValueExpr   valueExpr = variableValues.get(rootName);
        String      valueType = variableTypes.get(rootName);
        for (PathSegment segment : _segments) {
            if (valueExpr instanceof Var) {
                Var input = (Var) valueExpr;
                
                PropertyRecord record = database.getPropertyRecord(segment.propertyID);
                Var output = builder.makeVar("seg");
                Var propertyVar = builder.makeVar("seg", record.uri);
                if (segment.forward) {
                    builder.addTupleExpr(new StatementPattern(input, propertyVar, output));
                    valueType = record.valueType;
                } else {
                    builder.addTupleExpr(new StatementPattern(output, propertyVar, input));
                    valueType = "item";
                }
                
                valueExpr = output;
            } else {
                throw new ExpressionException("Non-final set in a path must be a variable."); 
            }
        }
        
        return new ExpressionResult(valueExpr, valueType);
    }
    
    @Override
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	
        if (_rootVariable != null) {
        	sb.append(_rootVariable);
        }
        
        for (PathSegment segment : _segments) {
        	sb.append(segment.forward ? '.' : '!');
        	if (segment.isArray) {
        		sb.append('@');
        	}
        	sb.append(segment.propertyID);
        }
        
        return sb.toString();
    }
}
