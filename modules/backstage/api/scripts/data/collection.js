/*======================================================================
 *  Collection
 *======================================================================
 */
Backstage.Collection = function(id, database) {
    this._id = id;
    this._database = database;
    
    this._listeners = new SimileAjax.ListenerQueue();
    this._facets = [];
    this._updating = false;
    
    this._items = null;
    this._restrictedItems = null;
};

Backstage.Collection.create = function(id, configuration, backstage) {
    var collection = new Backstage.Collection(id, backstage);
    
    if ("itemTypes" in configuration) {
        collection._itemTypes = configuration.itemTypes;
        collection._update = Backstage.Collection._typeBasedCollection_update;
    } else {
        collection._update = Backstage.Collection._allItemsCollection_update;
    }
    
    return collection;
};

Backstage.Collection.create2 = function(id, configuration, uiContext) {
    var backstage = uiContext.getBackstage();
    
    if ("expression" in configuration) {
        var collection = new Backstage.Collection(id, backstage);
        
        collection._expression = Exhibit.ExpressionParser.parse(configuration.expression);
        collection._baseCollection = ("baseCollectionID" in configuration) ? 
            uiContext.getExhibit().getCollection(configuration.baseCollectionID) : 
            uiContext.getCollection();
        
        return collection;
    } else {
        return Backstage.Collection.create(id, configuration, backstage);
    }
};

Backstage.Collection.createFromDOM = function(id, elmt, backstage) {
    var collection = new Backstage.Collection(id, backstage);
    
    var itemTypes = Exhibit.getAttribute(elmt, "itemTypes", ",");
    if (itemTypes != null && itemTypes.length > 0) {
        collection._itemTypes = itemTypes;
        collection._update = Backstage.Collection._typeBasedCollection_update;
    } else {
        collection._update = Backstage.Collection._allItemsCollection_update;
    }
    
    return collection;
};

Backstage.Collection.createFromDOM2 = function(id, elmt, uiContext) {
    var backstage = uiContext.getBackstage();
    
    var expressionString = Exhibit.getAttribute(elmt, "expression");
    if (expressionString != null && expressionString.length > 0) {
        var collection = new Backstage.Collection(id, backstage);
    
        collection._expression = Exhibit.ExpressionParser.parse(expressionString);
        
        var baseCollectionID = Exhibit.getAttribute(elmt, "baseCollectionID");
        collection._baseCollection = (baseCollectionID != null && baseCollectionID.length > 0) ? 
            uiContext.getExhibit().getCollection(baseCollectionID) : 
            uiContext.getCollection();
            
        return collection;
    } else {
        return Backstage.Collection.createFromDOM(id, elmt, backstage);
    }
};

Backstage.Collection.createAllItemsCollection = function(id, backstage) {
    var collection = new Backstage.Collection(id, backstage);
    collection._type = "all-items";
    
    return collection;
};

/*======================================================================
 *  Implementation
 *======================================================================
 */
Backstage.Collection._allItemsCollection_update = function() {
    this._items = this._database.getAllItems();
    this._onRootItemsChanged();
};

Backstage.Collection._typeBasedCollection_update = function() {
    var newItems = new Exhibit.Set();
    for (var i = 0; i < this._itemTypes.length; i++) {
        this._database.getSubjects(this._itemTypes[i], "type", newItems);
    }
    
    this._items = newItems;
    this._onRootItemsChanged();
};

Backstage.Collection._basedCollection_update = function() {
    this._items = this._expression.evaluate(
        { "value" : this._baseCollection.getRestrictedItems() }, 
        { "value" : "item" }, 
        "value",
        this._database
    ).values;
    
    this._onRootItemsChanged();
};

Backstage.Collection.prototype.getID = function() {
    return this._id;
};

Backstage.Collection.prototype.dispose = function() {
    if ("_baseCollection" in this) {
        this._baseCollection.removeListener(this._listener);
        this._baseCollection = null;
        this._expression = null;
    } else {
        this._database.removeListener(this._listener);
    }
    this._database = null;
    this._listener = null;
    
    this._listeners = null;
    this._items = null;
    this._restrictedItems = null;
};

Backstage.Collection.prototype.getServerSideConfiguration = function() {
    return {
        id:     this._id,
        type:   this._type
    };
};

Backstage.Collection.prototype.addListener = function(listener) {
    this._listeners.add(listener);
};

Backstage.Collection.prototype.removeListener = function(listener) {
    this._listeners.remove(listener);
};

Backstage.Collection.prototype.addFacet = function(facet) {
    this._facets.push(facet);
    
    if (facet.hasRestrictions()) {
        this._computeRestrictedItems();
        this._updateFacets(null);
        this._listeners.fire("onItemsChanged", []);
    } else {
        facet.update(this.getRestrictedItems());
    }
};

Backstage.Collection.prototype.removeFacet = function(facet) {
    for (var i = 0; i < this._facets.length; i++) {
        if (facet == this._facets[i]) {
            this._facets.splice(i, 1);
            if (facet.hasRestrictions()) {
                this._computeRestrictedItems();
                this._updateFacets(null);
                this._listeners.fire("onItemsChanged", []);
            }
            break;
        }
    }
};

Backstage.Collection.prototype.clearAllRestrictions = function() {
    var restrictions = [];
    
    this._updating = true;
    for (var i = 0; i < this._facets.length; i++) {
        restrictions.push(this._facets[i].clearAllRestrictions());
    }
    this._updating = false;
    
    this.onFacetUpdated(null);
    
    return restrictions;
};

Backstage.Collection.prototype.applyRestrictions = function(restrictions) {
    this._updating = true;
    for (var i = 0; i < this._facets.length; i++) {
        this._facets[i].applyRestrictions(restrictions[i]);
    }
    this._updating = false;
    
    this.onFacetUpdated(null);
};

Backstage.Collection.prototype.getAllItems = function() {
    return new Exhibit.Set(this._items);
};

Backstage.Collection.prototype.countAllItems = function() {
    return this._items.size();
};

Backstage.Collection.prototype.getRestrictedItems = function() {
    return new Exhibit.Set(this._restrictedItems);
};

Backstage.Collection.prototype.countRestrictedItems = function() {
    return this._restrictedItems.size();
};

Backstage.Collection.prototype.onFacetUpdated = function(facetChanged) {
    if (!this._updating) {
        this._computeRestrictedItems();
        this._updateFacets(facetChanged);
        this._listeners.fire("onItemsChanged", []);
    }
}

Backstage.Collection.prototype._onRootItemsChanged = function() {
    this._listeners.fire("onRootItemsChanged", []);
    
    this._computeRestrictedItems();
    this._updateFacets(null);
    
    this._listeners.fire("onItemsChanged", []);
};

//Exhibit.Collection = Backstage.Collection;