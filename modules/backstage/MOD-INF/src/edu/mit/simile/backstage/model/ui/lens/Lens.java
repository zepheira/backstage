package edu.mit.simile.backstage.model.ui.lens;

import org.mozilla.javascript.Scriptable;
import org.openrdf.model.URI;
import org.openrdf.repository.sail.SailRepositoryConnection;

import edu.mit.simile.backstage.model.data.Database;


abstract public class Lens {
	abstract public void render(URI item, Scriptable result, Database database, SailRepositoryConnection connection);
}
