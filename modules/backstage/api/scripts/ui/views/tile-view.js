/*==================================================
 *  Backstage.TileView
 *==================================================
 */

Backstage.TileView = function(containerElmt, uiContext, id) {
    this._id = id;
    this._div = containerElmt;
    this._uiContext = uiContext;
    this._settings = {};
    
    this._state = { 
        count: 0,
        items: []
    };
};

Backstage.TileView._settingSpecs = {
    "showToolbox":          { type: "boolean", defaultValue: true }
};

Backstage.TileView.createFromDOM = function(configElmt, containerElmt, uiContext, id) {
    var configuration = Exhibit.getConfigurationFromDOM(configElmt);
    var view = new Backstage.TileView(
        containerElmt != null ? containerElmt : configElmt,
        Backstage.UIContext.createFromDOM(configElmt, uiContext),
        id
    );
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(
        configElmt, Backstage.TileView._settingSpecs, view._settings);
    Exhibit.SettingsUtilities.collectSettings(
        configuration, Backstage.TileView._settingSpecs, view._settings);
    
    view._initializeUI();
    return view;
};

Backstage.TileView.prototype.dispose = function() {
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
    this._dom = $.simileDOM("template", template);

    //this._reconstruct();
};

Backstage.TileView.prototype.getServerSideConfiguration = function() {
    return {
        role: "view",
        viewClass: "Tile",
        collectionID: this._uiContext.getCollection().getID()
    };
};

Backstage.TileView.prototype.onNewState = function(state) {
    this._state = state;
    this._reconstruct();
};

Backstage.TileView.prototype.onUpdate = function(update) {
    this._reconstruct();
};

Backstage.TileView.prototype._reconstruct = function() {
    var view = this;
    var uiContext = this._uiContext;
    var lensRegistry = uiContext.getLensRegistry();
    
    this._div.style.display = "none";
    this._dom.bodyDiv.innerHTML = 
        "<p>" + 
            this._state.count + " results in total" +
            ((this._state.count <= 20) ? "" : " (showing first 20 only)") +
        "</p>";
    
    var ul = document.createElement("ul");
    for (var i = 0; i < this._state.items.length; i++) {
        var itemID = this._state.items[i];
        var li = document.createElement("li");
        
        lensRegistry.createLensFromBackstage(this._state.lenses[i], li, uiContext);
        
        ul.appendChild(li);
    }
    this._dom.bodyDiv.appendChild(ul);

    this._div.style.display = "block";
};
