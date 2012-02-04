importPackage(Packages.java.lang);
//  determine and /-terminate server mount URI
var SERVER_ROOT = String(System.getProperty("backstage.serverRoot","http://localhost:8181/backstage/"));
if (SERVER_ROOT[SERVER_ROOT.length-1] != "/") {
    SERVER_ROOT += "/";
}
var DATA_SEG = "data";
var DATA_URL_ROOT = SERVER_ROOT+DATA_SEG+"/";

function process(path, request, response) {
    //
    // Resource URLs;
    //
    // /data/{disk,mem}/ - POST Exhibit JSON data here. Returns a 201 pointing to ...
    // /data/{disk,mem}/<data-slug>/ - reference URL used in the Exhibit HTML template. POST here to add more data(TBD).
    // /exhibit-session - where configurations of lenses and facets are POSTed, returning 201 to...
    // /exhibit-session/<sess-slug>/ - which is where the facet queries are performed
    // /exhibit-session/<sess-slug>/component/<compid> - components of the exhibit, where you can POST state
    // /exhibit-session/<sess-slug>/lens-cache - query cache for this data. DELETE it when you modify an exhibit
    //

    // Backstage is stateful so we need sessions to hold on to our Exhibit objects
    var session = request.getSession(true);

    var method = request.getMethod();
    var pathSegs = path.split("/");
    if ((pathSegs[pathSegs.length-1]).length == 0) {
        pathSegs.pop() // remove trailing empty string due to terminating "/"
    }
    if (method == "GET") {
        if (pathSegs[0] == DATA_SEG) {
            if (pathSegs.length == 2) {
                // return HTML form for file upload
                CORSify(request,response);
                butterfly.sendString(request, response, "<html><body>Upload form goes here</body></html>", "utf-8", "text/html");
                return;
            } else if (pathSegs.length == 3) {
                importPackage(edu.mit.simile.backstage.util);
                var db = backstage.getDatabase(DATA_URL_ROOT+pathSegs.slice(1).join("/"));
                if (db == null) {
                    CORSify(request,response);
                    butterfly.sendError(request, response, 404, "Data not found");
                    return;
                }
              
                var limit = extractQueryParamValue(request,"limit");
                if (!limit) limit = 20;
                var result = db.exportRDFa(limit,pathSegs[2]);

                respond(request,response,{"contentType":"text/html",
                                          "Cache-Control":"max-age="+String(86400*365),
                                          "status":200,
                                          "out":String(result)});
                return;
            } else {
                CORSify(request,response);
                butterfly.sendError(request, response, 404, "Page not found");
                return;
            }
        } else if (pathSegs[0] == "exhibit-session") {
            // tried to implement a scissor-UI like session snapshot export here, but
            // impractical
            CORSify(request,response);
            butterfly.sendError(request, response, 403, "Page not found");

            CORSify(request,response,exhibit);
            respond(request,response,result);
            return;
        } else {
            return false;
        }
    } else if (method == "POST") {
        if (pathSegs[0] == DATA_SEG) {
            if (pathSegs.length == 2) {
                // next path segment is repository type
                var repoType = pathSegs[1];
                if (repoType != "mem" && repoType != "disk") {
                    CORSify(request,response);
                    butterfly.sendError(request, response, 404, "Data not found");
                    return;
                }

                importPackage(Packages.java.io);
                importPackage(Packages.java.lang);

                var dataSlug = getSlug(request);
                var dbRootDir = System.getProperty("backstage.databaseDir","databases");

                var fullDbDir = File(File(dbRootDir,repoType),dataSlug);
                if (fullDbDir.exists()) {
                    respond(request,response,{"status":500, "out": "The slug '"+dataSlug+"'is already in use"});
                }

                // verify repo type directory exists, else make it
                var repoTypeDir = File(dbRootDir,repoType);
                if (!repoTypeDir.exists()) {
                    // can't do much with exceptions, so punt to user via 500 response
                    if (repoTypeDir.mkdir()) {
                        // pass
                    } else {
                        respond(request,response,{"status":500, "out": "Unable to create database directory under "+dbRootDir});
                    }
                }

                var result = uploadExhibitData(request,repoType,dataSlug);
                respond(request,response,result);
                return;
            } else if (pathSegs.length == 3) {
                // uploadExhibitData(), appending to existing data. TBD.
            } else {
                CORSify(request,response);
                butterfly.sendError(request, response, 404, "Data not found");
                return;
            }
        } else if (pathSegs[0] == "exhibit-session") {
            if (pathSegs.length == 1) {
                var result = uploadExhibitConfig(request,response)
                CORSify(request,response);
                respond(request,response,result);
                return;
            } else {
                CORSify(request,response);
                butterfly.sendError(request, response, 500, "Unable to take action on this exhibit");
                return;
            }
        } else {
            CORSify(request,response);
            butterfly.sendError(request, response, 404, "Page not found");
            return;
        }
    } else if (method == "PUT" ) {
        if (pathSegs[0] == "exhibit-session") {
            if (pathSegs.length == 4 && pathSegs[2]=="component") {
                var compId = pathSegs[3];
                var exhibit = backstage.getExhibit(request, pathSegs[1])
                if (exhibit == null) {
                    CORSify(request,response);
                    butterfly.sendError(request, response, 404, "Exhibit session not found");
                    return;
                }

                var comp = exhibit.getComponent(compId);
                if (comp == null) {
                    CORSify(request,response);
                    butterfly.sendError(request, response, 404, "Component not found");
                    return;
                }

                var restr = readBodyAsJSON(request);
                var result;
                if ("restrictions" in restr) {
                    result = facetApplyRestrictions(comp,restr.restrictions);
                } else {
                    result = facetClearRestrictions(comp);
                }

                CORSify(request,response);
                respond(request,response,result);
                return;
            }
        }
        return;
    } else if (method == "OPTIONS") {
        CORSify(request,response);
        butterfly.sendString(request, response, "", "utf-8", "text/plain");
        return;
    } else if (method == "DELETE") {
        // flushes the lens cache for the database associated with the given session.
        // no UI is provided so don't forget the session cookie
        if (pathSegs[0] == "exhibit-session") {
            if (pathSegs.length >= 2) {
                var exhibit = backstage.getExhibit(request, pathSegs[1])
                if (exhibit == null) {
                    butterfly.sendError(request, response, 404, "Exhibit session not found");
                    return;
                }

                if (pathSegs[2] == "lens-cache") {
                  exhibit.getDatabase().discardQueryCache()
                  butterfly.sendString(request, response, "Lens cache cleared", "utf-8", "text/plain");
                  return;
                }

                butterfly.sendError(request, response, 404, "Page not found");
                return;
            }
        }
        CORSify(request,response);
        butterfly.sendString(request, response, "", "utf-8", "text/plain");
        return;
    } else {
        butterfly.sendError(request, response, 501, "Unsupported method");
        return;
    }
}

function CORSify(request,response,exhibit) {
    // wide open
    var origin = request.getHeader("Origin");
    response.setHeader("Access-Control-Allow-Origin", origin);
    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Methods","GET, PUT, POST, OPTIONS");
    response.setHeader("Access-Control-Expose-Headers","Location");
}

function respond(request,response,result) {
    // Generic handling between controller functions and Butterfly HTTP response
    var stat = 200;
    var responseText = "";
    var contentType = "text/plain";
    var encoding = "utf-8";
    if ( "status" in result ) {
        stat = result.status;
        delete result.status;
    }
    if ( "out" in result ) {
        responseText = result.out;
        delete result.out;
    }
    if ( "contentType" in result ) {
        contentType = result.contentType;
        delete result.contentType;
    }
    if ( "encoding" in result ) {
        encoding = result.encoding;
        delete result.encoding;
    }

    // copy other properties into HTTP response headers
    for (var header in result) {
        if (result.hasOwnProperty(header)) response.setHeader(header,result[header]);
    }

    response.setStatus(stat);
    responseText = butterfly.toJSONString(responseText);

    butterfly.sendString(request, response, responseText, encoding, contentType);
}

function getSlug(request) {
    // Can't only use getParameter since we need to extract the query param for both GET
    // and POST requests.  We also use Atom's Slug header, falling back to a UUID-like
    // string when no slug-hint is provided
    var slug = ""
    if ( request.getMethod().toUpperCase() == "GET" ) {
        slug = request.getParameter("slug");
    } else {
        slug = extractQueryParamValue(request,"slug");
    }
    if (slug == null) {
        slug = request.getHeader("slug");
    }
    if (slug == null) {
        slug = randomString();
    }
    return encodeURIComponent(slug);
}

function uploadExhibitConfig(request,response) {
    // Configure exhibit from lens and facet descriptions
    var exhibitSlug = randomString();
    var exhibit = backstage.getExhibit(request, exhibitSlug);
    var params = readBodyAsJSON(request);
    if (exhibit == null) {
        exhibit = backstage.createExhibit(request, params.refererUrlSHA1, exhibitSlug);
        var result;
        try {
            result = configureExhibit(request,params,exhibit);
        var result;
            var location = "/exhibit-session/"+exhibitSlug;
            result.location = location; // stick in body since Chrome can't expose the header
            return {"status":201,"out":result,"location":location};
        } catch(errtext) {
            return {"status":500,"out":errtext};
        }
    } else {
        return {"status":500,"out": "An exhibit by that name already exists"};
    }
}

function parseQueryParams(request) {
    var queryParams = new Array();
    var qs = request.getQueryString();
    if ( qs == null ) return queryParams;

    var qss = qs.split("&");
    for (var i=0; i<qss.length; i++) {
        var p = qss[i];
        nv = p.split("=");
        var o = {}
        if (nv.length == 2) {
            o[nv[0]] = nv[1];
            queryParams.push(o);
        } else {
            o[nv[0]] = null;
            queryParams.push(o);
        }
    }
    return queryParams;
}

function extractQueryParamValue(request,param) {
    qp = parseQueryParams(request);
    var paramValue = null;
    for (var i=0; i<qp.length; i++) {
        var nv = qp[i];
        var n = v = null;
        for (tmp in nv) { // one property per object
            n = tmp;
            v = nv[tmp];
        }
        if (n.indexOf(param)>-1) {
            paramValue = v;
            break;
        }
    }
    return paramValue;
}

function randomString() {
   // generates slugs for anonymous data
   return (((1+Math.random())*0x1000000000000)|0).toString(16).substring(1);
}

function readBodyAsJSON(request) {
    reader = request.getReader();
    line = reader.readLine();
    var json = "";
    while (line != null ) {
        json += line;
        line = reader.readLine();
    }
    return butterfly.parseJSON(json);
}

function uploadExhibitData(request,repoType,dataSlug) {
    importPackage(Packages.edu.mit.simile.backstage.util);
    importPackage(Packages.java.io);

    // create repo then assign to database object, as we assume
    // that the repository from an uploaded dataset will be used
    // soon. if not, it will be collected when Butterfly's
    // ServletContext dies
    var repo = null;
    try {
        repo = backstage.createRepository(request,repoType,dataSlug);
    } catch (e) {
        return {"status":500,"out":"Problem creating repository: "+e};
    }

    var dbUrl = DATA_URL_ROOT+repoType+File.separator+dataSlug;
    var db = backstage.getDatabase(dbUrl);
    db.setRepository(repo);

    return {"status":201,"location":dbUrl, "out":"Data successfully uploaded"};
}

function addDataLink(exhibit, link) {
    // the old "hosted" mode, now known as standalone mode, has been disabled

    //if (link.url == SERVER_ROOT+"hosted-database") {
        //exhibit.addHostedDataLink();  // disable hosted mode for now
    //} else {
        exhibit.addDataLink(link.url);
    //}
}

function processBackChannel(result, backChannel) {
    if (backChannel.hasComponentsChangingState()) {
        result._componentStates = backChannel.getComponentStateArray();
    }
    if (backChannel.hasComponentUpdates()) {
        result._componentUpdates = backChannel.getComponentUpdateArray();
    }
    return result;
}

function getDatabase(exhibit, params, result) {
    var database = exhibit.getDatabase();
    if (!params._system.initialized) {
        butterfly.log("Exhibit database needs initialization");
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

function configureExhibit(request, params, exhibit) {
    var result = {};
    var configuration = params.configuration;
    
    importPackage(Packages.edu.mit.simile.backstage.model);
    importPackage(Packages.edu.mit.simile.backstage.model.data);
    importPackage(Packages.edu.mit.simile.backstage.model.ui.views);
    importPackage(Packages.edu.mit.simile.backstage.model.ui.facets);

    var match = configuration.link.url.match(/http:\/\/(\S+?)[\/:]/);
    if ( match == null || match.length < 2 ) {
        throw "Invalid URL";
    }

    var host = match[1];
    var sr_host = SERVER_ROOT.match(/http:\/\/(\S+?)[\/:]/)[1];
    if ( host.toLowerCase() != sr_host.toLowerCase()) {
        throw "Can only exhibit data URLs under "+sr_host;
    }
    addDataLink(exhibit, configuration.link);

    // this initializes the database's client side information if it's not already initialized.
    var database = getDatabase(exhibit, params, result); 
    
    var backChannel = new BackChannel();

    var collections = configuration.collections;
    for (var i = 0; i < collections.length; i++) {
        var c = collections[i];
        var collection;
        
        switch (c.type) {
        case "types-based":
            collection = new TypeBasedCollection(exhibit, c.id);
            break;
        case "based":
            collection = new BasedCollection(exhibit, c.id);
            break;
        default:
            collection = new AllItemsCollection(exhibit, c.id);
            break;
        }
        
        exhibit.setCollection(c.id, collection);
        collection.configure(c, backChannel);
    }
    
    var context = exhibit.getContext();
    context.configure(configuration.uiContext, backChannel);
    
    var components = configuration.components;
    for (var i = 0; i < components.length; i++) {
        var c = components[i];
        var component;
        
        switch (c.role) {
        case "view":
            switch (c.viewClass) {
            default:
                component = new TileView(context, c.id);
                break;
            }
            break;
        case "facet":
            switch (c.facetClass) {
            default:
                component = new ListFacet(context, c.id);
                break;
            }
            break;
        }
        
        exhibit.setComponent(c.id, component);
        component.configure(c, backChannel);
    }
    
    return processBackChannel(result, backChannel);
}

function facetApplyRestrictions(facet,restrictions) {
    importPackage(Packages.edu.mit.simile.backstage.model);

    var result = {};
    var backChannel = new BackChannel();
    facet.applyRestrictions(restrictions, backChannel);

    return {"status":200,"out":processBackChannel(result, backChannel)};
}

function facetClearRestrictions(facet) {
    importPackage(Packages.edu.mit.simile.backstage.model);

    var result = {};
    var backChannel = new BackChannel();

    facet.clearRestrictions(backChannel);

    return {"status":200,"out":processBackChannel(result, backChannel)};
};
