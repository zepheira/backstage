Backstage.UI = {};

Backstage.UI.createFromDOM = function(elmt, uiContext, id) {
    var role = Exhibit.getRoleAttribute(elmt);
    switch (role) {
    case "view":
        return Backstage.UI.createViewFromDOM(elmt, null, uiContext, id);
    case "facet":
        return Backstage.UI.createFacetFromDOM(elmt, null, uiContext, id);
    }
    return null;  
};

Backstage.UI.createViewFromDOM = function(elmt, container, uiContext, id) {
    var viewClass = Backstage.UI.viewClassNameToViewClass(Exhibit.getAttribute(elmt, "viewClass"));
    return viewClass.createFromDOM(elmt, container, uiContext, id);
};

Backstage.UI.viewClassNameToViewClass = function(name) {
    if (name != null && name.length > 0) {
        try {
            return Backstage.UI._stringToObject(name, "View");
        } catch (e) {
            Exhibit.Debug.warn("Unknown viewClass " + name);
        }
    }
    return Backstage.TileView;
};

Backstage.UI.createFacetFromDOM = function(elmt, container, uiContext, id) {
    var facetClass = Backstage.UI.facetClassNameToFacetClass(Exhibit.getAttribute(elmt, "facetClass"));
    return facetClass.createFromDOM(elmt, container, uiContext, id);
};

Backstage.UI.facetClassNameToFacetClass = function(name) {
    if (name != null && name.length > 0) {
        try {
            return Backstage.UI._stringToObject(name, "Facet");
        } catch (e) {
            Exhibit.Debug.warn("Unknown facetClass " + name);
        }
    }
    return Backstage.ListFacet;
};

Backstage.UI._stringToObject = function(name, suffix) {
    if (!name.startsWith("Backstage.")) {
        if (!name.endsWith(suffix)) {
            try {
                return eval("Backstage." + name + suffix);
            } catch (e) {
                // ignore
            }
        }
        
        try {
            return eval("Backstage." + name);
        } catch (e) {
            // ignore
        }
    }
    
    if (!name.endsWith(suffix)) {
        try {
            return eval(name + suffix);
        } catch (e) {
            // ignore
        }
    }
    
    try {
        return eval(name);
    } catch (e) {
        // ignore
    }
    
    throw new Error("Unknown class " + name);
};
