package edu.mit.simile.backstage;

import edu.mit.simile.butterfly.ButterflyScriptableObject;

abstract public class BackstageScriptableObject extends ButterflyScriptableObject {
	private static final long serialVersionUID = 4338094664119493629L;

	public BackstageModule getModule() {
        return (BackstageModule) _module;
    }
}
