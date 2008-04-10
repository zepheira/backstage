package edu.mit.simile.backstage.model.data;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.Value;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.ProjectionElemList;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.repository.sail.SailRepositoryConnection;

import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.util.MyTupleQuery;

abstract public class Expression {
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
    
    public ExpressionResult computeOutputOnItem(
        Database                database, 
        TupleQueryBuilder       builder, 
        Var                     valueVar) throws ExpressionException {
        
        Map<String, ValueExpr> variableValues = new HashMap<String, ValueExpr>();
        variableValues.put("value", valueVar);
        
        Map<String, String> variableTypes = new HashMap<String, String>();
        variableTypes.put("value", "item");
        
        return computeOutput(
            database, 
            builder, 
            variableValues, 
            variableTypes
        );
    }
    
    public ExpressionQueryResult computeOutputOnValue(
        Value value,
        Database database, 
        SailRepositoryConnection connection
    ) throws ExpressionException {
        
        TupleQueryBuilder builder = new TupleQueryBuilder();
        Var valueVar = builder.makeVar("value", value);
        
		ExpressionResult expressionResult = computeOutputOnItem(database, builder, valueVar);
        if (expressionResult.valueExpr instanceof Var) {
        	Var resultVar = (Var) expressionResult.valueExpr;
        	
            ProjectionElemList projectionElements = new ProjectionElemList();
            projectionElements.addElement(new ProjectionElem(resultVar.getName()));
            
            TupleExpr t = builder.makeFilterTupleExpr();
            if (t == null) { 
            	// TODO[dfhuynh]: This happens if the expression is just "value". I'm not sure what to do here.
            	return null;
            }
            
            Projection projection = new Projection(t, projectionElements);
            TupleQuery query = new MyTupleQuery(new ParsedTupleQuery(projection), connection);
            
            return new ExpressionQueryResult(query, expressionResult.valueType, resultVar);
        }
        return null;
    }
    
    abstract public ExpressionResult computeOutput(
            Database                database, 
            TupleQueryBuilder       builder, 
            Map<String, ValueExpr>  variableValues,
            Map<String, String>     variableTypes) throws ExpressionException;
    
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
