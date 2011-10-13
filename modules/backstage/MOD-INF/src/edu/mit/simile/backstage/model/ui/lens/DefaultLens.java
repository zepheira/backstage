package edu.mit.simile.backstage.model.ui.lens;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.data.Database.PropertyRecord;
import edu.mit.simile.backstage.util.DefaultScriptableObject;
import edu.mit.simile.backstage.util.SailUtilities;
import edu.mit.simile.backstage.util.ScriptableArrayBuilder;
import edu.mit.simile.backstage.util.Utilities;

public class DefaultLens extends Lens {
    protected static Logger _logger = LoggerFactory.getLogger("backstage.lens.default-lens");

	@Override
	public void render(URI item, Scriptable result, Database database, SailRepositoryConnection connection) {
		result.put("label", result, database.getItemLabel(database.getItemId(item)));
		
		try {
			RepositoryResult<Statement> r = connection.getStatements(item, null, null, true, SailUtilities.noContext);
			
			Map<URI, Set<Value>> propertyToValues = new HashMap<URI, Set<Value>>();
			try {
				while (r.hasNext()) {
					Statement s = r.next();
					
					URI predicate = s.getPredicate();
					Set<Value> values = propertyToValues.get(predicate);
					if (values == null) {
						values = new HashSet<Value>();
						
						propertyToValues.put(predicate, values);
					}
					values.add(s.getObject());
				}
			} finally {
				r.close();
			}
			
	        DefaultScriptableObject propertiesO = new DefaultScriptableObject();
	        
			for (URI property : propertyToValues.keySet()) {
				String propertyID = database.getPropertyId(property);
				PropertyRecord propertyRecord = database.getPropertyRecord(propertyID);
				
		        DefaultScriptableObject o = new DefaultScriptableObject();
				o.put("propertyLabel", o, propertyRecord.label);
				o.put("valueType", o, propertyRecord.valueType);
				
				ScriptableArrayBuilder valueArrayBuilder = new ScriptableArrayBuilder();
				
				Set<Value> values = propertyToValues.get(property);
				if ("item".equals(propertyRecord.valueType)) {
					for (Value value : values) {
						if (value instanceof URI) {
							URI itemURI = (URI) value;
							String itemID = database.getItemId(itemURI);
							
					        DefaultScriptableObject v = new DefaultScriptableObject();
					        v.put("id", v, itemID);
					        v.put("label", v, database.getItemLabel(itemID));
					        
					        valueArrayBuilder.add(v);
						} else {
							valueArrayBuilder.add(((Resource) value).stringValue());
						}
					}
				} else {
					for (Value value : values) {
						valueArrayBuilder.add(Utilities.valueToString(value));
					}
				}
				o.put("values", o, valueArrayBuilder.toArray());
				
				propertiesO.put(propertyID, propertiesO, o);
			}
			
			result.put("propertyValues", result, propertiesO);
		} catch (Exception e) {
			_logger.error("Failed to generate default lens for item " + item, e);
		}
	}
}
