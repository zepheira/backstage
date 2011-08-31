function process(path, request, response) {
    //
    // Resource URLs;
    //
    // /data/ - POST Exhibit JSON data here. Returns a 201 pointing to ...
    // /data/<slug>/ - reference URL used in the Exhibit HTML template. POST here to add more data(TBD).
    // /exhibit-session - where configurations of lenses and facets are POSTed, returning 201 to...
    // /exhibit-session/<slug>/ - which is where the facet queries are performed
    //

    // Backstage is stateful so we need sessions to hold on to our Exhibit objects
    var session = request.getSession(true);
    
    var method = request.getMethod();
    var pathSegs = path.split("/");
    if ((pathSegs[pathSegs.length-1]).length == 0) {
        pathSegs.pop() // remove trailing empty string due to terminating "/"
    }
    if (method == "GET") {
        if (pathSegs[0] == "data") {
            if (pathSegs.length == 1) {
                // return HTML form for file upload
                butterfly.sendString(request, response, "<html><body>Upload form goes here</body></html>", "utf-8", "text/html");
                return;
            } else if (pathSegs.length == 2) {
                butterfly.sendError(request, response, 405, "Data export feature unavailable at the moment");
                return;
            } else {
                butterfly.sendError(request, response, 404, "Page not found");
                return;
            }
        } else if (pathSegs[0] == "exhibit-session") {
            if (pathSegs.length != 2) {
                butterfly.sendError(request, response, 404, "Page not found");
                return;
            }
            exhibit = backstage.getExhibit(request, pathSegs[1])
            if (exhibit == null) {
                butterfly.sendError(request, response, 404, "Exhibit not found");
                return;
            }
            // extract any facet selection state from query params. Unlike Backstage
            // restrictions which were per-facet, "restrictions" contains the state of all
            // facets. JSON for now for simplicity, but would be more transparent as a
            // regular URL query term.
            var restrictions = butterfly.parseJSON(unescape(extractQueryParamValue(request,"restr")));

            // remove some state by resetting and rebuilding restrictions with each query.
            // the performance will likely suck, but it's a start.
            var result = facetClearRestrictions(request, exhibit);

            if (restrictions) {
                var result = facetApplyRestrictions(request, restrictions, exhibit);
            }
            respond(request,response,result);
            return;
        } else {
            return false;
        }
    } else if (method == "POST") {
        if (pathSegs[0] == "data") {
            if (pathSegs.length == 1) {
                var result = uploadExhibitData(request);
                respond(request,response,result);
            } else if (pathSegs.length == 2) {
                // uploadExhibitData(), appending to existing data. TBD.
            } else {
                butterfly.sendError(request, response, 404, "Data not found");
                return;
            }
        //} else if (pathSegs[0] == "localstore") {
            // create/update the HostedDatabase
        } else if (pathSegs[0] == "exhibit-session") {
            if (pathSegs.length == 1) {
                var result = uploadExhibitConfig(request,response)
                respond(request,response,result);
            } else {
                butterfly.sendError(request, response, 500, "Unable to take action on this exhibit");
                return;
            }
        } else {
            butterfly.sendError(request, response, 404, "Page not found");
            return;
        }
    } else {
        butterfly.sendError(request, response, 501, "Unsupported method");
        return;
    }
}

function respond(request,response,result) {
    // Generic handling between controller functions and Butterfly HTTP response
    if ( "location" in result ) {
        response.setHeader("Location",result.location);
    }
    if ( "status" in result ) {
        response.setStatus(result.status);
    }

    butterfly.sendJSON(request, response, result.out);
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
            return {"status":201,"out":result,"location":"/exhibit-session/"+exhibitSlug};
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

function uploadExhibitData(request) {
    importPackage(Packages.java.io);
    importPackage(Packages.java.lang);
    importPackage(Packages.edu.mit.simile.backstage.util);
    importPackage(Packages.org.openrdf.repository.sail);
    importPackage(Packages.org.openrdf.sail.memory);

    var dataSlug = getSlug(request);
    var dbDir = System.getProperty("backstage.databaseDir","databases");
    var fullDbDir = File(dbDir,dataSlug);

    if (fullDbDir.exists()) {
        return {"status":500, "out": "The slug '"+dataSlug+"'is already in use"};
    }

    // create repo
    var sail = MemoryStore(fullDbDir);
    var repository = new SailRepository(sail);
    repository.initialize();

    // populate from request body
    var lang = DataLoadingUtilities.contentTypeToLang(request.getContentType());
    DataLoadingUtilities.loadDataFromStream( request.getInputStream(),
                                             request.getRequestURL(),
                                             lang, sail );

    // these repo objects are garbage now, but then recreated when UnhostedDatabase is
    // instantiated in the trace. Inefficient, so please FIXME

    return {"status":201,"location":"/data/"+dataSlug, "out":"Data successfully uploaded"};
}

function addDataLink(exhibit, link) {
    var url = link.url;
    if (url == "http://localhost/hosted-database") {
        //exhibit.addHostedDataLink();  // disable hosted mode for now
    } else {
        exhibit.addDataLink(link.url);
    }
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
    if ( host.toLowerCase() != "localhost" ) {
        throw "Cannot exhibit non-localhost URLs";
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

function facetApplyRestrictions(request, restrictions, exhibit) {
    importPackage(Packages.edu.mit.simile.backstage.model);
    
    var result = {};
    
    for (i in restrictions) {
        // only the last backchannel is used to determine the respons. we do it
        // this way to work with the legacy statefulness of Backstage
        var backChannel = new BackChannel();
        var facet = exhibit.getComponent(restrictions[i].facetID);
        if ( facet ) {
            facet.applyRestrictions(restrictions[i].restrictions, backChannel);
        } else {
            return {"status":400,"out":"Invalid facet id: "+restrictions[i].facetID};
        }
    }
    
    return {"status":200,"out":processBackChannel(result, backChannel)};
}

function facetClearRestrictions(request, exhibit) {
    importPackage(Packages.edu.mit.simile.backstage.model);
    
    var result = {};
    
    comps = exhibit.getAllComponents().toArray();
    for (var i=0; i<comps.length; i++ ) {
        if ( "clearRestrictions" in comps[i] ) { // facets only
            var backChannel = new BackChannel();
            comps[i].clearRestrictions(backChannel);
        }
    }
    
    return {"status":200,"out":processBackChannel(result, backChannel)};
}
