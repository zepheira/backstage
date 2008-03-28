/*======================================================================
 *  Lens
 *======================================================================
 */
Exhibit.LensRegistry.prototype.getServerSideConfiguration = function(uiContext) {
    var r = {
        defaultLens: null,
        typeToLens: {}
    };
    
    if (this._defaultLens != null) {
        r.defaultLens = Exhibit.LensRegistry._getTemplateNodeServerSideConfiguration(
            Exhibit.LensRegistry._getCompiledLens(this._defaultLens, uiContext));
    }
    
    for (var t in this._typeToLens) {
        r.typeToLens[t] = Exhibit.LensRegistry._getTemplateNodeServerSideConfiguration(
            Exhibit.LensRegistry._getCompiledLens(this._typeToLens[t], uiContext));
    }
    
    return r;
};

Exhibit.LensRegistry.prototype.getLensFromServerNode = function(serverNode) {
    var type = serverNode.itemType;
    if (type in this._typeToLens) {
        return this._typeToLens[type];
    }
    if (this._defaultLens != null) {
        return this._defaultLens;
    }
    if (this._parentRegistry) {
        return this._parentRegistry.getLensFromServerNode(serverNode);
    }
    return null;
};

Exhibit.LensRegistry.prototype.createLensFromBackstage = function(serverNode, div, uiContext) {
    var lens = new Exhibit.Lens();
    var lensTemplate = this.getLensFromServerNode(serverNode);
    if (lensTemplate == null) {
        // NOT YET IMPLEMENTED
    } else {
        Exhibit.Lens.constructFromBackstage(
            Exhibit.LensRegistry._getCompiledLens(lensTemplate, uiContext), serverNode, div);
    }
    return lens;
};

Exhibit.LensRegistry._getCompiledLens = function(lensTemplateNode, uiContext) {
    var id = lensTemplateNode.id;
    if (id == null || id.length == 0) {
        id = "exhibitLensTemplate" + Math.floor(Math.random() * 10000);
        lensTemplateNode.id = id;
    }
    
    var compiledTemplate = Exhibit.Lens._compiledTemplates[id];
    if (compiledTemplate == null) {
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
    if (typeof node == "string") {
        return node;
    }
    
    var r = {
        uiContext:              node.uiContext.getID(),
        control:                node.control,
        condition:              (node.condition == null) ? null : { 
                                    test: node.condition.test, 
                                    expression: node.condition.expression.getServerSideConfiguration() 
                                },
        content:                (node.content == null) ? null : node.content.getServerSideConfiguration(),
        contentAttributes:      null,
        subcontentAttributes:   null,
        children:               null
    };
    
    if (node.contentAttributes != null) {
        r.contentAttributes = [];
        for (var i = 0; i < node.contentAttributes.length; i++) {
            var e = node.contentAttributes[i];
            r.contentAttributes.push({
                name:       e.name,
                expression: e.expression.getServerSideConfiguration()
            });
        }
    }
    
    if (node.subcontentAttributes != null) {
        r.subcontentAttributes = [];
        for (var i = 0; i < node.subcontentAttributes.length; i++) {
            var e = node.subcontentAttributes[i];
            var a = {
                name:       e.name,
                fragments:  []
            };
            
            for (var j = 0; j < e.fragments.length; j++) {
                var f = e.fragments[j];
                if (typeof f == "string") {
                    a.fragments.push(f);
                } else {
                    a.fragments.push(f.getServerSideConfiguration());
                }
            }
            
            r.subcontentAttributes.push(a);
        }
    }
    
    if (node.children != null) {
        r.children = [];
        for (var i = 0; i < node.children.length; i++) {
            r.children.push(Exhibit.LensRegistry._getTemplateNodeServerSideConfiguration(node.children[i]));
        }
    }
    
    return r;
};

Exhibit.Lens.constructFromBackstage = function(clientNode, serverNode, parentElmt) {
    var elmt = Exhibit.Lens._constructFromBackstage(clientNode, serverNode, parentElmt);
    elmt.style.display = "block";
    return elmt;
};

Exhibit.Lens._constructFromBackstage = function(clientNode, serverNode, parentElmt) {
    if (typeof clientNode == "string") {
        parentElmt.appendChild(document.createTextNode(clientNode));
        return;
    }
    
    var children = clientNode.children;
    if (clientNode.condition != null) {
        if (templateNode.condition.test == "if-exists" || templateNode.condition.test == "if") {
            if (!serverNode.condition) {
                return;
            }
        } else if (templateNode.condition.test == "select") {
            // NOT IMPLEMENTED YET
            return;
        }
    }
    
    var elmt = Exhibit.Lens._constructElmtWithAttributes(clientNode, parentElmt, {/*database*/});
    if (clientNode.contentAttributes != null) {
        var contentAttributes = clientNode.contentAttributes;
        for (var i = 0; i < contentAttributes.length; i++) {
            var attribute = contentAttributes[i];
            var value = serverNode.contentAttributes[i].value;
            if (attribute.isStyle) {
                elmt.style[attribute.name] = value;
            } else if ("class" == attribute.name) {
                elmt.className = value;
            } else if (Exhibit.Lens._attributeValueIsSafe(attribute.name, value)) {
                elmt.setAttribute(attribute.name, value);
            }
        }
    }
    if (clientNode.subcontentAttributes != null) {
        var subcontentAttributes = clientNode.subcontentAttributes;
        for (var i = 0; i < subcontentAttributes.length; i++) {
            var attribute = subcontentAttributes[i];
            var value = serverNode.subcontentAttributes[i].value;
            
            if (attribute.isStyle) {
                elmt.style[attribute.name] = value;
            } else if ("class" == attribute.name) {
                elmt.className = value;
            } else if (Exhibit.Lens._attributeValueIsSafe(attribute.name, value)) {
                elmt.setAttribute(attribute.name, value);
            }
        }
    }
    
    if (!Exhibit.params.safe) {
        var handlers = clientNode.handlers;
        for (var h = 0; h < handlers.length; h++) {
            var handler = handlers[h];
            elmt[handler.name] = handler.code;
        }
    }
    
    if (clientNode.control != null) {
        switch (clientNode.control) {
        case "item-link":
            var a = document.createElement("a");
            a.innerHTML = Exhibit.l10n.itemLinkLabel;
            a.href = Exhibit.Persistence.getItemLink(roots["value"]);
            a.target = "_blank";
            elmt.appendChild(a);
        }
    } else if (clientNode.content != null) {
        var results = serverNode.content;
        if (children != null) {
            // NOT YET IMPLEMENTED
        } else if (results.valueType == "item") {
            // NOT YET IMPLEMENTED
            for (var i = 0; i < results.values; i++) {
                var r = results.values[i];
                
            }
        } else {
            Exhibit.Lens._constructDefaultValueList(
                new Exhibit.Set(results.values), results.valueType, elmt, clientNode.uiContext
            );
        }
    } else if (children != null) {
        for (var i = 0; i < children.length; i++) {
            Exhibit.Lens._constructFromBackstage(children[i], serverNode.children[i], elmt);
        }
    }
    
    return elmt;
};

