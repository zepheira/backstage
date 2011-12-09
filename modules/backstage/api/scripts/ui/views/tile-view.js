/**
 * @fileOverview Default tiled view for displaying query results.
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
Backstage.TileView = function(containerElmt, uiContext) {
    this._id = null;
    this._registered = false;
    this._div = containerElmt;
    this._uiContext = uiContext;
    this._settings = {};
    
    this._state = { 
        count: 0,
        items: []
    };
};

/**
 * @constant
 */ 
Backstage.TileView._settingSpecs = {
    "showToolbox":          { type: "boolean", defaultValue: true }
};

/**
 * @static
 * @param {Element} configElmt
 * @param {Element} containerElmt
 * @param {Backstage.UIContext} uiContext
 * @returns {Backstage.TileView}
 */
Backstage.TileView.createFromDOM = function(configElmt, containerElmt, uiContext) {
    var configuration, view;
    configuration = Exhibit.getConfigurationFromDOM(configElmt);
    view = new Backstage.TileView(
        (typeof containerElmt !== "undefined" && containerElmt !== null) ?
            containerElmt :
            configElmt,
        Backstage.UIContext.createFromDOM(configElmt, uiContext)
    );
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(
        configElmt, Backstage.TileView._settingSpecs, view._settings);
    Exhibit.SettingsUtilities.collectSettings(
        configuration, Backstage.TileView._settingSpecs, view._settings);
    
    view._setIdentifier();
    view.register();
    view._initializeUI();
    return view;
};

/**
 * @private
 */
Backstage.TileView.prototype._setIdentifier = function() {
    this._id = $(this._div).attr("id");

    if (typeof this._id === "undefined" || this._id === null) {
        this._id = Exhibit.View._registryKey
            + "-"
            + this._expressionString
            + "-"
            + this._uiContext.getCollection().getID()
            + "-"
            + this._uiContext.getBackstage().getRegistry().generateIdentifier(
               Exhibit.View._registryKey
            );
    }
};

/**
 * @returns {String}
 */
Backstage.TileView.prototype.getID = function() {
    return this._id;
};

/**
 *
 */
Backstage.TileView.prototype.register = function() {
    this._uiContext.getBackstage().getRegistry().register(
        Exhibit.View._registryKey,
        this.getID(),
        this
    );
    this._registered = true;
};

/**
 *
 */
Backstage.TileView.prototype.unregister = function() {
    this._uiContext.getBackstage().getRegistry().unregister(
        Exhibit.View._registryKey,
        this.getID()
    );
    this._registered = false;
};

/**
 *
 */
Backstage.TileView.prototype.dispose = function() {
    this.unregister();
    $(this._div).empty();

    this._dom = null;

    this._div = null;
    this._uiContext = null;
};

/**
 *
 */
Backstage.TileView.prototype._initializeUI = function() {
    var self, template;

    self = this;
    
    $(this._div).empty();
    template = {
        elmt: this._div,
        children: [
            {   tag: "div",
                field: "headerDiv"
            },
            {   tag: "div",
                "class": "exhibit-collectionView-body",
                field: "bodyDiv"
            },
            {   tag: "div",
                field: "footerDiv"
            }
        ]
    };
    this._dom = $.simileDOM("template", template);

    //this._reconstruct();
};

/**
 * @returns {Object}
 */
Backstage.TileView.prototype.getServerSideConfiguration = function() {
    return {
        role: "view",
        viewClass: "Tile",
        collectionID: this._uiContext.getCollection().getID()
    };
};

/**
 * @param {Object} state
 */
Backstage.TileView.prototype.onNewState = function(state) {
    this._state = state;
    this._reconstruct();
};

/**
 *
 */
Backstage.TileView.prototype.onUpdate = function(update) {
    this._reconstruct();
};

/**
 * @private
 */
Backstage.TileView.prototype._reconstruct = function() {
    var view, uiContext, lensRegistry, ul, i, itemID, li;
    view = this;
    uiContext = this._uiContext;
    lensRegistry = uiContext.getLensRegistry();
    
    $(this._div).hide();
    $(this._dom.bodyDiv).html(
        "<p>" + 
            String(this._state.count) + " results in total" +
            ((this._state.count <= 20) ? "" : " (showing first 20 only)") +
            "</p>");
    
    ul = $("<ul>");
    for (i = 0; i < this._state.items.length; i++) {
        itemID = this._state.items[i];
        li = $("<li>").get(0);
        
        lensRegistry.createLensFromBackstage(this._state.lenses[i], li, uiContext);
        
        $(ul).append(li);
    }
    $(this._dom.bodyDiv).append(ul);

    $(this._div).show();
};
