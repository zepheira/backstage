/**
 * @fileOveriew
 * @author David Huynh
 * @author <a href="mailto:ryanlee@zepheira.com">Ryan Lee</a>
 */

/**
 * @class
 * @constructor
 */ 
Backstage.UIContext = function() {
    this._parent = null;
    this._id = "context" + String(new Date().getTime()) + String(Math.ceil(Math.random() * 1000));
    
    this._exhibit = null;
    this._collection = null;
    this._lensRegistry = new Exhibit.LensRegistry();
    this._settings = {};
    
    this._formatters = {};
    this._listFormatter = null;
};

/**
 * @static
 * @param {Object} configuration
 * @param {Backstage._Impl} backstage
 */
Backstage.UIContext.createRootContext = function(configuration, backstage) {
    var context, settings, n, formats;
    context = new Backstage.UIContext();
    context._backstage = backstage;
    
    settings = Exhibit.UIContext.l10n.initialSettings;
    for (n in settings) {
        if (settings.hasOwnProperty(n)) {
            context._settings[n] = settings[n];
        }
    }
    
    formats = Exhibit.getAttribute(document.body, "formats");
    if (formats !== null && formats.length > 0) {
        Exhibit.FormatParser.parseSeveral(context, formats, 0, {});
    }
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(
        document.body, Exhibit.UIContext._settingSpecs, context._settings);
        
    Backstage.UIContext._configure(context, configuration);
    
    return context;
};

/**
 * @static
 * @param {Object} configuration
 * @param {Backstage.UIContext} parentUIContext
 * @param {Boolean} ignoreLenses
 */
Backstage.UIContext.create = function(configuration, parentUIContext, ignoreLenses) {
    var context = Backstage.UIContext._createWithParent(parentUIContext);
    Backstage.UIContext._configure(context, configuration, ignoreLenses);
    
    return context;
};

/**
 * @static
 * @param {Element} configElmt
 * @param {Backstage.UIContext} parentUIContext
 * @param {Boolean} ignoreLenses
 */
Backstage.UIContext.createFromDOM = function(configElmt, parentUIContext, ignoreLenses) {
    var context, id, formats;
    context = Backstage.UIContext._createWithParent(parentUIContext);
    
    if (!(ignoreLenses)) {
        Backstage.UIContext.registerLensesFromDOM(configElmt, context.getLensRegistry());
    }
    
    id = Exhibit.getAttribute(configElmt, "collectionID");
    if (id !== null && id.length > 0) {
        context._collection = context._exhibit.getCollection(id);
    }
    
    formats = Exhibit.getAttribute(configElmt, "formats");
    if (formats !== null && formats.length > 0) {
        Exhibit.FormatParser.parseSeveral(context, formats, 0, {});
    }
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(
        configElmt, Exhibit.UIContext._settingSpecs, context._settings);
        
    Backstage.UIContext._configure(context, Exhibit.getConfigurationFromDOM(configElmt), ignoreLenses);
    
    return context;
};

/**
 * Public interface
 */

/**
 * @returns {String}
 */
Backstage.UIContext.prototype.getID = function() {
    return this._id;
};

/**
 * 
 */
Backstage.UIContext.prototype.dispose = function() {
};

/**
 * @returns {Object}
 */
Backstage.UIContext.prototype.getServerSideConfiguration = function() {
    return {
        id: this.getID(),
        lensRegistry: this._lensRegistry.getServerSideConfiguration(this)
    };
};

/**
 * @returns {Backstage.UIContext}
 */
Backstage.UIContext.prototype.getParentUIContext = function() {
    return this._parent;
};

/**
 * @returns {Backstage._Impl}
 */
Backstage.UIContext.prototype.getBackstage = function() {
    return this._backstage;
};

/**
 * @returns {Backstage.Collection}
 */
Backstage.UIContext.prototype.getCollection = function() {
    if (this._collection === null) {
        if (this._parent !== null) {
            this._collection = this._parent.getCollection();
        } else {
            this._collection = this._backstage.getDefaultCollection();
        }
    }
    return this._collection;
};

/**
 * @returns {Exhibit.LensRegistry}
 */
Backstage.UIContext.prototype.getLensRegistry = function() {
    return this._lensRegistry;
};

/**
 * @param {String} name
 * @returns {}
 */
Backstage.UIContext.prototype.getSetting = function(name) {
    return typeof this._settings[name] !== "undefined" ? 
        this._settings[name] : 
        (this._parent !== null ? this._parent.getSetting(name) : undefined);
};

/**
 * @param {String} name
 * @param {Boolean} defaultValue
 * @returns {Boolean}
 */
Backstage.UIContext.prototype.getBooleanSetting = function(name, defaultValue) {
    var v = this.getSetting(name);
    return (typeof v === "undefined" || v === null) ? defaultValue : v;
};

/**
 * @param {String} name
 * @param {} value
 */
Backstage.UIContext.prototype.putSetting = function(name, value) {
    this._settings[name] = value;
};

/**
 * @param {} value
 * @param {String} valueType
 * @param {Function} appender
 */
Backstage.UIContext.prototype.format = function(value, valueType, appender) {
    var f;
    if (typeof this._formatters[valueType] !== "undefined") {
        f = this._formatters[valueType];
    } else {
        f = this._formatters[valueType] = 
            new Exhibit.Formatter._constructors[valueType](this);
    }
    f.format(value, appender);
};

/**
 * @param {Function} iterator
 * @param {Number} count
 * @param {String} valueType
 * @param {Function} appender
 */
Backstage.UIContext.prototype.formatList = function(iterator, count, valueType, appender) {
    if (this._listFormatter === null) {
        this._listFormatter = new Exhibit.Formatter._ListFormatter(this);
    }
    this._listFormatter.formatList(iterator, count, valueType, appender);
};

/**
 * @param {Element} elmt
 */
Backstage.UIContext.prototype.registerLensFromDOM = function(elmt) {
    Backstage.UIContext.registerLensFromDOM(elmt, this._lensRegistry);
};

/*----------------------------------------------------------------------
 *  Internal implementation
 *----------------------------------------------------------------------
 */

/**
 * @private
 * @static
 * @param {Backstage.UIContext} parent
 * @returns {Backstage.UIContext}
 */
Backstage.UIContext._createWithParent = function(parent) {
    var context = new Backstage.UIContext();
    
    context._parent = parent;
    context._exhibit = parent._exhibit;
    context._backstage = parent._backstage;
    context._lensRegistry = new Exhibit.LensRegistry(parent.getLensRegistry());
    
    return context;
};

/**
 * @private
 * @constant
 */
Backstage.UIContext._settingSpecs = {
    "bubbleWidth":      { type: "int" },
    "bubbleHeight":     { type: "int" }
};

/**
 * @private
 * @static
 * @param {Backstage.UIContext} context
 * @param {Object} configuration
 * @param {Boolean} ignoreLenses
 */
Backstage.UIContext._configure = function(context, configuration, ignoreLenses) {
    Backstage.UIContext.registerLenses(configuration, context.getLensRegistry());
    
    if (typeof configuration.collectionID !== "undefined") {
        context._collection = context._exhibit.getCollection(configuration.collectionID);
    }
    
    if (typeof configuration.formats !== "undefined") {
        Exhibit.FormatParser.parseSeveral(context, configuration.formats, 0, {});
    }
    
    if (!(ignoreLenses)) {
        Exhibit.SettingsUtilities.collectSettings(
            configuration, Exhibit.UIContext._settingSpecs, context._settings);
    }
};

/*----------------------------------------------------------------------
 *  Lens utility functions for internal use
 *----------------------------------------------------------------------
 */

/**
 * @static
 * @param {Object} configuration
 * @param {Exhibit.LensRegistry} lensRegistry
 */
Backstage.UIContext.registerLens = function(configuration, lensRegistry) {
    var template, i;
    template = configuration.templateFile;
    if (typeof template !== "undefined" && template !== null) {
        if (typeof configuration.itemTypes !== "undefined") {
            for (i = 0; i < configuration.itemTypes.length; i++) {
                lensRegistry.registerLensForType(template, configuration.itemTypes[i]);
            }
        } else {
            lensRegistry.registerDefaultLens(template);
        }
    }
};

/**
 * @static
 * @param {Element} elmt
 * @param {Exhibit.LensRegistry} lensRegistry
 */
Backstage.UIContext.registerLensFromDOM = function(elmt, lensRegistry) {
    var itemTypes, template, url, id, elmt2, i;

    $(elmt).hide();
    
    itemTypes = Exhibit.getAttribute(elmt, "itemTypes", ",");
    template = null;
    
    url = Exhibit.getAttribute(elmt, "templateFile");
    if (url !== null && url.length > 0) {
        template = url;
    } else {
        id = Exhibit.getAttribute(elmt, "template");
        elmt2 = $("#" + id);
        if (elmt2.length > 0) {
            template = elmt2.get(0);
        } else {
            template = elmt;
        }
    }
    
    if (template !== null) {
        if (itemTypes === null || itemTypes.length === 0 || (itemTypes.length === 1 && itemTypes[0] === "")) {
            lensRegistry.registerDefaultLens(template);
        } else {
            for (i = 0; i < itemTypes.length; i++) {
                lensRegistry.registerLensForType(template, itemTypes[i]);
            }
        }
    }
};

/**
 * @static
 * @param {Object} configuration
 * @param {Exhibit.LensRegistry} lensRegistry
 */
Backstage.UIContext.registerLenses = function(configuration, lensRegistry) {
    var i, lensSelector;
    if (typeof configuration.lenses !== "undefined") {
        for (i = 0; i < configuration.lenses.length; i++) {
            Backstage.UIContext.registerLens(configuration.lenses[i], lensRegistry);
        }
    }
    if (typeof configuration.lensSelector !== "undefined") {
        lensSelector = configuration.lensSelector;
        if (typeof lensSelector === "function") {
            lensRegistry.addLensSelector(lensSelector);
        } else {
            Exhibit.Debug.log("lensSelector is not a function");
        }
    }
};

/**
 * @static
 * @param {Element} parentNode
 * @param {Exhibit.LensRegistry} lensRegistry
 */
Backstage.UIContext.registerLensesFromDOM = function(parentNode, lensRegistry) {
    var role, lensSelectorString, lensSelector;
    $(parentNode).children().each(function(idx, el) {
        role = Exhibit.getRoleAttribute(el);
        if (role === "lens") {
            Backstage.UIContext.registerLensFromDOM(el, lensRegistry);
        }
    });
    
    lensSelectorString = Exhibit.getAttribute(parentNode, "lensSelector");
    if (lensSelectorString !== null && lensSelectorString.length > 0) {
        try {
            lensSelector = eval(lensSelectorString);
            if (typeof lensSelector === "function") {
                lensRegistry.addLensSelector(lensSelector);
            } else {
                Exhibit.Debug.log("lensSelector expression " + lensSelectorString + " is not a function");
            }
        } catch (e) {
            Exhibit.Debug.exception(e, "Bad lensSelector expression: " + lensSelectorString);
        }
    }
};

/**
 * @static
 * @param {Object} configuration
 * @param {Exhibit.LensRegistry} parentLensRegistry
 * @returns {Exhibit.LensRegistry}
 */
Backstage.UIContext.createLensRegistry = function(configuration, parentLensRegistry) {
    var lensRegistry = new Exhibit.LensRegistry(parentLensRegistry);
    Backstage.UIContext.registerLenses(configuration, lensRegistry);
    
    return lensRegistry;
};

/**
 * @static
 * @param {Element} parentNode
 * @param {Object} configuration
 * @param {Exhibit.LensRegistry} parentLensRegistry
 * @returns {Exhibit.LensRegistry}
 */
Backstage.UIContext.createLensRegistryFromDOM = function(parentNode, configuration, parentLensRegistry) {
    var lensRegistry = new Exhibit.LensRegistry(parentLensRegistry);
    Backstage.UIContext.registerLensesFromDOM(parentNode, lensRegistry);
    Backstage.UIContext.registerLenses(configuration, lensRegistry);
    
    return lensRegistry;
};
