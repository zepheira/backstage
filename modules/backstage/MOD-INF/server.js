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

function getDatabase(is, params, result) {
    var database = is.getDatabase();
    if (!params._system.initialized) {
        butterfly.log("Exhibit session needs initialization");
        result._system = {
            properties: {},
            types: {}
        };
        
        var properties = database.getPropertyRecords();
        for (var i = 0; i < properties.size(); i++) {
            var p = properties.get(i);
            result._system.properties[p.id] = {
                id:         p.id,
                label:      p.label,
                uri:        p.uri,
                valueType:  p.valueType
            };
        }
        
        var types = database.getTypeRecords();
        for (var i = 0; i < types.size(); i++) {
            var t = types.get(i);
            result._system.types[t.id] = {
                id:     t.id,
                label:  t.label,
                uri:    t.uri
            };
        }
    }
    return database;
}

jsonpMethods["test"] = function(request, params) {
    return { pong: params.ping };
};

jsonpMethods["initialize-session"] = function(request, params) {
    var exhibit = backstage.createExhibit(request, params.isid);
    butterfly.log(exhibit);
    return { status: "OK" };
};

jsonpMethods["add-data-links"] = function(request, params) {
    var exhibit = backstage.getExhibit(request, params.isid);
    var links = params.links;
    for (var i = 0; i < links.length; i++) {
        var link = links[i];
        exhibit.addDataLink(
            link.url, 
            (link.mimeType != null && link.mimeType != "") ? link.mimeType : "application/json", 
            (link.charset != null && link.charset != "") ? link.charset : "utf-8"
        );
    }
    return { status: "OK" };
};

jsonpMethods["do-it"] = function(request, params) {
    var result = {};
    var exhibit = backstage.getExhibit(request, params.isid);
    var database = getDatabase(exhibit, params, result);
    return result;
};
