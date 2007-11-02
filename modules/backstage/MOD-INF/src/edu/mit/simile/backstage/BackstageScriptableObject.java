package edu.mit.simile.backstage;

import edu.mit.simile.butterfly.ButterflyScriptableObject;

abstract public class BackstageScriptableObject extends ButterflyScriptableObject {
    public BackstageModule getModule() {
        return (BackstageModule) _module;
    }
}
