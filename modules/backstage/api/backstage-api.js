/**
 * @fileOverview Backstage loading scripts; loads after scriptsLoaded.exhibit
 *     event is fired in Exhibit loading.
 * @author David Huynh
 * @author <a href="mailto:ryanlee@zepheira.com">Ryan Lee</a>
 * @depends jQuery
 * @depends LABjs
 * @depends Exhibit
 */

(function() {
    var loadMe = function() {
        if (typeof window.Backstage !== "undefined" &&
            window.Backstage !== null) {
            return;
        }
        
        window.Backstage = {
            loaded:     false,
            params:     { bundle: false, autoCreate: true },
            namespace:  "http://simile.mit.edu/2006/11/backstage#",
            locales:    [ "en" ]
        };

        var javascriptFiles, cssFiles, paramTypes, url, scriptURLs, cssURLs, i;
        
        javascriptFiles = [
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
        cssFiles = [
            "backstage.css"
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

        paramTypes = { bundle:Boolean, autoCreate:Boolean };
        if (typeof window.Backstage_urlPrefix === "string") {
            Backstage.urlPrefix = window.Backstage_urlPrefix;
            if (typeof window.Backstage_parameters !== "undefined" &&
                window.Backstage_parameters !== null) {
                Exhibit.parseURLParameters(window.Backstage_parameters,
                                           Backstage.params,
                                           paramTypes);
            }
        } else {
            url = null;
            $("script").each(
                function(idx, el) {
                    if (typeof $(this).attr("src") !== "undefined") {
                        url = $(this).attr("src");
                        if (url.indexOf("/backstage-api.js") >= 0 &&
                            url.indexOf("/exhibit-api.js") === -1) {
                            Backstage.urlPrefix = url.substr(0, url.indexOf("backstage-api.js"));
                            Backstage.serverPrefix = Backstage.urlPrefix.substr(0, Backstage.urlPrefix.indexOf("/api"));
                            return false;
                        }
                    }
                }
            );

            if (url === null) {
                Backstage.error = new Error("Failed to derive URL prefix for Simile Backstage API code files");
                return;
            }

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

        scriptURLs = Backstage.params.js || [].concat(javascriptFiles);
        cssURLs = Backstage.params.css || [].concat(cssFiles);

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

        for (i = 0; i < cssURLs.length; i++) {
            $("head:eq(0)").append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + Backstage.urlPrefix + "styles/" + cssURLs[i] + "\" />");
        }
        for (i = 0; i < scriptURLs.length; i++) {
            $LAB.script(Backstage.urlPrefix + "scripts/" + scriptURLs[i]);
        }
        Backstage.loaded = true;
    };

    $(document).one("scriptsLoaded.exhibit", loadMe);
}());
