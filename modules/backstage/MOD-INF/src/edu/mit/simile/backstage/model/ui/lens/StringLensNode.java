/**
 * 
 */
package edu.mit.simile.backstage.model.ui.lens;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.URI;
import org.openrdf.repository.sail.SailRepositoryConnection;

import edu.mit.simile.backstage.model.data.Database;

class StringLensNode extends LensNode {
	final protected String text;
	
	public StringLensNode(String t) {
		text = t;
	}
	
	public void render(URI item, Scriptable result, Database database, SailRepositoryConnection connection) {
		// Don't have to do anything since this is just a static string
	}
}