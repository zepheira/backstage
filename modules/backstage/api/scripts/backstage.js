/**
 * @fileOverview Base Backstage implementation.  Adds window.backstage
 *     as the interaction point.
 * @author David Huynh
 * @author <a href="mailto:ryanlee@zepheira.com">Ryan Lee</a>
 */
 
/**
 * @param {Function} cont
 * @returns {Backstage._Impl}
 */
Backstage.create = function(cont) {
    return new Backstage._Impl(cont);
};

/**
 * @class
 * @constructor
 * @param {Function} cont
 */
Backstage._Impl = function(cont) {
    this._initialized = false;

    this._exhibitSession = null;
    
    this._jobQueue = new Backstage.JobQueue();
    
    this._dataLinks = [];
    
    this._properties = {};
    this._types = {};
    
    this._uiContext = Backstage.UIContext.createRootContext({}, this);
    this._domConfiguration = {
        refererUrlSHA1: Backstage.SHA1.hex_sha1(document.location.href),
        link:           {},
        role:           "exhibit",
        uiContext:      this._uiContext.getServerSideConfiguration(),
        collections:    [],
        components:     []
    };
    this._collectionMap = {};
    this._registry = new Exhibit.Registry();
    $(document).trigger("registerComponents.exhibit", this._registry);

    /*
     *  We use window.setTimeout because otherwise, on Opera 9, cont gets
     *  called before this constructor returns. This means that the
     *  Backstage object hasn't been assigned to some variable in the caller
     *  and cont won't be able to retrieve it.
     */
    window.setTimeout(cont, 0);
};

/**
 *
 */
Backstage._Impl.prototype.dispose = function() {
    var id;

    for (id in this._collectionMap) {
        if (this._collectionMap.hasOwnProperty(id)) {
            if (typeof this._collectionMap[id].dispose === "function") {
                try {
                    this._collectionMap[id].dispose();
                } catch (ex) {
                    Exhibit.Debug.exception(ex, "Failed to dispose of collection.");
                }
            }
        }
    }

    this._uiContext.dispose();
    this._uiContext = null;
    this._registry = null;
    this._domConfiguration = null;
    this._types = null;
    this._propeties = null;
    this._dataLinks = null;
    this._jobQueue = null;
    this._exhibitSession = null;
};

/**
 * @param {String} id
 * @returns {Backstage.Collection}
 */
Backstage._Impl.prototype.getCollection = function(id) {
    var collection = this._collectionMap[id];
    if ((typeof collection === "undefined" || collection === null) && id === "default") {
        collection = Backstage.Collection.createAllItemsCollection(id, this);
        
        this._collectionMap[id] = collection;
        this._domConfiguration.collections.push(collection.getServerSideConfiguration());
    }
    return collection;
};

/**
 * @returns {Backstage.Collection}
 */
Backstage._Impl.prototype.getDefaultCollection = function() {
    return this.getCollection("default");
};

/**
 * @returns {Exhibit.Registry}
 */
Backstage._Impl.prototype.getRegistry = function() {
    return this._registry;
};

/**
 * All async calls should go through this method, which is responsible for
 * reconstructing the server's state should the connection got closed, and
 * for handling system data that piggybacks on normal calls.
 *
 * @param {String} method
 * @param {String} url
 * @param {Object} params
 * @param {Function} onSuccess
 * @param {Function} onError
 */
Backstage._Impl.prototype.asyncCall = function(method, url, params, onSuccess, onError) {
    // flag to cause initialization data to flow back in case we're not initialized
    params._system = { initialized: this._initialized };
    
    var self, f;
    self = this;
    f = function() {
        $.ajax({
            "url": url,
            "type": method,
            "crossDomain": true,
            "data": JSON.stringify(params),
            "contentType": "text/plain",
            "xhrFields": {
                "withCredentials": true
            },
            "dataType": "json",
            "success": function(data, status, jqxhr) {
                if (typeof data._system !== "undefined") {
                    // process system data that piggybacks on normal calls
                    self._processSystemData(data._system);
                }
                if (typeof data._componentStates !== "undefined") {
                    // process component states that piggyback on normal calls
                    self._processComponentStates(data._componentStates);
                }
                if (typeof data._componentUpdates !== "undefined") {
                    // process component states that piggyback on normal calls
                    self._processComponentUpdates(data._componentUpdates);
                }
                if (typeof onSuccess === "function") {
                    onSuccess(data, jqxhr.getAllResponseHeaders());
                }
            },
            "error": function(jqxhr, status, ex) {
                if (jqxhr.status >= 400 && jqxhr.status <500) {
                     // Session has expired, so auto-reinitialize
                    self._reinitialize( function() { Exhibit.UI.hideBusyIndicator(); });
                } else if (typeof onError === "function") {
                    onError(ex);
                } else {
                    Exhibit.Debug.log(jqxhr);
                }
            }
        });
    };
    f();
};

/**
 *
 */
Backstage._Impl.prototype.clearAsyncCalls = function() {
    //this._jsonpTransport.clear();
};

/**
 * @param {Object} job
 * @param {Function} job.run
 */
Backstage._Impl.prototype.queueJob = function(job) {
    this._jobQueue.queue(job);
};

/**
 * 
 */
Backstage._Impl.prototype.clearJobs = function() {
    this._jobQueue.clear();
};

/**
 * @param {Function} onSuccess
 * @param {Function} onError
 */
Backstage._Impl.prototype.loadDataLinks = function(onSuccess, onError) {
    var self = this;

    $("head").each(function(idx, el) {
        $("link", this).each(function(sidx, sel) {
            var rel = $(this).attr("rel");
            if (typeof rel !== "undefined" && (rel.match(/\bexhibit\/data\b/) || rel.match(/\bexhibit-data\b/))) {
                // only one per Exhibit now; the last one wins
                self._domConfiguration.link = { "url": $(this).attr("href") };
            }
        });
    });
    
    if (typeof onSuccess === "function") {
        onSuccess();
    }
};

/**
 * @param {Element} root
 * @param {Function} onSuccess
 * @param {Function} onError
 */
Backstage._Impl.prototype.configureFromDOM = function(root, onSuccess, onError) {
    var collectionElmts, coderElmts, coordinatorElmts, lensElmts, facetElmts, controlPanelElmts, otherElmts, f, uiContext, i, elmt, id, collection, serverSideConfig, self, processElmts, panel;

    collectionElmts = [];
    coderElmts = [];
    coordinatorElmts = [];
    lensElmts = [];
    facetElmts = [];
    controlPanelElmts = [];
    otherElmts = [];

    f = function(elmt) {
        var role;
        role = Exhibit.getRoleAttribute(elmt);
        if (role.length > 0) {
            switch (role) {
            case "collection":  collectionElmts.push(elmt); break;
            //case "coder":       coderElmts.push(elmt); break;
            //case "coordinator": coordinatorElmts.push(elmt); break;
            case "lens":        lensElmts.push(elmt); break;
            case "facet":       facetElmts.push(elmt); break;
            case "controlPanel": controlPanelElmts.push(elmt); break;
            default: 
                otherElmts.push(elmt);
            }
        } else {
            $(elmt).children().each(function(idx, el) {
                f(el);
            });
        }
    };
    f(root || document.body);
    
    uiContext = this._uiContext;

    for (i = 0; i < collectionElmts.length; i++) {
        elmt = collectionElmts[i];
        id = elmt.id;
        if (typeof id === "undefined" || id === null || id.length === 0) {
            id = "default";
        }
        
        collection = Backstage.Collection.createFromDOM2(id, elmt, uiContext);
        serverSideConfig = collection.getServerSideConfiguration();
        serverSideConfig.id = id;
        
        this._collectionMap[id] = collection;
        this._domConfiguration.collections.push(serverSideConfig);
    }
    
    for (i = 0; i < lensElmts.length; i++) {
        elmt = lensElmts[i];
        try {
            this._uiContext.registerLensFromDOM(elmt);
        } catch (e) {
            Exhibit.Debug.exception(e);
        }
    }
    
    self = this;
    processElmts = function(elmts) {
        var i, elmt, id, component, serverSideConfig;
        for (i = 0; i < elmts.length; i++) {
            elmt = elmts[i];
            try {
                component = Backstage.UI.createFromDOM(elmt, uiContext);
                if (component !== null) {
                    serverSideConfig = component.getServerSideConfiguration();
                    serverSideConfig.id = component.getID();
                    
                    self._domConfiguration.components.push(serverSideConfig);
                }
            } catch (e) {
                Exhibit.Debug.exception(e);
            }
        }
    };

    processElmts(coordinatorElmts);
    processElmts(coderElmts);
    processElmts(facetElmts);

    /**
    if (controlPanelElmts.length === 0) {
        panel = Exhibit.ControlPanel.createFromDOM(
            $("<div>").prependTo(document.body),
            null,
            uiContext
        );
        panel.setCreatedAsDefault();
    } else {
        processElmts(controlPanelElmts);
    }
    */

    processElmts(otherElmts);
    
    /*
    var exporters = Exhibit.getAttribute(document.body, "exporters");
    if (exporters != null) {
        exporters = exporters.split(";");
        for (var i = 0; i < exporters.length; i++) {
            var expr = exporters[i];
            var exporter = null;
            
            try {
                exporter = eval(expr);
            } catch (e) {}
            
            if (exporter === null) {
                try { exporter = eval(expr + "Exporter"); } catch (e) {}
            }
            
            if (exporter === null) {
                try { exporter = eval("Exhibit." + expr + "Exporter"); } catch (e) {}
            }
            
            if (typeof exporter === "object") {
                Exhibit.addExporter(exporter);
            }
        }
    }
    
    var hash = document.location.hash;
    if (hash.length > 1) {
        var itemID = decodeURIComponent(hash.substr(1));
        if (this._database.containsItem(itemID)) {
            this._showFocusDialogOnItem(itemID);
        }
    }
    */
    
    if (typeof this._collectionMap["default"] === "undefined") {
        collection = Backstage.Collection.createAllItemsCollection("default", this);
        this._collectionMap[id] = collection;
        this._domConfiguration.collections.push(collection.getServerSideConfiguration());
    }
    
    this._domConfiguration.uiContext = this._uiContext.getServerSideConfiguration();

    this._configureFromDOM(onSuccess,onError);
};

Backstage._Impl.prototype._reinitialize = function(onSuccess) {
    this._initialized = false;
    
    var onError = function(e) {
        /*
         * Couldn't automatically reinitialize
         */
        alert("We're sorry: \n" +
              "This session has been inactive for too long and cannot be continued.\n" +
              "Please refresh the page to start a new session.");
    };
    
    this._configureFromDOM(onSuccess, onError);
};

/**
 * @private
 * @param {Function} onSuccess
 * @param {Function} onError
 */
Backstage._Impl.prototype._configureFromDOM = function(onSuccess, onError) {
    var self = this;
    this.asyncCall(
        "POST",
        Backstage.serverPrefix + "/exhibit-session/",
        {
            "configuration": self._domConfiguration
        },
        function(o) {
            self._exhibitSession = o.location;
            if (typeof onSuccess === "function") {
                $(document).trigger("exhibitConfigured.exhibit", self);
                onSuccess();
            }
        },
        onError
    );
};

/**
 * @private
 * @param {Object} o
 * @param {Object} o.properties
 * @param {Object} o.types
 */
Backstage._Impl.prototype._processSystemData = function(o) {
    this._properties = o.properties;
    this._types = o.types;
    this._initialized = true;
};

/**
 * @param {String} id Component identifier
 * @returns {String} Component URL on this Backstage server
 */
Backstage._Impl.prototype.makeComponentURL = function(id) {
    return Backstage.serverPrefix + this._exhibitSession + "/component/" + id;
};

/**
 * @private
 * @param {Array} states
 */
Backstage._Impl.prototype._processComponentStates = function(states) {
    var i, state, component;
    for (i = 0; i < states.length; i++) {
        try {
            state = states[i];
            component = this.getRegistry().getID(state.id);
            component.onNewState(state);
        } catch (e) {
            Exhibit.Debug.exception(e);
        }
    }
};

/**
 * @private
 * @param {Array} updates
 */
Backstage._Impl.prototype._processComponentUpdates = function(updates) {
    var i, update, component;
    for (i = 0; i < updates.length; i++) {
        try {
            update = updates[i];
            component = this.getRegistry().getID(update.id);
            component.onUpdate(update);
        } catch (e) {
            Exhibit.Debug.exception(e);
        }
    }
};
