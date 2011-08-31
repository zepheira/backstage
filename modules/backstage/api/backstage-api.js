/*==================================================
 *  Simile Backstage API
 *==================================================
 */

(function() {
    var loadMe = function() {
        if (typeof window.Backstage != "undefined") {
            return;
        }
        
        window.Backstage = {
            loaded:     false,
            params:     { bundle: false, autoCreate: true },
            namespace:  "http://simile.mit.edu/2006/11/backstage#",
            locales:    [ "en" ]
        };
        
        var javascriptFiles = [
            "backstage.js",
            "util/sha1.js",
            "util/json.js",
            "util/jsonp-transport.js",
            "util/job-queue.js",
            "data/collection.js",
            "data/expression.js",
            "ui/ui.js",
            "ui/ui-context.js",
            "ui/lens.js",
            "ui/views/tile-view.js",
            "ui/facets/list-facet.js"
        ];
        var cssFiles = [
            "backstage.css",
        ];
        
        /**
         * commenting out for now, would be good to tie to exhibit 3 locale
         * no locales exist yet, so this is doing nothing
        var defaultClientLocales = ("language" in navigator ? navigator.language : navigator.browserLanguage).split(";");
        for (var l = 0; l < defaultClientLocales.length; l++) {
            var locale = defaultClientLocales[l];
            if (locale != "en") {
                var segments = locale.split("-");
                if (segments.length > 1 && segments[0] != "en") {
                    Backstage.locales.push(segments[0]);
                }
                Backstage.locales.push(locale);
            }
        }
        */

        var paramTypes = { bundle:Boolean, autoCreate:Boolean };
        if (typeof Backstage_urlPrefix == "string") {
            Backstage.urlPrefix = Backstage_urlPrefix;
            if ("Backstage_parameters" in window) {
                Exhibit.parseURLParameters(Backstage_parameters,
                                           Backstage.params,
                                           paramTypes);
            }
        } else {
            var url = null;
            var scripts = document.getElementsByTagName("script");
            for (var i = 0; i < scripts.length; i++) {
                var script = scripts[i];
                if (script.hasAttribute("src")) {
                    var url = script.getAttribute("src");
                    if (url.indexOf("/backstage-api.js") >= 0 && url.indexOf("/exhibit-api.js") === -1) {
                        Backstage.urlPrefix = url.substr(0, url.indexOf("backstage-api.js"));
                        break;
                    }
                }
            }
            if (url == null) {
                Backstage.error = new Error("Failed to derive URL prefix for Simile Backstage API code files");
                return;
            }
            Backstage.urlPrefix = url.substr(0, url.indexOf("backstage-api.js"));
            Exhibit.parseURLParameters(url, Backstage.params, paramTypes);
        }

        /**
         * commenting out for now, would be good to tie to exhibit 3 locale
         * no locales exist yet, so this is doing nothing
        if (Backstage.params.locale) { // ISO-639 language codes,
            // optional ISO-3166 country codes (2 characters)
            if (Backstage.params.locale != "en") {
                var segments = Backstage.params.locale.split("-");
                if (segments.length > 1 && segments[0] != "en") {
                    Backstage.locales.push(segments[0]);
                }
                Backstage.locales.push(Backstage.params.locale);
            }
        }
        */

        var scriptURLs = Backstage.params.js || [].concat(javascriptFiles);
        var cssURLs = Backstage.params.css || [].concat(cssFiles);

        /**
         * no bundling
        if (Backstage.params.bundle) {
            scriptURLs.push(Backstage.urlPrefix + "backstage-bundle.js");
            cssURLs.push(Backstage.urlPrefix + "backstage-bundle.css");
        } else {
            SimileAjax.prefixURLs(scriptURLs, Backstage.urlPrefix + "scripts/", javascriptFiles);
            SimileAjax.prefixURLs(cssURLs, Backstage.urlPrefix + "styles/", cssFiles);
        }
        */

        /*
         *  Localization
         */
        /**
         * commenting out for now, would be good to tie to exhibit 3 locale
         * no locales exist yet, so this is doing nothing
        for (var i = 0; i < Backstage.locales.length; i++) {
            scriptURLs.push(Backstage.urlPrefix + "locales/" + Backstage.locales[i] + "/locale.js");
        };
        */
        
        /*
         *  Autocreate
         */
        if (Backstage.params.autoCreate) {
            scriptURLs.push("create.js");
        }

        for (var i = 0; i < cssURLs.length; i++) {
            $("head:eq(0)").append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + Backstage.urlPrefix + "styles/" + cssURLs[i] + "\" />");
        }
        for (var i = 0; i < scriptURLs.length; i++) {
            $LAB.script(Backstage.urlPrefix + "scripts/" + scriptURLs[i]);
        }
        Backstage.loaded = true;
    };

    $(document).one("scriptsLoaded.exhibit", loadMe);
}());
