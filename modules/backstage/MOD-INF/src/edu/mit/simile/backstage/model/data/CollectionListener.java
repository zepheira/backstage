package edu.mit.simile.backstage.model.data;

import java.util.EventListener;

import edu.mit.simile.backstage.model.BackChannel;

public interface CollectionListener extends EventListener {
    public void onItemsChanged(BackChannel backChannel);
}
