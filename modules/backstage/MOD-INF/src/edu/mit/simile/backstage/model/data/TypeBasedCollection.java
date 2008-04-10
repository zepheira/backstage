package edu.mit.simile.backstage.model.data;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;

import edu.mit.simile.backstage.model.BackChannel;
import edu.mit.simile.backstage.model.Exhibit;
import edu.mit.simile.backstage.model.TupleQueryBuilder;

public class TypeBasedCollection extends Collection {
	protected URI _typeURI;
	
    public TypeBasedCollection(Exhibit exhibit, String id) {
        super(exhibit, id);
    }

    @Override
    public void configure(Scriptable config, BackChannel backChannel) {
        super.configure(config, backChannel);
        
        String itemTypes = (String) config.get("itemTypes", config);
        
        _typeURI = _exhibit.getDatabase().getTypeURI(itemTypes);
    }

    public Var getAllItems(TupleQueryBuilder builder, Var defaultVar) {
        Var var = defaultVar != null ? defaultVar : builder.makeVar("item");
        
        builder.addTupleExpr(
            new StatementPattern(
                var, 
                builder.makeVar("p", RDF.TYPE),
                builder.makeVar("type", _typeURI)
            )
        );
        
        return var;
    }
}
