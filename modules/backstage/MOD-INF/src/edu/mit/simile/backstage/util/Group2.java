package edu.mit.simile.backstage.util;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.query.algebra.Group;
import org.openrdf.query.algebra.TupleExpr;

/*
 * 	This is a temporary fix to Sesame 2 rc2's org.openrdf.query.algebra.Group. In Group,
 *  the getGroupBindingNames method is supposed to return an unmodifiable set, but it
 *  somehow gets cleared when the query is evaluated.
 */
public class Group2 extends Group {
	public Group2(TupleExpr t) {
		super(t);
	}

	public Set<String> getGroupBindingNames() {
		return new HashSet<String>(super.getGroupBindingNames());
	}
}
