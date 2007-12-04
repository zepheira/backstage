package edu.mit.simile.backstage;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

/**
 * The identity of an exhibit is its URL if it is public, e.g., http://foo.com/file.html.
 * Otherwise, if it is private, e.g., http://127.0.0.1:8000/..., file:///C:/dev/file.html,
 * http://intranet-site/file.html, then its identity also depends on the remote host--that is,
 * the requesting client.
 * 
 * An exhibit can be private so long as its data links are public. If any of its data links is
 * private, then Backstage cannot host it. 
 */
abstract public class ExhibitIdentity {
    final protected URL _refererURL;
    
    protected ExhibitIdentity(URL refererURL) {
        _refererURL = refererURL;
    }
    
    public URL getURL() {
        return _refererURL;
    }
    
    static public ExhibitIdentity create(HttpServletRequest request) throws MalformedURLException {
        URL refererURL;
        try {
            refererURL = new URL(request.getHeader("Referer"));
        } catch (Exception e) {
            // TODO: there is still a problem because the user can have several local
            // exhibits in different files, which are right now indistinguishable.
            refererURL = new URL("http://127.0.0.1/");
        }
        
        String  protocol = refererURL.getProtocol();
        String  host = refererURL.getHost(); 
        
        if (protocol.equalsIgnoreCase("file") ||
            ((protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) &&
             (host.equalsIgnoreCase("127.0.0.1") || 
              host.equalsIgnoreCase("localhost") ||
              host.indexOf('.') < 0))) {
            return new PrivateExhibitIdentity(refererURL, request.getRemoteHost());
        } else {
            return new PublicExhibitIdentity(refererURL);
        }
    }
    
    static private class PrivateExhibitIdentity extends ExhibitIdentity { 
        final private String  _remoteHost;
        
        private PrivateExhibitIdentity(URL refererURL, String remoteHost) {
            super(refererURL);
            _remoteHost = remoteHost;
        }
        
        @Override
        public int hashCode() {
            return _refererURL.hashCode() ^ _remoteHost.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PrivateExhibitIdentity) {
                PrivateExhibitIdentity id = (PrivateExhibitIdentity) obj;
                
                return id._refererURL.equals(_refererURL) && id._remoteHost.equals(_remoteHost);
            }
            return false;
        }
        
        @Override
        public String toString() {
            return "Exhibit " + _refererURL.toExternalForm() + " private to " + _remoteHost;
        }
    }
    
    static private class PublicExhibitIdentity extends ExhibitIdentity { 
        private PublicExhibitIdentity(URL refererURL) {
            super(refererURL);
        }
        
        @Override
        public int hashCode() {
            return _refererURL.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PublicExhibitIdentity) {
                PublicExhibitIdentity id = (PublicExhibitIdentity) obj;
                
                return id._refererURL.equals(_refererURL);
            }
            return false;
        }
        
        @Override
        public String toString() {
            return "Exhibit " + _refererURL.toExternalForm();
        }
    }
}
