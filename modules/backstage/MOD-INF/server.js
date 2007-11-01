var pendingCalls = {};

function process(path, request, response) {
    var method = request.getMethod();
    
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
                butterfly.sendJSONP(request, response, "OK", call.callback);
            }
        }
    } else if (method == "POST") {
    }
}

function processJsonpCall(request, response, call) {
    try {
        var params = butterfly.parseJSON(call.params);
        if (call.method in jsonpMethods) {
            var result = jsonpMethods[call.method](params);
            butterfly.sendJSONP(request, response, result, call.callback);
        } else {
            butterfly.sendJSONP(request, response, 
                {   code:    404, 
                    message: "JSONP Method " + call.method + " Not Found"
                }, 
                call.error
            );
        }
    } catch (e) {
        butterfly.sendJSONP(request, response, 
            {   code:    500, 
                message: "Internal Server Error: " + e
            }, 
            call.error
        );
    }
}

var jsonpMethods = {};

jsonpMethods["hello"] = function(params) {
    return { result: "world" };
};