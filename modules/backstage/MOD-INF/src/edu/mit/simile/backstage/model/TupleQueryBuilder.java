package edu.mit.simile.backstage.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Var;

public class TupleQueryBuilder {
    private List<TupleExpr> _tupleExprs = new ArrayList<TupleExpr>();
    private int _varCount = 0;
    
    public void addTupleExpr(TupleExpr p) {
        _tupleExprs.add(p);
    }
    
    public TupleExpr join() {
        Iterator<TupleExpr> i = _tupleExprs.iterator();
        TupleExpr result = i.hasNext() ? i.next() : null;
        
        while (i.hasNext()) {
            result = new Join(result, i.next());
        }
        return result;
    }
    
    public Var makeVar(String prefix) {
        return new Var(prefix + _varCount++);
    }
    
    public Var makeVar(String prefix, Value value) {
        return new Var(prefix + _varCount++, value);
    }
}
