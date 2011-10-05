/*======================================================================
 *  Backstage auto-create
 *  You can avoid running this code by adding the URL parameter
 *  autoCreate=false when you include exhibit-api.js.
 *======================================================================
 */
$(document).ready(function() { 
    var configureFromDOM, loadDataLinks;

    configureFromDOM = function() {
        window.backstage.configureFromDOM(
            document.body, 
            function() { Exhibit.UI.hideBusyIndicator(); }
        );
    };
    
    loadDataLinks = function() {
        window.backstage.loadDataLinks(configureFromDOM);
    };

    Exhibit.UI.showBusyIndicator();
    window.backstage = Backstage.create(loadDataLinks);
});
