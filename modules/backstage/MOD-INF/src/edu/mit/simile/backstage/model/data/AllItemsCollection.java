package edu.mit.simile.backstage.model.data;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Exhibit;
import edu.mit.simile.backstage.model.TupleQueryBuilder;

public class AllItemsCollection extends Collection {

    public AllItemsCollection(Exhibit exhibit, String id) {
        super(exhibit, id);
    }

    @Override
    public void configure(Scriptable config, BackChannel backChannel) {
        super.configure(config, backChannel);
        // nothing to do
    }
    
    public Var getAllItems(TupleQueryBuilder builder, Var defaultVar) {
        Var var = defaultVar != null ? defaultVar : builder.makeVar("item");
        
        builder.addTupleExpr(
            new StatementPattern(
                var, 
                builder.makeVar("p", RDF.TYPE),
                builder.makeVar("ignore")
            )
        );
        
        return var;
    }
}
