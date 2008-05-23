package edu.mit.simile.backstage.model.data;

abstract public class CacheableQuery {
	protected boolean _alreadyRun;
	protected Object  _result;
	
	public Object run() {
		if (!_alreadyRun) {
			_alreadyRun = true;
			_result = internalRun();
		}
		return _result;
	}
	
	abstract protected Object internalRun();
}
