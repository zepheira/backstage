package edu.mit.simile.backstage.model.ui.lens;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.data.Expression;
import edu.mit.simile.backstage.util.Utilities;

class SubcontentAttribute {
	static public class Fragment {
	}
	
	static public class StringFragment extends Fragment {
		final public String text;
		public StringFragment(String s) {
			text = s;
		}
	}
	
	static public class ExpressionFragment extends Fragment {
		final public Expression expression;
		public ExpressionFragment(Expression x) {
			expression = x;
		}
	}
	
	public String name;
	public List<Fragment> fragments = new ArrayList<Fragment>();
	
	public SubcontentAttribute(Scriptable config) {
		name = Utilities.getString(config, "name");
		if (name != null) {
			name = "<noname>";
		}
		
		Object o = config.get("fragments", config);
		if (o instanceof Scriptable) {
			Scriptable f = (Scriptable) o;
			
	        int fragmentLength = ((Number) f.get("length", f)).intValue();
	        for (int j = 0; j < fragmentLength; j++) {
	        	Object o2 = f.get(j, f);
	        	if (o2 instanceof String) {
	        		fragments.add(new StringFragment((String) o2));
	        	} else if (o2 instanceof Scriptable) {
	        		fragments.add(new ExpressionFragment(Expression.construct((Scriptable) o2)));
	        	}
	        }
		}
	}
}
