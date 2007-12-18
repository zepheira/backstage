/*==================================================
 *  Backstage.TileView
 *==================================================
 */

Backstage.TileView = function(containerElmt, uiContext) {
    this._div = containerElmt;
    this._uiContext = uiContext;
    this._settings = {};
    
    var view = this;

    this._listener = { onItemsChanged: function() { view._reconstruct(); } };
    uiContext.getCollection().addListener(this._listener);
};

Backstage.TileView._settingSpecs = {
    "showToolbox":          { type: "boolean", defaultValue: true }
};

Backstage.TileView.createFromDOM = function(configElmt, containerElmt, uiContext) {
    var configuration = Exhibit.getConfigurationFromDOM(configElmt);
    var view = new Backstage.TileView(
        containerElmt != null ? containerElmt : configElmt,
        Backstage.UIContext.createFromDOM(configElmt, uiContext)
    );
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(
        configElmt, Backstage.TileView._settingSpecs, view._settings);
    Exhibit.SettingsUtilities.collectSettings(
        configuration, Backstage.TileView._settingSpecs, view._settings);
    
    view._initializeUI();
    return view;
};

Backstage.TileView.prototype.dispose = function() {
    this._uiContext.getCollection().removeListener(this._listener);

    this._div.innerHTML = "";

    this._dom = null;

    this._div = null;
    this._uiContext = null;
};

Backstage.TileView.prototype._initializeUI = function() {
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

Backstage.TileView.prototype.getServerSideConfiguration = function() {
    return {
        role: "view",
        viewClass: "Tile",
        collectionID: this._uiContext.getCollection().getID()
    };
};

Backstage.TileView.prototype._reconstruct = function() {
    var view = this;
    
    this._div.style.display = "none";

    this._dom.bodyDiv.innerHTML = "";
    //this._orderedViewFrame.reconstruct();
    //closeGroups(0);

    this._div.style.display = "block";
};
