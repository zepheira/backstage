/*======================================================================
 *  UIContext
 *======================================================================
 */
Backstage.UIContext = function() {
    this._parent = null;
    
    this._exhibit = null;
    this._collection = null;
    this._lensRegistry = new Exhibit.LensRegistry();
    this._settings = {};
    
    this._formatters = {};
    this._listFormatter = null;
};

Backstage.UIContext.createRootContext = function(configuration, backstage) {
    var context = new Backstage.UIContext();
    context._backstage = backstage;
    
    var settings = Exhibit.UIContext.l10n.initialSettings;
    for (var n in settings) {
        context._settings[n] = settings[n];
    }
    
    var formats = Exhibit.getAttribute(document.body, "formats");
    if (formats != null && formats.length > 0) {
        Exhibit.FormatParser.parseSeveral(context, formats, 0, {});
    }
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(
        document.body, Exhibit.UIContext._settingSpecs, context._settings);
        
    Backstage.UIContext._configure(context, configuration);
    
    return context;
};

Backstage.UIContext.create = function(configuration, parentUIContext, ignoreLenses) {
    var context = Backstage.UIContext._createWithParent(parentUIContext);
    Backstage.UIContext._configure(context, configuration, ignoreLenses);
    
    return context;
};

Backstage.UIContext.createFromDOM = function(configElmt, parentUIContext, ignoreLenses) {
    var context = Backstage.UIContext._createWithParent(parentUIContext);
    
    if (!(ignoreLenses)) {
        Backstage.UIContext.registerLensesFromDOM(configElmt, context.getLensRegistry());
    }
    
    var id = Exhibit.getAttribute(configElmt, "collectionID");
    if (id != null && id.length > 0) {
        context._collection = context._exhibit.getCollection(id);
    }
    
    var formats = Exhibit.getAttribute(configElmt, "formats");
    if (formats != null && formats.length > 0) {
        Exhibit.FormatParser.parseSeveral(context, formats, 0, {});
    }
    
    Exhibit.SettingsUtilities.collectSettingsFromDOM(
        configElmt, Exhibit.UIContext._settingSpecs, context._settings);
        
    Backstage.UIContext._configure(context, Exhibit.getConfigurationFromDOM(configElmt), ignoreLenses);
    
    return context;
};

/*----------------------------------------------------------------------
 *  Public interface
 *----------------------------------------------------------------------
 */
Backstage.UIContext.prototype.dispose = function() {
};

Backstage.UIContext.prototype.getServerSideConfiguration = function() {
    return {};
};

Backstage.UIContext.prototype.getParentUIContext = function() {
    return this._parent;
};

Backstage.UIContext.prototype.getBackstage = function() {
    return this._backstage;
};

Backstage.UIContext.prototype.getCollection = function() {
    if (this._collection == null) {
        if (this._parent != null) {
            this._collection = this._parent.getCollection();
        } else {
            this._collection = this._backstage.getDefaultCollection();
        }
    }
    return this._collection;
};

Backstage.UIContext.prototype.getLensRegistry = function() {
    return this._lensRegistry;
};

Backstage.UIContext.prototype.getSetting = function(name) {
    return name in this._settings ? 
        this._settings[name] : 
        (this._parent != null ? this._parent.getSetting(name) : undefined);
};

Backstage.UIContext.prototype.getBooleanSetting = function(name, defaultValue) {
    var v = this.getSetting(name);
    return v == undefined || v == null ? defaultValue : v;
};

Backstage.UIContext.prototype.putSetting = function(name, value) {
    this._settings[name] = value;
};

Backstage.UIContext.prototype.format = function(value, valueType, appender) {
    var f;
    if (valueType in this._formatters) {
        f = this._formatters[valueType];
    } else {
        f = this._formatters[valueType] = 
            new Exhibit.Formatter._constructors[valueType](this);
    }
    f.format(value, appender);
};

Backstage.UIContext.prototype.formatList = function(iterator, count, valueType, appender) {
    if (this._listFormatter == null) {
        this._listFormatter = new Exhibit.Formatter._ListFormatter(this);
    }
    this._listFormatter.formatList(iterator, count, valueType, appender);
};

/*----------------------------------------------------------------------
 *  Internal implementation
 *----------------------------------------------------------------------
 */
Backstage.UIContext._createWithParent = function(parent) {
    var context = new Backstage.UIContext();
    
    context._parent = parent;
    context._exhibit = parent._exhibit;
    context._backstage = parent._backstage;
    context._lensRegistry = new Exhibit.LensRegistry(parent.getLensRegistry());
    
    return context;
};

Backstage.UIContext._settingSpecs = {
    "bubbleWidth":      { type: "int" },
    "bubbleHeight":     { type: "int" }
};

Backstage.UIContext._configure = function(context, configuration, ignoreLenses) {
    Backstage.UIContext.registerLenses(configuration, context.getLensRegistry());
    
    if ("collectionID" in configuration) {
        context._collection = context._exhibit.getCollection(configuration["collectionID"]);
    }
    
    if ("formats" in configuration) {
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
Backstage.UIContext.registerLens = function(configuration, lensRegistry) {
    var template = configuration.templateFile;
    if (template != null) {
        if ("itemTypes" in configuration) {
            for (var i = 0; i < configuration.itemTypes.length; i++) {
                lensRegistry.registerLensForType(template, configuration.itemTypes[i]);
            }
        } else {
            lensRegistry.registerDefaultLens(template);
        }
    }
};

Backstage.UIContext.registerLensFromDOM = function(elmt, lensRegistry) {
    elmt.style.display = "none";
    
    var itemTypes = Exhibit.getAttribute(elmt, "itemTypes", ",");
    var template = null;
    
    var url = Exhibit.getAttribute(elmt, "templateFile");
    if (url != null && url.length > 0) {
        template = url;
    } else {
        var id = Exhibit.getAttribute(elmt, "template");
        var elmt2 = document.getElementById(id);
        if (elmt2 != null) {
            template = elmt2;
        } else {
            template = elmt;
        }
    }
    
    if (template != null) {
        if (itemTypes == null || itemTypes.length == 0 || (itemTypes.length == 1 && itemTypes[0] == "")) {
            lensRegistry.registerDefaultLens(template);
        } else {
            for (var i = 0; i < itemTypes.length; i++) {
                lensRegistry.registerLensForType(template, itemTypes[i]);
            }
        }
    }
};

Backstage.UIContext.registerLenses = function(configuration, lensRegistry) {
    if ("lenses" in configuration) {
        for (var i = 0; i < configuration.lenses.length; i++) {
            Backstage.UIContext.registerLens(configuration.lenses[i], lensRegistry);
        }
    }
    if ("lensSelector" in configuration) {
        var lensSelector = configuration.lensSelector;
        if (typeof lensSelector == "function") {
            lensRegistry.addLensSelector(lensSelector);
        } else {
            SimileAjax.Debug.log("lensSelector is not a function");
        }
    }
};

Backstage.UIContext.registerLensesFromDOM = function(parentNode, lensRegistry) {
    var node = parentNode.firstChild;
    while (node != null) {
        if (node.nodeType == 1) {
            var role = Exhibit.getRoleAttribute(node);
            if (role == "lens") {
                Backstage.UIContext.registerLensFromDOM(node, lensRegistry);
            }
        }
        node = node.nextSibling;
    }
    
    var lensSelectorString = Exhibit.getAttribute(parentNode, "lensSelector");
    if (lensSelectorString != null && lensSelectorString.length > 0) {
        try {
            var lensSelector = eval(lensSelectorString);
            if (typeof lensSelector == "function") {
                lensRegistry.addLensSelector(lensSelector);
            } else {
                SimileAjax.Debug.log("lensSelector expression " + lensSelectorString + " is not a function");
            }
        } catch (e) {
            SimileAjax.Debug.exception(e, "Bad lensSelector expression: " + lensSelectorString);
        }
    }
};

Backstage.UIContext.createLensRegistry = function(configuration, parentLensRegistry) {
    var lensRegistry = new Exhibit.LensRegistry(parentLensRegistry);
    Backstage.UIContext.registerLenses(configuration, lensRegistry);
    
    return lensRegistry;
};

Backstage.UIContext.createLensRegistryFromDOM = function(parentNode, configuration, parentLensRegistry) {
    var lensRegistry = new Exhibit.LensRegistry(parentLensRegistry);
    Backstage.UIContext.registerLensesFromDOM(parentNode, lensRegistry);
    Backstage.UIContext.registerLenses(configuration, lensRegistry);
    
    return lensRegistry;
};
