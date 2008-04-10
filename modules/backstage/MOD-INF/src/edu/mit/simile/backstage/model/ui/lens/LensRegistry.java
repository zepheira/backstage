package edu.mit.simile.backstage.model.ui.lens;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Scriptable;

import edu.mit.simile.backstage.model.BackChannel;

public class LensRegistry {
	protected LensRegistry _parent;
	protected Lens _defaultLens;
	protected Map<String, Lens> _typeToLens = new HashMap<String, Lens>();
	
	public LensRegistry(LensRegistry parent) {
		_parent = parent;
	}
	
    public void configure(Scriptable config, BackChannel backChannel) {
    	Object o = config.get("defaultLens", config);
    	if (o != null) {
    		_defaultLens = new TemplatedLens((Scriptable) o);
    	}
    	
    	o = config.get("typeToLens", config);
    	if (o != null) {
    		Scriptable typeToLens = (Scriptable) o;
    		
    		Object[] ids = typeToLens.getIds();
    		for (Object i : ids) {
    			String id = (String) i;
    			_typeToLens.put(id, new TemplatedLens((Scriptable) typeToLens.get(id, typeToLens)));
    		}
    	}
    }
    
    public Lens getLens(String typeId) {
    	if (_typeToLens.containsKey(typeId)) {
    		return _typeToLens.get(typeId);
    	} else if (_defaultLens != null) {
    		return _defaultLens;
    	} else if (_parent != null) {
    		return _parent.getLens(typeId);
    	} else {
    		return new DefaultLens();
    	}
    }
}
