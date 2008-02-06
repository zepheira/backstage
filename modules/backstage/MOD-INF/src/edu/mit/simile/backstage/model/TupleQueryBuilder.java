package edu.mit.simile.backstage.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.algebra.And;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.ProjectionElemList;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.repository.sail.SailRepositoryConnection;

import edu.mit.simile.backstage.util.MyTupleQuery;

public class TupleQueryBuilder {
    private List<TupleExpr> _tupleExprs = new ArrayList<TupleExpr>();
    private List<ValueExpr> _conditions = new ArrayList<ValueExpr>();
    
    private int _varCount = 0;
    
    public void addTupleExpr(TupleExpr p) {
        if (p != null) {
            _tupleExprs.add(p);
        }
    }
    
    public void addCondition(ValueExpr e) {
        if (e != null) {
            _conditions.add(e);
        }
    }
    
    public Var makeVar(String prefix) {
        return new Var(prefix + _varCount++);
    }
    
    public Var makeVar(String prefix, Value value) {
        return new Var(prefix + _varCount++, value);
    }
    
    public TupleExpr joinTupleExpressions() {
        Iterator<TupleExpr> i = _tupleExprs.iterator();
        TupleExpr result = i.hasNext() ? i.next() : null;
        
        while (i.hasNext()) {
            result = new Join(result, i.next());
        }
        return result;
    }
    
    public ValueExpr joinConditions() {
        ValueExpr expr = null;
        
        for (ValueExpr ve : _conditions) {
            expr = (expr == null) ? ve : new And(expr, ve); 
        }
        
        return expr;
    }
    
    public TupleExpr makeFilterTupleExpr() {
        TupleExpr tupleExprs = joinTupleExpressions();
        ValueExpr conditions = joinConditions();
        
        return (conditions == null) ?
                tupleExprs :
                new Filter(tupleExprs, conditions);
    }
    
    public Projection makeProjection(Var var) {
        return makeProjection(new ProjectionElemList(new ProjectionElem(var.getName())));
    }
    
    public Projection makeProjection(ProjectionElemList projectionElements) {
        return new Projection(
            makeFilterTupleExpr(),
            projectionElements
        );
    }
    
    public TupleQuery makeTupleQuery(Var var, SailRepositoryConnection connection) {
        return makeTupleQuery(new ProjectionElemList(new ProjectionElem(var.getName())), connection);
    }
    
    public TupleQuery makeTupleQuery(ProjectionElemList projectionElements, SailRepositoryConnection connection) {
        return new MyTupleQuery( 
            new ParsedTupleQuery(makeProjection(projectionElements)),
            connection
        );
    }
}
