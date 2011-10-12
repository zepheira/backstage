/**
 * @fileOverview Backstage UI utility functions.
 * @author David Huynh
 * @author <a href="mailto:ryanlee@zepheira.com">Ryan Lee</a>
 */

/**
 * @namespace
 */
Backstage.UI = {};

/**
 * @static
 * @param {Element} elmt
 * @param {Backstage.UIContext} uiContext
 * @param {String} id
 * @returns {Object}
 */
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

/**
 * @static
 * @param {Element} elmt
 * @param {Element} container
 * @param {Backstage.UIContext} uiContext
 * @param {String} id
 * @returns {Object}
 */
Backstage.UI.createViewFromDOM = function(elmt, container, uiContext, id) {
    var viewClass = Backstage.UI.viewClassNameToViewClass(Exhibit.getAttribute(elmt, "viewClass"));
    return viewClass.createFromDOM(elmt, container, uiContext, id);
};

/**
 * @static
 * @param {String} name
 * @returns {Object}
 */
Backstage.UI.viewClassNameToViewClass = function(name) {
    if (typeof name !== "undefined" && name !== null && name.length > 0) {
        try {
            return Backstage.UI._stringToObject(name, "View");
        } catch (e) {
            Exhibit.Debug.warn("Unknown viewClass " + name);
        }
    }
    return Backstage.TileView;
};

/**
 * @static
 * @param {Element} elmt
 * @param {Element} container
 * @param {Backstage.UIContext} uiContext
 * @param {String} id
 * @returns {Object}
 */
Backstage.UI.createFacetFromDOM = function(elmt, container, uiContext, id) {
    var facetClass = Backstage.UI.facetClassNameToFacetClass(Exhibit.getAttribute(elmt, "facetClass"));
    return facetClass.createFromDOM(elmt, container, uiContext, id);
};

/**
 * @static
 * @param {String} name
 * @returns {Object}
 */
Backstage.UI.facetClassNameToFacetClass = function(name) {
    if (typeof name !== "undefined" && name !== null && name.length > 0) {
        try {
            return Backstage.UI._stringToObject(name, "Facet");
        } catch (e) {
            Exhibit.Debug.warn("Unknown facetClass " + name);
        }
    }
    return Backstage.ListFacet;
};

/**
 * @static
 * @private
 * @param {String} name
 * @param {String} suffix
 * @returns {Object}
 */
Backstage.UI._stringToObject = function(name, suffix) {
    if (!name.startsWith("Backstage.")) {
        if (!name.endsWith(suffix)) {
            try {
                return eval("Backstage." + name + suffix);
            } catch (e1) {
                // ignore
            }
        }
        
        try {
            return eval("Backstage." + name);
        } catch (e2) {
            // ignore
        }
    }
    
    if (!name.endsWith(suffix)) {
        try {
            return eval(name + suffix);
        } catch (e3) {
            // ignore
        }
    }
    
    try {
        return eval(name);
    } catch (e4) {
        // ignore
    }
    
    throw new Error("Unknown class " + name);
};
