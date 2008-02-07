package edu.mit.simile.backstage.model.ui.facets;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mozilla.javascript.Scriptable;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.Compare;
import org.openrdf.query.algebra.Count;
import org.openrdf.query.algebra.Group;
import org.openrdf.query.algebra.GroupElem;
import org.openrdf.query.algebra.Or;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.ProjectionElemList;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.Compare.CompareOp;
import org.openrdf.query.algebra.helpers.QueryModelTreePrinter;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepositoryConnection;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.data.Expression;
import edu.mit.simile.backstage.model.data.ExpressionException;
import edu.mit.simile.backstage.model.data.ExpressionResult;
import edu.mit.simile.backstage.util.DefaultScriptableObject;
import edu.mit.simile.backstage.util.Group2;
import edu.mit.simile.backstage.util.MyTupleQuery;
import edu.mit.simile.backstage.util.ScriptableArrayBuilder;
import edu.mit.simile.backstage.util.Utilities;

public class ListFacet extends Facet {
    private static Logger _logger = Logger.getLogger(ListFacet.class);
    
    protected Expression    _expression;
    protected Set<String>   _selection = new HashSet<String>();
    protected boolean       _selectMissing;
    
    protected TupleQueryBuilder _builder;
    protected Var               _itemVar;
    protected Var               _valueVar;
    protected Var               _countVar;
    protected String            _valueType;
    
    public ListFacet(Context context, String id) {
        super(context, id);
    }

    @Override
    public void configure(Scriptable config, BackChannel backChannel) {
        super.configure(config, backChannel);
        _expression = Expression.construct((Scriptable) config.get("expression", config));
        
        Utilities.getScriptableArrayElements(
            (Scriptable) config.get("selection", config), _selection); 
        
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
        
        ExpressionResult result = _expression.computeOutputOnItem(
            _context.getDatabase(), 
            _builder, 
            _itemVar
        );
        
        if (!(result.valueExpr instanceof Var)) {
            throw new ExpressionException("Facet expression does not evaluate to a Var");
        }
        
        _valueVar = (Var) result.valueExpr;
        _valueType = result.valueType;
        _countVar = _builder.makeVar("count");
        
        backChannel.addComponentChangingState(this);
    }
    
    @Override
    public Scriptable getComponentState() {
        DefaultScriptableObject result = new DefaultScriptableObject();
        Database database = _context.getDatabase();
        try {
            SailRepositoryConnection connection = (SailRepositoryConnection)
                database.getRepository().getConnection();
            
            try {
                Group2 group = new Group2(_builder.makeFilterTupleExpr());
                group.addGroupElement(new GroupElem(_countVar.getName(), new Count(_itemVar)));
                group.addGroupBindingName(_valueVar.getName());
                
                ProjectionElemList projectionElements = new ProjectionElemList();
                projectionElements.addElement(new ProjectionElem(_valueVar.getName()));
                projectionElements.addElement(new ProjectionElem(_countVar.getName()));
                
                Projection projection = new Projection(group, projectionElements);
                
                TupleQuery query = new MyTupleQuery(new ParsedTupleQuery(projection), connection);
                TupleQueryResult queryResult = query.evaluate();
                try {
                	createComponentState(queryResult, result);
                } finally {
                    queryResult.close();
                }
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            _logger.error("Error querying to compute list facet", e);
        }
        
        return result;
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
    
    protected void createComponentState(TupleQueryResult queryResult, Scriptable result) throws QueryEvaluationException {
        ScriptableArrayBuilder facetChoices = new ScriptableArrayBuilder();
        while (queryResult.hasNext()) {
            BindingSet bindingSet = queryResult.next();
            
            Value value = bindingSet.getValue(_valueVar.getName());
            Value count = bindingSet.getValue(_countVar.getName());
            
            String s = value.stringValue();
            int c = Integer.parseInt(count.stringValue());
            
            DefaultScriptableObject valueO = new DefaultScriptableObject();
            valueO.put("value", valueO, s);
            valueO.put("count", valueO, c);
            
            facetChoices.add(valueO);
        }
    	
        result.put("values", result, facetChoices.toArray());
    }
}
