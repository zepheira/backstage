/**
 * 
 */
package edu.mit.simile.backstage.util;

import com.whirlycott.cache.Cacheable;

public class CompositeKey implements Cacheable {
    final private Object _a;

    final private Object _b;

    final private Object _c;

    public CompositeKey(Object a) {
        this._a = a;
        this._b = null;
        this._c = null;
    }

    public CompositeKey(Object a, Object b) {
        this._a = a;
        this._b = b;
        this._c = null;
    }

    public CompositeKey(Object a, Object b, Object c) {
        this._a = a;
        this._b = b;
        this._c = c;
    }

    final public int hashCode() {
        int hash = this._a.hashCode();
        if (this._b != null) hash ^= this._b.hashCode();
        if (this._c != null) hash ^= this._c.hashCode();
        return hash;
    }

    final public boolean equals(Object o) {
        return this.hashCode() == o.hashCode();
    }

    final public void onRetrieve(Object o) {
        // do nothing
    }

    final public void onStore(Object o) {
        // do nothing
    }

    final public void onRemove(Object o) {
        // do nothing
    }
}