/*==================================================
 *  Backstage.JsonpTransport
 *==================================================
 */

Backstage.JsonpTransport = function(entryURL) {
    this._entryURL = entryURL;
    this._pendingCalls = [];
    this._currentCall = null;
    
    var id = Math.floor(1000000 * Math.random());
    
    this._successCallback = "__success" + id;
    this._errorCallback = "__error" + id;
    
    var self = this;
    window[this._successCallback] = function(o) {
        self._onSuccessCallback(o);
    };
    window[this._errorCallback] = function(e) {
        self._onErrorCallback(e);
    };
};

Backstage.JsonpTransport.payloadLimit = 256;
Backstage.JsonpTransport.removeScripts = true;

Backstage.JsonpTransport.prototype.clear = function() {
    this._pendingCalls = [];
};

Backstage.JsonpTransport.prototype.asyncCall = function(method, params, onSuccess, onError) {
    var call = {
        method:         method,
        paramsAsString: Backstage.JSON.toJSONString(params),
        onSuccess:      (onSuccess == undefined) ? function() {} : onSuccess,
        onError:        (onError == undefined) ? function(e) { SimileAjax.Debug.log(e); } : onError,
        id:             "call-" + new Date().getTime() + "-" + Math.floor(1000 * Math.random()),
        complete:       false
    };
    this._pendingCalls.push(call);
    
    if (this._currentCall == null) {
        this._makeNextCall();
    }
};

Backstage.JsonpTransport.prototype._makeNextCall = function() {
    if (this._pendingCalls.length > 0) {
        this._currentCall = this._pendingCalls.shift();
        this._processCurrentCall();
    }
};

Backstage.JsonpTransport.prototype._processCurrentCall = function() {
    var call = this._currentCall;
    
    var stringToSend;
    var limit = Backstage.JsonpTransport.payloadLimit;
    if (call.paramsAsString.length > limit) {
        stringToSend = call.paramsAsString.substr(0, limit);
        call.paramsAsString = call.paramsAsString.substr(limit);
    } else {
        stringToSend = call.paramsAsString;
        call.complete = true;
    }
    
    var script = document.createElement("script");
    var errorCallback = this._errorCallback + "();";
    try { script.innerHTML = errorCallback; } catch(e) {}
    script.setAttribute("onerror", errorCallback);
    script.type = "text/javascript";
    script.language = "JavaScript";
    script.id = call.id;
    script.src = this._entryURL + "?" + [
        "id=" + encodeURIComponent(call.id),
        "method=" + encodeURIComponent(call.method),
        "params=" + encodeURIComponent(stringToSend),
        "complete=" + call.complete,
        "callback=" + this._successCallback,
        "error=" + this._errorCallback
    ].join("&");
    
    document.getElementsByTagName("head")[0].appendChild(script);
};

Backstage.JsonpTransport.prototype._onSuccessCallback = function(o) {
    this._removeCurrentCallScript();
    
    if (this._currentCall.complete) {
        try {
            this._currentCall.onSuccess(o);
        } catch (e) {
            SimileAjax.Debug.log(e);
        }
        this._currentCall = null;
    } else {
        this._processCurrentCall();
    }
    this._makeNextCall();
};

Backstage.JsonpTransport.prototype._onErrorCallback = function(e) {
    this._removeCurrentCallScript();
    
    e = (e == undefined) ? { code: 500, message: "Unknown error" } : e;
    
    SimileAjax.Debug.log("error calling " + this._currentCall.method + ": " + e.code + " " + e.message); 
    try {
        this._currentCall.onError(e);
    } catch (e2) {
        SimileAjax.Debug.log(e2);
    }
    this._currentCall = null;
    this._makeNextCall();
};

Backstage.JsonpTransport.prototype._removeCurrentCallScript = function() {
    if (Backstage.JsonpTransport.removeScripts) {
        var script = document.getElementById(this._currentCall.id);
        if (script) {
            script.parentNode.removeChild(script);
        }
    }
};