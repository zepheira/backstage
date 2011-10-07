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
    var configuration, view;
    configuration = Exhibit.getConfigurationFromDOM(configElmt);
    view = new Backstage.TileView(
        (typeof containerElmt !== "undefined" && containerElmt !== null) ?
            containerElmt :
            configElmt,
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
    $(this._div).empty();

    this._dom = null;

    this._div = null;
    this._uiContext = null;
};

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
