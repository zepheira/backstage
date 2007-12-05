/*======================================================================
 *  Backstage auto-create
 *  You can avoid running this code by adding the URL parameter
 *  autoCreate=false when you include exhibit-api.js.
 *======================================================================
 */
$(document).ready(function() { 
    var fDone = function() {
        alert("loaded data");
        //window.exhibit = Exhibit.create();
        //window.exhibit.configureFromDOM();
    };
    var fDone2 = function() {
        window.backstage.loadDataLinks(fDone);
    };
    
    window.backstage = Backstage.create(fDone2);
});
