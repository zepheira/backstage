package edu.mit.simile.backstage;

import java.net.URL;
import java.util.Date;

import javax.servlet.ServletConfig;

import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheManager;

import edu.mit.simile.butterfly.ButterflyModuleImpl;

public class BackstageModule extends ButterflyModuleImpl {
    static Cache s_exhibits;
    
    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);
        if (s_exhibits == null) {
            s_exhibits = CacheManager.getInstance().getCache();
        }
    }
    
    public Exhibit getExhibit(URL exhibitURL, String lastModified) {
        Date d = Utilities.parseLastModifiedDate(lastModified);
        CompositeKey key = new CompositeKey(exhibitURL, d != null ? d : new Date());
        
        Exhibit exhibit = (Exhibit) s_exhibits.retrieve(key);
        if (exhibit == null) {
            exhibit = new Exhibit();
            s_exhibits.store(key, exhibit);
        }
        return exhibit;
    }
}