/*======================================================================
 *  Backstage auto-create
 *  You can avoid running this code by adding the URL parameter
 *  autoCreate=false when you include exhibit-api.js.
 *======================================================================
 */
$(document).ready(function() { 
    var loadDataLinks = function() {
        window.backstage.loadDataLinks(configureFromDOM);
    };
    var configureFromDOM = function() {
        window.backstage.configureFromDOM(
            document.body, 
            function() { Exhibit.UI.hideBusyIndicator(); }
        );
    };
    
    Exhibit.UI.showBusyIndicator();
    window.backstage = Backstage.create(loadDataLinks);
});
