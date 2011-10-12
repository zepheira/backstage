/**
 * @fileOverview
 * @author David Huynh
 * @author <a href="mailto:ryanlee@zepheira.com">Ryan Lee</a>
 */

/**
 * @constructor
 * @class
 * @param {Element} containerElmt
 * @param {Backstage.UIContext} uiContext
 * @param {String} id
 */
Backstage.ListFacet = function(containerElmt, uiContext, id) {
    this._id = id;
    this._localID = null;
    this._div = containerElmt;
    this._uiContext = uiContext;
    this._expressionString = null;
    this._expression = null;
    this._settings = {};
    
    this._valueSet = new Exhibit.Set();
    this._selectMissing = false;
    
    this._state = {
        values: [],
        count:  0
    };
};

/**
 * @constant
 */
Backstage.ListFacet._settingSpecs = {
    "facetLabel":       { type: "text" },
    "fixedOrder":       { type: "text" },
    "sortMode":         { type: "text", defaultValue: "value" },
    "sortDirection":    { type: "text", defaultValue: "forward" },
    "showMissing":      { type: "boolean", defaultValue: true },
    "missingLabel":     { type: "text" },
    "scroll":           { type: "boolean", defaultValue: true },
    "height":           { type: "text" },
    "colorCoder":       { type: "text", defaultValue: null }
};

/**
 * @static
 * @param {Element} configElmt
 * @param {Element} containerElmt
 * @param {Backstage.UIContext} uiContext
 * @param {String} id
 * @returns {Backstage.ListFacet}
 */
Backstage.ListFacet.createFromDOM = function(configElmt, containerElmt, uiContext, id) {
    var configuration, thisUIContext, facet, expressionString, selection, i, selectMissing;
    configuration = Exhibit.getConfigurationFromDOM(configElmt);
    thisUIContext = Backstage.UIContext.createFromDOM(configElmt, uiContext);
    facet = new Backstage.ListFacet(
        (typeof containerElmt !== "undefined" && containerElmt !== null) ?
            containerElmt :
            configElmt, 
        thisUIContext,
        id
    );
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(configElmt, Exhibit.ListFacet._settingSpecs, facet._settings);
    
    try {
        expressionString = Exhibit.getAttribute(configElmt, "expression");
        if (expressionString !== null && expressionString.length > 0) {
            facet._expressionString = expressionString;
            facet._expression = Exhibit.ExpressionParser.parse(expressionString);
        }
        
        selection = Exhibit.getAttribute(configElmt, "selection", ";");
        if (selection !== null && selection.length > 0) {
            for (i = 0; i < selection.length; i++) {
                facet._valueSet.add(selection[i]);
            }
        }
        
        selectMissing = Exhibit.getAttribute(configElmt, "selectMissing");
        if (selectMissing !== null && selectMissing.length > 0) {
            facet._selectMissing = (selectMissing === "true");
        }
    } catch (e) {
        Exhibit.Debug.exception(e, "ListFacet: Error processing configuration of list facet");
    }
    Backstage.ListFacet._configure(facet, configuration);
    
    facet._initializeUI();
    //thisUIContext.getCollection().addFacet(facet);
    
    return facet;
};

/**
 * @private
 * @static
 * @param {Backstage.ListFacet} facet
 * @param {Object} configuration 
 */
Backstage.ListFacet._configure = function(facet, configuration) {
    var selection, i, values, orderMap, segment;

    Exhibit.SettingsUtilities.collectSettings(configuration, Backstage.ListFacet._settingSpecs, facet._settings);
    
    if (typeof configuration.expression !== "undefined") {
        facet._expressionString = configuration.expression;
        facet._expression = Exhibit.ExpressionParser.parse(configuration.expression);
    }
    if (typeof configuration.selection !== "undefined") {
        selection = configuration.selection;
        for (i = 0; i < selection.length; i++) {
            facet._valueSet.add(selection[i]);
        }
    }
    if (typeof configuration.selectMissing !== "undefined") {
        facet._selectMissing = configuration.selectMissing;
    }
    
    if (typeof facet._settings.facetLabel === "undefined") {
        facet._settings.facetLabel = "missing ex:facetLabel";
        if (facet._expression !== null && facet._expression.isPath()) {
            segment = facet._expression.getPath().getLastSegment();
            /*
            var property = facet._uiContext.getDatabase().getProperty(segment.property);
            if (property !== null) {
                facet._settings.facetLabel = segment.forward ? property.getLabel() : property.getReverseLabel();
            }
            */
        }
    }
    if (typeof facet._settings.fixedOrder !== "undefined") {
        values = facet._settings.fixedOrder.split(";");
        orderMap = {};
        for (i = 0; i < values.length; i++) {
            orderMap[values[i].trim()] = i;
        }
        
        facet._orderMap = orderMap;
    }
    
    //if (typeof facet._settings.colorCoder !== "undefined") {
        //facet._colorCoder = facet._uiContext.getExhibit().getComponent(facet._settings.colorCoder);
    //}

    facet._setLocalID();
    facet._register();
};

/**
 * @private
 */
Backstage.ListFacet.prototype._register = function() {
    Exhibit.Registry.register(Exhibit.Facet._registryKey, this.getID(), this);
};

/**
 * @private
 */
Backstage.ListFacet.prototype._unregister = function() {
    Exhibit.Registry.unregister(Exhibit.Facet._registryKey, this.getID());
};

/**
 * @private
 */
Backstage.ListFacet.prototype.dispose = function() {
    this._unregister();
    $(this._div).empty();

    this._dom = null;

    this._div = null;
    this._uiContext = null;
};

/**
 * @private
 */
Backstage.ListFacet.prototype._setLocalID = function() {
    this._localID = $(this._div).attr("id");

    // @@@ not very unique
    if (typeof this._localID === "undefined" || this._localID === null) {
        this._localID = "facet"
            + "-"
            + this._expressionString
            + "-"
            + this._uiContext.getCollection().getID();
    }
};

/**
 * @returns {String}
 */
Backstage.ListFacet.prototype.getServerID = function() {
    return this._id;
};

/**
 * @returns {String}
 */
Backstage.ListFacet.prototype.getID = function() {
    return this._localID;
};

/**
 * @private
 */
Backstage.ListFacet.prototype._initializeUI = function() {
    var self = this;
    this._dom = Exhibit.FacetUtilities[this._settings.scroll ? "constructFacetFrame" : "constructFlowingFacetFrame"](
        this,
        this._div,
        this._settings.facetLabel,
        function(evt) { self._clearSelections(); },
        this._uiContext
    );
    
    if (typeof this._settings.height !== "undefined" && this._settings.scroll) {
        $(this._dom.valuesContainer).css("height", this._settings.height);
    }
};

/**
 * @returns {Boolean}
 */
Backstage.ListFacet.prototype.hasRestrictions = function() {
    return this._valueSet.size() > 0 || this._selectMissing;
};

/**
 * @returns {Object}
 */
Backstage.ListFacet.prototype.getServerSideConfiguration = function() {
    return {
        role:           "facet",
        facetClass:     "List",
        collectionID:   this._uiContext.getCollection().getID(),
        expression:     this._expression.getServerSideConfiguration(),
        selection:      this._valueSet.toArray(),
        selectMissing:  this._selectMissing,
        sortMode:       this._settings.sortMode,
        sortDirection:  this._settings.sortDirection
    };
};

/**
 * @param {Object} state
 */
Backstage.ListFacet.prototype.onNewState = function(state) {
    this._state = state;
    this._reconstruct();
};

/**
 */
Backstage.ListFacet.prototype.onUpdate = function(update) {
    //this._reconstruct();
};

/**
 *
 */
Backstage.ListFacet.prototype.clearAllRestrictions = function() {
    var restrictions, self, onSuccess, url;
    restrictions = { selection: [], selectMissing: false };
    if (this.hasRestrictions()) {
        this._valueSet.visit(function(v) {
            restrictions.selection.push(v);
        });
        restrictions.selectMissing = this._selectMissing;
        
        self = this;
        onSuccess = function() {
            self._valueSet = new Exhibit.Set();
            self._selectMissing = false;
        };
        
        url = Backstage.urlPrefix + ".." + backstage._exhibitSession + "/component/" + this._id;
        this._uiContext.getBackstage().asyncCall("PUT", url, {}, onSuccess);
    }
    return restrictions;
};

/**
 * @param {Array} restrictions
 */
Backstage.ListFacet.prototype.applyRestrictions = function(restrictions) {
    var self, onSuccess, i, url;
    self = this;
    
    onSuccess = function() {
        self._valueSet = new Exhibit.Set();
        for (i = 0; i < restrictions.selection.length; i++) {
            self._valueSet.add(restrictions.selection[i]);
        }
        self._selectMissing = restrictions.selectMissing;
        
        Exhibit.UI.hideBusyIndicator();
    };
    
    Exhibit.UI.showBusyIndicator();
    url = Backstage.urlPrefix + ".." + backstage._exhibitSession + "/component/" + this._id;
    this._uiContext.getBackstage().asyncCall(
        "PUT",
        url,
        { restrictions: restrictions }, 
        onSuccess
    );
};

/**
 * @private
 */
Backstage.ListFacet.prototype._reconstruct = function() {
    var entries, facetHasSelection, omittedCount, self, containerDiv, constructFacetItemFunction, constructValue, j, omittedDiv;
    entries = this._state.values;
    facetHasSelection = this._state.selectionCount > 0;
    
	omittedCount = 0;
    self = this;
    containerDiv = this._dom.valuesContainer;
    $(containerDiv).hide();
    $(containerDiv).empty();
    
    constructFacetItemFunction = Exhibit.FacetUtilities[this._settings.scroll ? "constructFacetItem" : "constructFlowingFacetItem"];
    constructValue = function(entry) {
        var label, onSelect, onSelectOnly, elmt;
		if (entry.count === 1 && !entry.selected) {
			omittedCount++;
			return;
		}
		
        label = typeof entry.label !== "undefined" ? entry.label : entry.value;
        
        onSelect = function(evt) {
            self._filter(entry.value, label, false);
            evt.preventDefault();
            evt.stopPropagation();
        };
        onSelectOnly = function(evt) {
            self._filter(entry.value, label, !(evt.ctrlKey || evt.metaKey));
            evt.preventDefault();
            evt.stopPropagation();
        };
        elmt = constructFacetItemFunction(
            label, 
            entry.count, 
            null, // color
            entry.selected, 
            facetHasSelection,
            onSelect,
            onSelectOnly,
            self._uiContext
        );

        $(containerDiv).append(elmt);
    };
    
    for (j = 0; j < entries.length; j++) {
        constructValue(entries[j]);
    }
	
	if (omittedCount > 0) {
		omittedDiv = $("<div>");
		$(omittedDiv).html("<center>Omitted " + String(omittedCount) + " choices with counts of 1</center>");
        $(containerDiv).append(omittedDiv);
	}
	
    $(containerDiv).show();
    
    this._dom.setSelectionCount(this._state.selectionCount);
};

/**
 * @private
 * @param {String|Number} value
 * @param {String} label
 * @param {Boolean} selectOnly
 */
Backstage.ListFacet.prototype._filter = function(value, label, selectOnly) {
    var self, selected, select, deselect, oldValues, oldSelectMissing, newValues, newSelectMissing, actionLabel, wasSelected, wasOnlyThingSelected, newRestrictions, oldRestrictions;
    
    self = this;
    oldValues = new Exhibit.Set(this._valueSet);
    oldSelectMissing = this._selectMissing;
    
    if (value === null) { // the (missing this field) case
        wasSelected = oldSelectMissing;
        wasOnlyThingSelected = wasSelected && (oldValues.size() === 0);
        
        if (selectOnly) {
            if (oldValues.size() === 0) {
                newSelectMissing = !oldSelectMissing;
            } else {
                newSelectMissing = true;
            }
            newValues = new Exhibit.Set();
        } else {
            newSelectMissing = !oldSelectMissing;
            newValues = new Exhibit.Set(oldValues);
        }
    } else {
        wasSelected = oldValues.contains(value);
        wasOnlyThingSelected = wasSelected && (oldValues.size() === 1) && !oldSelectMissing;
        
        if (selectOnly) {
            newSelectMissing = false;
            newValues = new Exhibit.Set();
            
            if (!oldValues.contains(value)) {
                newValues.add(value);
            } else if (oldValues.size() > 1 || oldSelectMissing) {
                newValues.add(value);
            }
        } else {
            newSelectMissing = oldSelectMissing;
            newValues = new Exhibit.Set(oldValues);
            if (newValues.contains(value)) {
                newValues.remove(value);
            } else {
                newValues.add(value);
            }
        }
    }
    
    newRestrictions = { selection: newValues.toArray(), selectMissing: newSelectMissing };
    oldRestrictions = { selection: oldValues.toArray(), selectMissing: oldSelectMissing };

    Exhibit.History.pushComponentState(
        this,
        Exhibit.Facet._registryKey,
        newRestrictions,
        (selectOnly && !wasOnlyThingSelected) ?
            String.substitute(
                Exhibit.FacetUtilities.l10n.facetSelectOnlyActionTitle,
                [ label, this._settings.facetLabel ]) :
            String.substitute(
                Exhibit.FacetUtilities.l10n[wasSelected ? "facetUnselectActionTitle" : "facetSelectActionTitle"],
                [ label, this._settings.facetLabel ]),
        true
    );
};

/**
 * @private
 */
Backstage.ListFacet.prototype._clearSelections = function() {
    var state, self;
    state = {
        "selection": [],
        "selectMissing": false
    };
    self = this;
    Exhibit.History.pushComponentState(
        this,
        Exhibit.Facet._registryKey,
        state,
        String.substitute(
            Exhibit.FacetUtilities.l10n.facetClearSelectionsActionTitle,
            [ this._settings.facetLabel ])
    );
};

/**
 * @returns {Object}
 */
Backstage.ListFacet.prototype.exportState = function() {
    var s = this._valueSet.toArray();
    return {
        "selection": s,
        "selectMissing": this._selectMissing
    };
};

/**
 * @param {Object} state
 * @param {Array} state.selection
 * @param {Boolean} state.selectMissing
 */
Backstage.ListFacet.prototype.importState = function(state) {
    if (state.selection.length === 0 && !state.selectMissing) {
        this.clearAllRestrictions();
    } else {
        this.applyRestrictions(state);
    }
};
