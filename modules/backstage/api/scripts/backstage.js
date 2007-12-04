/*======================================================================
 *  Backstage
 *  http://simile.mit.edu/wiki/Backstage/API/Backstage
 *======================================================================
 */
 
Backstage.create = function(cont) {
    return new Backstage._Impl(cont);
};

/*==================================================
 *  Backstage._Impl
 *==================================================
 */
Backstage._Impl = function(cont) {
    // interactive session id
    this._isid = "is" + Math.floor(1000000 * Math.random());
    this._initialized = false;
    
    this._jsonpTransport = new Backstage.JsonpTransport(Backstage.urlPrefix + "jsonpc");
    this._jobQueue = new Backstage.JobQueue();
    
    this._dataLinks = [];
    this._initialize(cont);
};

Backstage._Impl.prototype.dispose = function() {
};

/*
 *  All async calls should go through this method, which is responsible for
 *  reconstructing the server's state should the connection got closed, and
 *  for handling system data that piggybacks on normal calls.
 */
Backstage._Impl.prototype.asyncCall = function(method, params, onSuccess, onError) {
    // add the interactive session id
    params.isid = this._isid;
    
    // flag to cause initialization data to flow back in case we're not initialized
    params._system = { initialized: this._initialized };
    
    var self = this;
    var f = function() {
        self._jsonpTransport.asyncCall(
            method, 
            params, 
            function(o) {
                if ("_system" in o) {
                    // process system data that piggybacks on normal calls
                    self._processSystemData(o._system);
                }
                if (typeof onSuccess == "function") {
                    onSuccess(o);
                }
            },
            function(e) {
                if (e.code == 410) { // 410:Gone
                    self._reinitialize(f);
                } else if (onError != undefined) {
                    onError(e);
                } else {
                    SimileAjax.Debug.log(e);
                }
            }
        );
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

Backstage._Impl.prototype.loadDataLinks = function(onSuccess, onError) {
    var links = [];
    var heads = document.documentElement.getElementsByTagName("head");
    for (var h = 0; h < heads.length; h++) {
        var linkElmts = heads[h].getElementsByTagName("link");
        for (var l = 0; l < linkElmts.length; l++) {
            var link = linkElmts[l];
            if (link.rel.match(/\bexhibit\/data\b/)) {
                links.push({
                    url:        link.href,
                    mimeType:   link.type,
                    charset:    link.charset
                });
            }
        }
    }
    
    this._dataLinks = this._dataLinks.concat(links);
    this._internalAddDataLinks(links, onSuccess, onError);
};

Backstage._Impl.prototype._initialize = function(onSuccess, onError) {
    this._jsonpTransport.asyncCall(
        "initialize-session", 
        { isid: this._isid }, 
        function(o) { 
            console.log(o);
            if (typeof onSuccess == "function") {
                onSuccess();
            }
        },
        onError
    );
};

Backstage._Impl.prototype._reinitialize = function(onSuccess) {
    this._initialized = false;
    
    var onError = function(e) {
        /*
         *  We couldn't reconstruct the server's state from the client's state. This is really bad.
         */
        alert("We're sorry: this session has been inactive for too long and cannot be continued.\n" +
              "Please refresh the page to start a new session.");
    };
    
    var self = this;
    this._initialize(
        function() {
            self._internalAddDataLinks(self._dataLinks, onSuccess, onError);
        }, 
        onError
    );
};

Backstage._Impl.prototype._internalAddDataLinks = function(links, onSuccess, onError) {
    this._jsonpTransport.asyncCall(
        "add-data-links", 
        { isid: this._isid, links: links }, 
        function(o) { 
            onSuccess();
        },
        onError
    );
};

Backstage._Impl.prototype._processSystemData = function(o) {
    
};
