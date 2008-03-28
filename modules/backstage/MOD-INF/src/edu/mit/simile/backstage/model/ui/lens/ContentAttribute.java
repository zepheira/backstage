/**
 * 
 */
package edu.mit.simile.backstage.model.ui.lens;

import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.data.Expression;
import edu.mit.simile.backstage.util.Utilities;

class ContentAttribute {
	public String name;
	public Expression expression;
	
	public ContentAttribute(Scriptable config) {
		name = Utilities.getString(config, "name");
		expression = Utilities.getExpression(config, "expression");
	}
}