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
    var self = this;
    var f = function() {
        self._jsonpTransport.asyncCall(method, params, onSuccess, function(e) {
            if (e.code == 410) { // 410:Gone
                self._reconstructServerState(f);
            } else {
                onError(e);
            }
        });
    };
    f();
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

Backstage._Impl.prototype._reconstructServerState = function(cont) {
    var params = {}; // TODO: need to populate params
    this._jsonpTransport.asyncCall(
        "reconstruct", 
        params, 
        function(o) { 
            // TODO: what to do with o?
            cont();
        }, 
        function(e) {
            /*
             *  We couldn't reconstruct the server's state from the client's state. This is really bad.
             */
            alert("This session has been inactive for too long and it cannot be continued.\nPlease refresh the page to start a new session.");
        }
    );
};