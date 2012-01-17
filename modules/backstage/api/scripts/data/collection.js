/**
 * @fileOverview Backstage server aware data collections.
 * @author David Huynh
 * @author <a href="mailto:ryanlee@zepheira.com">Ryan Lee</a>
 */

/**
 * @@@ A number of the methods referred to below do not exist, probably
 *     because the intent was to replace Exhibit.Collection with
 *     Backstage.Collection on load, which is commented out.  What
 *     should be done with these calls to nowhere?
 */

/**
 * NB, all Backstage.Collection()._listeners related material is commented
 * out, largely because Backstage does not need listeners to operate (the
 * interaction with the server covers and responds with all changes, so
 * local listeners are not wholly necessary at this stage).  If they come
 * back, look to replace the listener-related material with jQuery events.
 */

/**
 * @class
 * @constructor
 * @param {String} id
 * @param {Exhibit.Database} database
 */ 
Backstage.Collection = function(id, database) {
    this._id = id;
    this._database = database;
    
    //this._listeners = new SimileAjax.ListenerQueue();
    this._facets = [];
    this._updating = false;
    
    this._items = null;
    this._restrictedItems = null;
};

/**
 * @static
 * @param {String} id
 * @param {Object} configuration
 * @param {Backstage._Impl} backstage
 * @returns {Backstage.Collection}
 */
Backstage.Collection.create = function(id, configuration, backstage) {
    var collection = new Backstage.Collection(id, backstage);
    
    if (typeof configuration.itemTypes !== "undefined") {
        collection._type = "types-based";
        collection._itemTypes = configuration.itemTypes;
        collection._update = Backstage.Collection._typeBasedCollection_update;
        collection.getServerSideConfiguration = Backstage.Collection._typeBasedCollection_getServerSideConfiguration;
    } else {
        collection._type = "all-items";
        collection._update = Backstage.Collection._allItemsCollection_update;
        collection.getServerSideConfiguration = Backstage.Collection._allItemsCollection_getServerSideConfiguration;
    }
    
    return collection;
};

/**
 * @static
 * @param {String} id
 * @param {Object} configuration
 * @param {Backstage.UIContext} uiContext
 * @returns {Backstage.Collection}
 */
Backstage.Collection.create2 = function(id, configuration, uiContext) {
    var backstage, collection;
    backstage = uiContext.getMain();
    
    if (typeof configuration.expression !== "undefined") {
        collection = new Backstage.Collection(id, backstage);
        
        collection._expression = Exhibit.ExpressionParser.parse(configuration.expression);
        collection._baseCollection = (typeof configuration.baseCollectionID !== "undefined") ? 
            uiContext.getExhibit().getCollection(configuration.baseCollectionID) : 
            uiContext.getCollection();
        
        return collection;
    } else {
        return Backstage.Collection.create(id, configuration, backstage);
    }
};

/**
 * @static
 * @param {String} id
 * @param {Element} elmt
 * @param {Backstage._Impl} backstage
 * @returns {Backstage.Collection}
 */
Backstage.Collection.createFromDOM = function(id, elmt, backstage) {
    var collection, itemTypes;
    collection = new Backstage.Collection(id, backstage);
    
    itemTypes = Exhibit.getAttribute(elmt, "itemTypes", ",");
    if (itemTypes !== null && itemTypes.length > 0) {
        collection._type = "types-based";
        collection._itemTypes = itemTypes;
        collection._update = Backstage.Collection._typeBasedCollection_update;
        collection.getServerSideConfiguration = Backstage.Collection._typeBasedCollection_getServerSideConfiguration;
    } else {
        collection._type = "all-items";
        collection._update = Backstage.Collection._allItemsCollection_update;
        collection.getServerSideConfiguration = Backstage.Collection._allItemsCollection_getServerSideConfiguration;
    }
    
    return collection;
};

/**
 * @static
 * @param {String} id
 * @param {Element} elmt
 * @param {Backstage.UIContext} uiContext
 * @returns {Backstage.Collection}
 */
Backstage.Collection.createFromDOM2 = function(id, elmt, uiContext) {
    var backstage, expressionString, collection, baseCollectionID;
    backstage = uiContext.getMain();
    
    expressionString = Exhibit.getAttribute(elmt, "expression");
    if (expressionString !== null && expressionString.length > 0) {
        collection = new Backstage.Collection(id, backstage);
    
        collection._expression = Exhibit.ExpressionParser.parse(expressionString);
        
        baseCollectionID = Exhibit.getAttribute(elmt, "baseCollectionID");
        collection._baseCollection = (baseCollectionID !== null && baseCollectionID.length > 0) ? 
            uiContext.getExhibit().getCollection(baseCollectionID) : 
            uiContext.getCollection();
            
        return collection;
    } else {
        return Backstage.Collection.createFromDOM(id, elmt, backstage);
    }
};

/**
 * @static
 * @param {String} id
 * @param {Backstage._Impl} backstage
 * @returns {Backstage.Collection}
 */
Backstage.Collection.createAllItemsCollection = function(id, backstage) {
    var collection = new Backstage.Collection(id, backstage);
    collection._type = "all-items";
    collection._update = Backstage.Collection._allItemsCollection_update;
    collection.getServerSideConfiguration = Backstage.Collection._allItemsCollection_getServerSideConfiguration;
    
    return collection;
};

/*======================================================================
 *  Implementation
 *======================================================================
 */

/**
 * 
 */
Backstage.Collection._allItemsCollection_update = function() {
    this._items = this._database.getAllItems();
    this._onRootItemsChanged();
};

/**
 * @returns {Object}
 */
Backstage.Collection._allItemsCollection_getServerSideConfiguration = function() {
    return {
        id:     this._id,
        type:   this._type
    };
};

/**
 *
 */
Backstage.Collection._typeBasedCollection_update = function() {
    var newItems, i;
    newItems = new Exhibit.Set();
    for (i = 0; i < this._itemTypes.length; i++) {
        this._database.getSubjects(this._itemTypes[i], "type", newItems);
    }
    
    this._items = newItems;
    this._onRootItemsChanged();
};

/**
 * @returns {Object}
 */
Backstage.Collection._typeBasedCollection_getServerSideConfiguration = function() {
    return {
        id:         this._id,
        type:       this._type,
        itemTypes:  this._itemTypes.join(";")
    };
};

/**
 *
 */
Backstage.Collection._basedCollection_update = function() {
    this._items = this._expression.evaluate(
        { "value" : this._baseCollection.getRestrictedItems() }, 
        { "value" : "item" }, 
        "value",
        this._database
    ).values;
    
    this._onRootItemsChanged();
};

/**
 * @returns {String}
 */
Backstage.Collection.prototype.getID = function() {
    return this._id;
};

/**
 *
 */
Backstage.Collection.prototype.dispose = function() {
    if (typeof this._baseCollection !== "undefined") {
        //this._baseCollection.removeListener(this._listener);
        this._baseCollection = null;
        this._expression = null;
    //} else {
        //this._database.removeListener(this._listener);
    }
    this._database = null;
    this._listener = null;
    
    //this._listeners = null;
    this._items = null;
    this._restrictedItems = null;
};

/**
 * 
 */
Backstage.Collection.prototype.addListener = function(listener) {
    //this._listeners.add(listener);
};

/**
 *
 */
Backstage.Collection.prototype.removeListener = function(listener) {
    //this._listeners.remove(listener);
};

/**
 * @param {Backstage.Facet} facet
 */
Backstage.Collection.prototype.addFacet = function(facet) {
    this._facets.push(facet);
    
    if (facet.hasRestrictions()) {
        this._computeRestrictedItems();
        this._updateFacets(null);
        //this._listeners.fire("onItemsChanged", []);
    } else {
        facet.update(this.getRestrictedItems());
    }
};

/**
 * @param {Backstage.Facet} facet
 */
Backstage.Collection.prototype.removeFacet = function(facet) {
    var i;
    for (i = 0; i < this._facets.length; i++) {
        if (facet === this._facets[i]) {
            this._facets.splice(i, 1);
            if (facet.hasRestrictions()) {
                this._computeRestrictedItems();
                this._updateFacets(null);
                //this._listeners.fire("onItemsChanged", []);
            }
            break;
        }
    }
};

/**
 *
 */
Backstage.Collection.prototype.clearAllRestrictions = function() {
    var restrictions, i;
    restrictions = [];
    
    this._updating = true;
    for (i = 0; i < this._facets.length; i++) {
        restrictions.push(this._facets[i].clearAllRestrictions());
    }
    this._updating = false;
    
    this.onFacetUpdated(null);
    
    return restrictions;
};

/**
 * @param {Array} restrictions
 */
Backstage.Collection.prototype.applyRestrictions = function(restrictions) {
    var i;
    this._updating = true;
    for (i = 0; i < this._facets.length; i++) {
        this._facets[i].applyRestrictions(restrictions[i]);
    }
    this._updating = false;
    
    this.onFacetUpdated(null);
};

/**
 * @returns {Exhibit.Set}
 */
Backstage.Collection.prototype.getAllItems = function() {
    return new Exhibit.Set(this._items);
};

/**
 * @returns {Number}
 */
Backstage.Collection.prototype.countAllItems = function() {
    return this._items.size();
};

/**
 * @returns {Exhibit.Set}
 */
Backstage.Collection.prototype.getRestrictedItems = function() {
    return new Exhibit.Set(this._restrictedItems);
};

/**
 * @returns {Number}
 */
Backstage.Collection.prototype.countRestrictedItems = function() {
    return this._restrictedItems.size();
};

/**
 * @param {Backstage.Facet} facetChanged
 */
Backstage.Collection.prototype.onFacetUpdated = function(facetChanged) {
    if (!this._updating) {
        this._computeRestrictedItems();
        this._updateFacets(facetChanged);
        //this._listeners.fire("onItemsChanged", []);
    }
};

/**
 *
 */
Backstage.Collection.prototype._onRootItemsChanged = function() {
    //this._listeners.fire("onRootItemsChanged", []);
    
    this._computeRestrictedItems();
    this._updateFacets(null);
    
    //this._listeners.fire("onItemsChanged", []);
};

//Exhibit.Collection = Backstage.Collection;
