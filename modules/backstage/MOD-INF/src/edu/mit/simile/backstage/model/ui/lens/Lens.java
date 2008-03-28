package edu.mit.simile.backstage.model.ui.lens;


import java.util.ArrayList;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.URI;
import org.openrdf.repository.sail.SailRepositoryConnection;

import edu.mit.simile.backstage.model.data.Database;
import edu.mit.simile.backstage.model.data.Expression;
import edu.mit.simile.backstage.util.Utilities;


public class Lens {
	protected DomLensNode _rootNode;
	
	public Lens(Scriptable configNode) {
		_rootNode = createLensNode(configNode);
	}
	
	static protected DomLensNode createLensNode(Scriptable configNode) {
		DomLensNode lensNode = new DomLensNode();
		
		lensNode._contentExpression = Utilities.getExpression(configNode, "content");
		
		Object o = configNode.get("control", configNode);
		if (o instanceof String) {
			lensNode._control = (String) o;
		}
		
		o = configNode.get("condition", configNode);
		if (o instanceof Scriptable) {
			Scriptable condition = (Scriptable) o;
			
			String test = Utilities.getString(condition, "test");
			Expression expression = Utilities.getExpression(condition, "expression");
			
			if (test != null && expression != null) {
				lensNode._conditionTest = test;
				lensNode._conditionExpression = expression;
			}
		}
		
		o = configNode.get("contentAttributes", configNode);
		if (o instanceof Scriptable) {
			lensNode._contentAttributes = new ArrayList<ContentAttribute>();
			
			Scriptable contentAttributes = (Scriptable) o;
			
	        int length = ((Number) contentAttributes.get("length", contentAttributes)).intValue();
	        for (int i = 0; i < length; i++) {
	        	Object o2 = contentAttributes.get(i, contentAttributes);
	        	if (o2 instanceof Scriptable) {
	        		lensNode._contentAttributes.add(new ContentAttribute((Scriptable) o2));
	        	}
	        }
		}
		
		o = configNode.get("subcontentAttributes", configNode);
		if (o instanceof Scriptable) {
			lensNode._subcontentAttributes = new ArrayList<SubcontentAttribute>();
			
			Scriptable subcontentAttributes = (Scriptable) o;
			
	        int length = ((Number) subcontentAttributes.get("length", subcontentAttributes)).intValue();
	        for (int i = 0; i < length; i++) {
	        	Object o2 = subcontentAttributes.get(i, subcontentAttributes);
	        	if (o2 instanceof Scriptable) {
	        		lensNode._subcontentAttributes.add(new SubcontentAttribute((Scriptable) o2));
	        	}
	        }
		}
		
		o = configNode.get("children", configNode);
		if (o instanceof Scriptable) {
			lensNode._children = new ArrayList<LensNode>();
			
			Scriptable children = (Scriptable) o;
			
	        int length = ((Number) children.get("length", children)).intValue();
	        for (int i = 0; i < length; i++) {
	        	Object o2 = children.get(i, children);
	        	if (o2 instanceof Scriptable) {
	        		lensNode._children.add(createLensNode((Scriptable) o2));
	        	} else if (o2 instanceof String) {
	        		lensNode._children.add(createLensNode((String) o2));
	        	}
	        }
		}
		
		return lensNode;
	}
	
	static protected LensNode createLensNode(String s) {
		return new StringLensNode(s);
	}
	
	public void render(URI item, Scriptable result, Database database, SailRepositoryConnection connection) {
        _rootNode.render(item, result, database, connection);
	}
}
