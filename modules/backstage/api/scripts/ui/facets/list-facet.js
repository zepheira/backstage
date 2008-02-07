/*==================================================
 *  Backstage.ListFacet
 *==================================================
 */

Backstage.ListFacet = function(containerElmt, uiContext) {
    this._div = containerElmt;
    this._uiContext = uiContext;
    this._settings = {};
    
    this._valueSet = new Exhibit.Set();
    this._selectMissing = false;
    
    this._state = {
        values: [],
        count:  0
    };
};

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

Backstage.ListFacet.createFromDOM = function(configElmt, containerElmt, uiContext) {
    var configuration = Exhibit.getConfigurationFromDOM(configElmt);
    var uiContext = Backstage.UIContext.createFromDOM(configElmt, uiContext);
    var facet = new Backstage.ListFacet(
        containerElmt != null ? containerElmt : configElmt, 
        uiContext
    );
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(configElmt, Exhibit.ListFacet._settingSpecs, facet._settings);
    
    try {
        var expressionString = Exhibit.getAttribute(configElmt, "expression");
        if (expressionString != null && expressionString.length > 0) {
            facet._expression = Exhibit.ExpressionParser.parse(expressionString);
        }
        
        var selection = Exhibit.getAttribute(configElmt, "selection", ";");
        if (selection != null && selection.length > 0) {
            for (var i = 0, s; s = selection[i]; i++) {
                facet._valueSet.add(s);
            }
        }
        
        var selectMissing = Exhibit.getAttribute(configElmt, "selectMissing");
        if (selectMissing != null && selectMissing.length > 0) {
            facet._selectMissing = (selectMissing == "true");
        }
    } catch (e) {
        SimileAjax.Debug.exception(e, "ListFacet: Error processing configuration of list facet");
    }
    Backstage.ListFacet._configure(facet, configuration);
    
    facet._initializeUI();
    //uiContext.getCollection().addFacet(facet);
    
    return facet;
};

Backstage.ListFacet._configure = function(facet, configuration) {
    Exhibit.SettingsUtilities.collectSettings(configuration, Backstage.ListFacet._settingSpecs, facet._settings);
    
    if ("expression" in configuration) {
        facet._expression = Exhibit.ExpressionParser.parse(configuration.expression);
    }
    if ("selection" in configuration) {
        var selection = configuration.selection;
        for (var i = 0; i < selection.length; i++) {
            facet._valueSet.add(selection[i]);
        }
    }
    if ("selectMissing" in configuration) {
        facet._selectMissing = configuration.selectMissing;
    }
    
    if (!("facetLabel" in facet._settings)) {
        facet._settings.facetLabel = "missing ex:facetLabel";
        if (facet._expression != null && facet._expression.isPath()) {
            var segment = facet._expression.getPath().getLastSegment();
            /*
            var property = facet._uiContext.getDatabase().getProperty(segment.property);
            if (property != null) {
                facet._settings.facetLabel = segment.forward ? property.getLabel() : property.getReverseLabel();
            }
            */
        }
    }
    if ("fixedOrder" in facet._settings) {
        var values = facet._settings.fixedOrder.split(";");
        var orderMap = {};
        for (var i = 0; i < values.length; i++) {
            orderMap[values[i].trim()] = i;
        }
        
        facet._orderMap = orderMap;
    }
    
    if ("colorCoder" in facet._settings) {
        //facet._colorCoder = facet._uiContext.getExhibit().getComponent(facet._settings.colorCoder);
    }
}

Backstage.ListFacet.prototype.dispose = function() {
    this._div.innerHTML = "";

    this._dom = null;

    this._div = null;
    this._uiContext = null;
};

Backstage.ListFacet.prototype._initializeUI = function() {
    var self = this;
    
    this._div.innerHTML = "";
    var template = {
        elmt: this._div,
        children: [
            {   tag: "div",
                field: "headerDiv"
            },
            {   tag: "div",
                className: "exhibit-collectionView-body",
                field: "bodyDiv"
            },
            {   tag: "div",
                field: "footerDiv"
            }
        ]
    };
    this._dom = SimileAjax.DOM.createDOMFromTemplate(template);

    //this._reconstruct();
};

Backstage.ListFacet.prototype.getServerSideConfiguration = function() {
    return {
        role:           "facet",
        facetClass:     "List",
        collectionID:   this._uiContext.getCollection().getID(),
        expression:     this._expression.getServerSideConfiguration(),
        selection:      this._valueSet.toArray(),
        selectMissing:  this._selectMissing
    };
};

Backstage.ListFacet.prototype.onNewState = function(state) {console.log(state);
    this._state = state;
    this._reconstruct();
};

Backstage.ListFacet.prototype.onUpdate = function(update) {
    this._reconstruct();
};

Backstage.ListFacet.prototype._reconstruct = function() {
};
