/*======================================================================
 *  Lens
 *======================================================================
 */
Exhibit.LensRegistry.prototype.getServerSideConfiguration = function(uiContext) {
    var r, t;
    r = {
        defaultLens: null,
        typeToLens: {}
    };
    
    if (this._defaultLens !== null) {
        r.defaultLens = Exhibit.LensRegistry._getTemplateNodeServerSideConfiguration(
            Exhibit.LensRegistry._getCompiledLens(this._defaultLens, uiContext));
    }
    
    for (t in this._typeToLens) {
        if (this._typeToLens.hasOwnProperty(t)) {
            r.typeToLens[t] = Exhibit.LensRegistry._getTemplateNodeServerSideConfiguration(
                Exhibit.LensRegistry._getCompiledLens(this._typeToLens[t], uiContext));
        }
    }
    
    return r;
};

Exhibit.LensRegistry.prototype.getLensFromServerNode = function(serverNode) {
    var type = serverNode.itemType;
    if (typeof this._typeToLens[type] !== "undefined") {
        return this._typeToLens[type];
    }
    if (this._defaultLens !== null) {
        return this._defaultLens;
    }
    if (this._parentRegistry) {
        return this._parentRegistry.getLensFromServerNode(serverNode);
    }
    return null;
};

Exhibit.LensRegistry.prototype.createLensFromBackstage = function(serverNode, div, uiContext) {
    var lens, lensTemplate;
    lens = new Exhibit.Lens();
    lensTemplate = this.getLensFromServerNode(serverNode);
    if (lensTemplate === null) {
        Exhibit.Lens.constructDefaultFromBackstage(serverNode, div, uiContext);
    } else {
        Exhibit.Lens.constructFromBackstage(
            Exhibit.LensRegistry._getCompiledLens(lensTemplate, uiContext), serverNode, div);
    }
    return lens;
};

Exhibit.LensRegistry._getCompiledLens = function(lensTemplateNode, uiContext) {
    var id, compiledTemplate;
    id = $(lensTemplateNode).attr("id");
    if (typeof id === "undefined" || id === null || id.length === 0) {
        id = "exhibitLensTemplate" + String(Math.floor(Math.random() * 10000));
        $(lensTemplateNode).attr("id", id);
    }
    
    compiledTemplate = Exhibit.Lens._compiledTemplates[id];
    if (compiledTemplate === null) {
        compiledTemplate = {
            url:        id,
            template:   Exhibit.Lens.compileTemplate(lensTemplateNode, false, uiContext),
            compiled:   true,
            jobs:       []
        };
        Exhibit.Lens._compiledTemplates[id] = compiledTemplate;
    }
    
    return compiledTemplate.template;
};

Exhibit.LensRegistry._getTemplateNodeServerSideConfiguration = function(node) {
    var r, i, e, a, f, j;

    if (typeof node === "string") {
        return node;
    }
    
    r = {
        uiContext:              node.uiContext.getID(),
        control:                node.control,
        condition:              (typeof node.condition === "undefined" || node.condition === null) ? null : { 
                                    test: node.condition.test, 
                                    expression: node.condition.expression.getServerSideConfiguration() 
                                },
        content:                (typeof node.content === "undefined" || node.content === null) ? null : node.content.getServerSideConfiguration(),
        contentAttributes:      null,
        subcontentAttributes:   null,
        children:               null
    };
    
    if (typeof node.contentAttributes !== "undefined" && node.contentAttributes !== null) {
        r.contentAttributes = [];
        for (i = 0; i < node.contentAttributes.length; i++) {
            e = node.contentAttributes[i];
            r.contentAttributes.push({
                name:       e.name,
                expression: e.expression.getServerSideConfiguration()
            });
        }
    }
    
    if (typeof node.subcontentAttributes !== "undefined" && node.subcontentAttributes !== null) {
        r.subcontentAttributes = [];
        for (i = 0; i < node.subcontentAttributes.length; i++) {
            e = node.subcontentAttributes[i];
            a = {
                name:       e.name,
                fragments:  []
            };
            
            for (j = 0; j < e.fragments.length; j++) {
                f = e.fragments[j];
                if (typeof f === "string") {
                    a.fragments.push(f);
                } else {
                    a.fragments.push(f.getServerSideConfiguration());
                }
            }
            
            r.subcontentAttributes.push(a);
        }
    }
    
    if (node.children !== null) {
        r.children = [];
        for (i = 0; i < node.children.length; i++) {
            r.children.push(Exhibit.LensRegistry._getTemplateNodeServerSideConfiguration(node.children[i]));
        }
    }
    
    return r;
};

Exhibit.Lens.constructDefaultFromBackstage = function(serverNode, div, uiContext) {
    var template, dom, j, propertyValues, propertyID, pair, tr, tdName, tdValues, m, value;
    template = {
        elmt:       div,
        "class":    "exhibit-lens",
        children: [
            {   tag:        "div",
                "class":    "exhibit-lens-title",
                title:      serverNode.label,
                children:   [ 
                    serverNode.label + " (",
                    {   tag:        "a",
                        href:       serverNode.itemURI,
                        target:     "_blank",
                        children:   [ Exhibit.l10n.itemLinkLabel ]
                    },
                    ")"
                ]
            },
            {   tag:        "div",
                "class":    "exhibit-lens-body",
                children: [
                    {   tag:        "table",
                        "class":    "exhibit-lens-properties",
                        field:      "propertiesTable"
                    }
                ]
            }
        ]
    };
    dom = $.simileDOM("template", template);
    $(div).attr("ex:itemID", serverNode.itemID);
    
    if (typeof serverNode.propertyValues !== "undefined") {
        propertyValues = serverNode.propertyValues;
        j = 0;
        
        for (propertyID in propertyValues) {
            if (propertyValues.hasOwnProperty(propertyID)) {
                pair = propertyValues[propertyID];
            
                tr = dom.propertiesTable.insertRow(j++);
                $(tr).attr("class", "exhibit-lens-property");
            
                tdName = tr.insertCell(0);
                $(tdName).attr("class", "exhibit-lens-property-name");
                $(tdName).html(pair.propertyLabel + ": ");
            
                tdValues = tr.insertCell(1);
                $(tdValues).attr("class", "exhibit-lens-property-values");
            
                if (pair.valueType === "item") {
                    for (m = 0; m < pair.values.length; m++) {
                        if (m > 0) {
                            $(tdValues).append(document.createTextNode(", "));
                        }
                        value = pair.values[m];
                    
                        $(tdValues).append(Exhibit.UI.makeItemSpan(value.id, value.label, uiContext));
                    }
                } else {
                    for (m = 0; m < pair.values.length; m++) {
                        if (m > 0) {
                            $(tdValues).append(document.createTextNode(", "));
                        }
                        $(tdValues).append(Exhibit.UI.makeValueSpan(pair.values[m], pair.valueType));
                    }
                }
            }
        }
    }
};

Exhibit.Lens.constructFromBackstage = function(clientNode, serverNode, parentElmt) {
    var elmt = Exhibit.Lens._constructFromBackstage(clientNode, serverNode, parentElmt);
    $(elmt).show();
    return elmt;
};

Exhibit.Lens._constructFromBackstage = function(clientNode, serverNode, parentElmt) {
    var children, elmt, contentAttributes, i, attribute, value, subcontentAttributes, handlers, h, handler, a, results, x, n, r;

    if (typeof clientNode === "string") {
        $(parentElmt).append(document.createTextNode(clientNode));
        return;
    }
    
    children = clientNode.children;
    if (clientNode.condition !== null) {
        if (clientNode.condition["test"] === "if-exists") {
            if (!serverNode.condition) {
                return;
            }
        } else if (clientNode.condition["test"] === "if") {
            // NOT IMPLEMENTED YET
            return;
        } else if (clientNode.condition["test"] === "select") {
            // NOT IMPLEMENTED YET
            return;
        }
    }
    
    elmt = Exhibit.Lens._constructElmtWithAttributes(clientNode, parentElmt, {/*database*/});
    if (typeof clientNode.contentAttributes !== "undefined" && clientNode.contentAttributes !== null) {
        contentAttributes = clientNode.contentAttributes;
        for (i = 0; i < contentAttributes.length; i++) {
            attribute = contentAttributes[i];
            value = serverNode.contentAttributes[i].value;
            if (attribute.isStyle) {
                $(elmt).css(attribute.name, value);
            } else if (Exhibit.Lens._attributeValueIsSafe(attribute.name, value)) {
                $(elmt).attr(attribute.name, value);
            }
        }
    }
    if (typeof clientNode.subcontentAttributes !== "undefined" && clientNode.subcontentAttributes !== null) {
        subcontentAttributes = clientNode.subcontentAttributes;
        for (i = 0; i < subcontentAttributes.length; i++) {
            attribute = subcontentAttributes[i];
            value = serverNode.subcontentAttributes[i].value;
            
            if (attribute.isStyle) {
                $(elmt).css(attribute.name, value);
            } else if (Exhibit.Lens._attributeValueIsSafe(attribute.name, value)) {
                $(elmt).attr(attribute.name, value);
            }
        }
    }
    
    if (!Exhibit.params.safe) {
        handlers = clientNode.handlers;
        for (h = 0; h < handlers.length; h++) {
            handler = handlers[h];
            elmt[handler.name] = handler.code;
        }
    }
    
    if (clientNode.control !== null) {
        switch (clientNode.control) {
        case "item-link":
            a = $("<a>")
                .html(Exhibit.l10n.itemLinkLabel)
                .attr("href", clientNode.itemID)
                .attr("target", "_blank");
            $(elmt).append(a);
            break;
        }
    } else if (typeof clientNode.content !== "undefined" && clientNode.content !== null) {
        results = serverNode.content;
        if (children !== null) {
            for (x = 0; x < results.length; x++) {
                n = results[x];
                for (i = 0; i < children.length; i++) {
                    Exhibit.Lens._constructFromBackstage(children[i], n[i], elmt);
                }
            }
        } else if (results.valueType === "item") {
            // NOT YET IMPLEMENTED
            for (i = 0; i < results.values.length; i++) {
                r = results.values[i];
                
            }
        } else {
            Exhibit.Lens._constructDefaultValueList(
                new Exhibit.Set(results.values), results.valueType, elmt, clientNode.uiContext
            );
        }
    } else if (children !== null) {
        for (i = 0; i < children.length; i++) {
            Exhibit.Lens._constructFromBackstage(children[i], serverNode.children[i], elmt);
        }
    }
    
    return elmt;
};
