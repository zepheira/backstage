package edu.mit.simile.backstage.model.data;

import org.openrdf.query.algebra.ValueExpr;

public class ExpressionResult {
    final public ValueExpr      valueExpr;
    final public String         valueType;
    
    public ExpressionResult(ValueExpr valueExpr, String valueType) {
        this.valueExpr = valueExpr;
        this.valueType = valueType;
    }
}
