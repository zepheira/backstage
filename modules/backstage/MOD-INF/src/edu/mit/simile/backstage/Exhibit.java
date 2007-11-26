package edu.mit.simile.backstage;

import java.util.LinkedList;
import java.util.List;


public class Exhibit extends BackstageScriptableObject {
    private static final long serialVersionUID = -8804204966521106254L;
    
    final private ExhibitIdentity           m_identity;
    final private List<AccessedDataLink>    m_dataLinks;
    
    private int m_referenceCount;
    
    public Exhibit(ExhibitIdentity identity, List<AccessedDataLink> dataLinks) {
        m_identity = identity;
        m_dataLinks = new LinkedList<AccessedDataLink>(dataLinks);
    }
    
    @Override
    public String getClassName() {
        return "Exhibit";
    }

    public ExhibitIdentity getIdentity() {
        return m_identity;
    }
    
    public int getReferenceCount() {
        return m_referenceCount;
    }
    
    public void addReference() {
        m_referenceCount++;
    }
    
    public void removeReference() {
        m_referenceCount--;
    }
    
}
