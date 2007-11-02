/*======================================================================
 *  Backstage
 *  http://simile.mit.edu/wiki/Backstage/API/Backstage
 *======================================================================
 */
 
Backstage.create = function() {
    return new Backstage._Impl();
};

/*==================================================
 *  Backstage._Impl
 *==================================================
 */
Backstage._Impl = function() {
    this._jsonpTransport = new Backstage.JsonpTransport(Backstage.urlPrefix + "jsonpc");
    this._jobQueue = new Backstage.JobQueue();
};

Backstage._Impl.prototype.dispose = function() {
};

Backstage._Impl.prototype.asyncCall = function(method, params, onSuccess, onError) {
    this._jsonpTransport.asyncCall(method, params, onSuccess, onError);
};

Backstage._Impl.prototype.clearAsyncCalls = function() {
    this._jsonpTransport.clear();
};

Backstage._Impl.prototype.queueJob = function(job) {
    this._jobQueue.queue(job);
};

Backstage._Impl.prototype.clearJobs = function() {
    this._jobQueue.clear();
};
