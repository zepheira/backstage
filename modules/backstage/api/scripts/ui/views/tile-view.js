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
    $.extend(this, new Exhibit.View(
        "backstagetile",
        containerElmt,
        uiContext
    ));
    this.addSettingSpecs(Backstage.TileView._settingSpecs);
    
    this._state = { 
        count: 0,
        items: []
    };

    this.register();
};

/**
 * @constant
 */ 
Backstage.TileView._settingSpecs = {};

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
        configElmt, view.getSettingSpecs(), view._settings);
    Exhibit.SettingsUtilities.collectSettings(
        configuration, view.getSettingSpecs(), view._settings);
    
    view._initializeUI();
    return view;
};

/**
 *
 */
Backstage.TileView.prototype.dispose = function() {
    this._dispose();
};

/**
 *
 */
Backstage.TileView.prototype._initializeUI = function() {
    var self, template;

    self = this;
    
    $(this.getContainer()).empty();
    template = {
        elmt: this.getContainer(),
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
        collectionID: this.getUIContext().getCollection().getID()
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
    uiContext = this.getUIContext();
    lensRegistry = uiContext.getLensRegistry();
    
    $(this.getContainer()).hide();
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

    $(this.getContainer()).show();
};
