//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

import client.editem.ItemEditor;
import client.util.RowPanel;

/**
 * Displays information on a sub-item.
 */
public class SubItemEntry extends ItemEntry
    implements DoListItemPopup.ListedListener
{
    public SubItemEntry (SubItemPanel spanel, ItemPanel panel, Item item)
    {
        super(panel, item, null);
        _spanel = spanel;
    }

    // @Override // from ItemEntry
    public void setItem (Item item)
    {
        super.setItem(item);

        SubItem sitem = (SubItem)item;
        getFlexCellFormatter().setRowSpan(0, 0, 3);
        setText(1, 0, sitem.ident);
        getFlexCellFormatter().setStyleName(1, 0, "Ident");

        RowPanel buttons = new RowPanel();
        String btitle = (item.catalogId == 0) ?
            CInventory.msgs.detailList() : CInventory.msgs.detailUplist();
        Button button = new Button(btitle, new ClickListener() {
            public void onClick (Widget sender) {
                new DoListItemPopup(_item, null, SubItemEntry.this).show();
            }
        });
        button.addStyleName("tinyButton");
        buttons.add(button);

// TODO
//         button = new Button(CInventory.msgs.detailEdit(), new ClickListener() {
//             public void onClick (Widget sender) {
//                 ItemEditor editor = ItemEditor.createItemEditor(_item.getType(), _spanel);
//                 editor.setItem(_item);
//                 editor.show();
//             }
//         });
//         button.addStyleName("tinyButton");
//         buttons.add(button);

        setWidget(2, 0, buttons);
    }

    // from DoListItemPopup.ListedListener
    public void itemListed (Item item, boolean updated)
    {
        // if this was a first time listing, reset our item so that our "List..." button becomes
        // "Update listing..."
        if (!updated) {
            setItem(item);
        }
    }

    protected SubItemPanel _spanel;
}
