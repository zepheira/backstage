package edu.mit.simile.backstage.model.ui.facets;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mozilla.javascript.Scriptable;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.query.algebra.Compare;
import org.openrdf.query.algebra.Or;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.Compare.CompareOp;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.model.data.Expression;
import edu.mit.simile.backstage.model.data.ExpressionException;
import edu.mit.simile.backstage.model.data.ExpressionResult;
import edu.mit.simile.backstage.util.Utilities;

public class ListFacet extends Facet {
    private static Logger _logger = Logger.getLogger(ListFacet.class);
    
    protected Expression    _expression;
    protected Set<String>   _selection = new HashSet<String>();
    protected boolean       _selectMissing;
    
    protected TupleQueryBuilder _builder;
    protected Var               _itemVar;
    
    public ListFacet(Context context, String id) {
        super(context, id);
    }

    @Override
    public void configure(Scriptable config, BackChannel backChannel) {
        super.configure(config, backChannel);
        _expression = Expression.construct((Scriptable) config.get("expression", config));
        
        Utilities.getScriptableArrayElements(
            (Scriptable) config.get("selection", config), _selection); 
        _logger.info("selection " + _selection.size());
        
        _collection.addFacet(this, backChannel);
    }

    @Override
    public boolean hasRestrictions() {
        return _selection.size() > 0 || _selectMissing;
    }

    @Override
    public void restrict(TupleQueryBuilder builder, Var itemVar) throws ExpressionException {
        if (!hasRestrictions()) {
            return;
        }
        
        ExpressionResult result = _expression.computeOutputOnItem(
            _context.getDatabase(), 
            builder, 
            itemVar
        );
        
        if (!(result.valueExpr instanceof Var)) {
            throw new ExpressionException("Facet expression does not evaluate to a Var");
        }
        
        ValueExpr input = result.valueExpr;
        ValueExpr condition = null;
        
        if (_selectMissing) {
            // what do we do???
        }
        for (String v : _selection) {
            condition = createRestrictionClause(builder, v, result.valueType, input, condition);
        }
        builder.addCondition(condition);
    }

    @Override
    public void update(TupleQueryBuilder queryBuilder, Var itemVar, BackChannel backChannel) throws ExpressionException {
        _builder = queryBuilder;
        _itemVar = itemVar;
        
        backChannel.addComponentChangingState(this);
    }
    
    protected ValueExpr createRestrictionClause(
        TupleQueryBuilder builder, 
        String valueAsString, 
        String valueType, 
        ValueExpr input, 
        ValueExpr previousClauses
    ) {
        Value value = ("item".equals(valueType)) ? 
            _context.getDatabase().getItemURI(valueAsString) :
            new LiteralImpl(valueAsString);
            
        Compare compare = new Compare(input, builder.makeVar("v", value), CompareOp.EQ);
        
        return previousClauses == null ? compare : new Or(previousClauses, compare);
    }
}
