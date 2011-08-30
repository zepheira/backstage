/*==================================================
 *  Backstage.JobQueue
 *==================================================
 */

Backstage.JobQueue = function() {
    this._pendingJobs = [];
    this._currentJob = null;
    this._timerID = null;
};

Backstage.JobQueue.prototype.clear = function() {
    this._pendingJobs = [];
};

Backstage.JobQueue.prototype.queue = function(job) {
    this._pendingJobs.push(job);
    if (this._currentJob == null) {
        this._startJob();
    }
};

Backstage.JobQueue.prototype._startJob = function() {
    this._currentJob = this._pendingJobs.shift();
    this._setTimeout();
};

Backstage.JobQueue.prototype._setTimeout = function() {
    var self = this;
    this._timerID = window.setTimeout(
        function() { self._onTimeout(); },
        100
    );
};

Backstage.JobQueue.prototype._onTimeout = function() {
    this._timerID = null;
    
    var done = true;
    try {
        done = this._currentJob.run();
    } catch (e) {
        Exhibit.Debug.log(e);
    }
    
    if (done) {
        this._currentJob = null;
        if (this._pendingJobs.length > 0) {
            this._startJob();
        }
    } else {
        this._setTimeout();
    }
};
