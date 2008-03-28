package edu.mit.simile.backstage.model.data;

import org.openrdf.query.TupleQuery;
import org.openrdf.query.algebra.Var;

public class ExpressionQueryResult {
    final public TupleQuery     tupleQuery;
    final public String         valueType;
    final public Var			resultVar;
    
    public ExpressionQueryResult(TupleQuery tupleQuery, String valueType, Var resultVar) {
        this.tupleQuery = tupleQuery;
        this.valueType = valueType;
        this.resultVar = resultVar;
    }
}
