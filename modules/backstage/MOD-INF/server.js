function process(path, request, response) {
    var method = request.getMethod();
    var session = request.getSession(true);
    var pendingCalls = session.getAttribute("pendingCalls");
    if (pendingCalls == null) {
        pendingCalls = {};
        session.setAttribute("pendingCalls", pendingCalls);
    }
    
    if (method == "GET") {
        if (path == "api/jsonpc") {
            var id = request.getParameter("id");
            var params = request.getParameter("params");
            
            var call;
            if (id in pendingCalls) {
                call = pendingCalls[id];
                call.params = call.params + params;
            } else {
                call = pendingCalls[id] = {
                    id:         id,
                    method:     request.getParameter("method"),
                    params:     "" + params,
                    callback:   request.getParameter("callback"),
                    error:      request.getParameter("error")
                }
            }
            
            var complete = request.getParameter("complete") == "true";
            if (complete) {
                delete pendingCalls[id];
                processJsonpCall(request, response, call);
            } else {
                butterfly.sendJSONP(request, response, { status: "OK" }, call.callback);
            }
        }
    } else if (method == "POST") {
    }
}

function processJsonpCall(request, response, call) {
    butterfly.log(call.id + " payload: \"" + call.params + "\"");
    try {
        var params = butterfly.parseJSON(call.params);
        if (call.method in jsonpMethods) {
            var result = jsonpMethods[call.method](request, params);
            butterfly.sendJSONP(request, response, result, call.callback);
        } else {
            sendError(request, response, 404, "JSONP Method " + call.method + " Not Found", call.error);
        }
    } catch (e) {
        sendError(request, response, 500, "Internal Server Error: " + e, call.error);
    }
}

function sendError(request, response, code, message, callback) {
    butterfly.sendJSONP(request, response, { code: code, message: message }, callback);
}

var jsonpMethods = {};

jsonpMethods["test"] = function(request, params) {
    return { pong: params.ping };
};

jsonpMethods["initialize-session"] = function(request, params) {
    backstage.createInteractiveSession(request, params.isid);
    return { status: "OK" };
};

jsonpMethods["add-data-link"] = function(request, params) {
    var is = backstage.getInteractiveSession(request, params.isid);
    butterfly.log(is.toString());
    butterfly.log(is.doIt("a"));
    is.addDataLink(
        params.url, 
        ("mimeType" in params) ? params.mimeType : "application/json", 
        ("charset" in params) ? params.charset : "utf-8"
    );
    return { status: "OK" };
};

