package edu.mit.simile.backstage.model.ui.views;

import org.apache.log4j.Logger;
import org.mozilla.javascript.Scriptable;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelTreePrinter;
import org.openrdf.repository.sail.SailRepositoryConnection;

import edu.mit.simile.backstage.model.Context;
import edu.mit.simile.backstage.model.TupleQueryBuilder;
import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.util.DefaultScriptableObject;
import edu.mit.simile.backstage.util.ScriptableArrayBuilder;

public class TileView extends View {
    private static Logger _logger = Logger.getLogger(TileView.class);

    public TileView(Context context, String id) {
        super(context, id);
    }

    @Override
    public Scriptable getComponentState() {
        Database database = _context.getDatabase();
        
        DefaultScriptableObject result = new DefaultScriptableObject();
        ScriptableArrayBuilder itemIDs = new ScriptableArrayBuilder();
        int count = 0;
        
        try {
            TupleQueryBuilder builder = new TupleQueryBuilder();
            
            Var itemVar = getCollection().getRestrictedItems(builder, null);
            
            SailRepositoryConnection connection = (SailRepositoryConnection)
                database.getRepository().getConnection();
            
            try {
                TupleQuery query = builder.makeTupleQuery(itemVar, connection);
                
                _logger.info("query = " + QueryModelTreePrinter.printTree(builder.makeProjection(itemVar)));
                
                TupleQueryResult queryResult = query.evaluate();
                try {
                    while (queryResult.hasNext()) {
                        BindingSet bindingSet = queryResult.next();
                        Value v = bindingSet.getValue(itemVar.getName());
                        if (v instanceof URI) {
                            if (count < 100) {
                                itemIDs.add(database.getItemId((URI) v));
                            }
                            count++;
                        }
                    }
                } finally {
                    queryResult.close();
                }
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            _logger.error("Error querying for restricted items", e);
        }
        
        result.put("items", result, itemIDs.toArray());
        result.put("count", result, count);
    
        return result;
    }
}
