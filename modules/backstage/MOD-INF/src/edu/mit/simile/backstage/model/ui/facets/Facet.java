package edu.mit.simile.backstage.model.ui.facets;

import edu.mit.simile.backstage.model.Component;
import edu.mit.simile.backstage.model.Context;

abstract public class Facet extends Component {
    protected Facet(Context context, String id) {
        super(context, id);
    }

}
