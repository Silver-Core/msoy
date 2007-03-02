//
// $Id$
package client.inventory;

import client.util.BorderedDialog;
import client.util.ClickCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;

public class DoListItemPopup extends BorderedDialog
{
    public DoListItemPopup (Item item, final Button parentButton, final Label parentStatus)
    {
        super(false, true);
        addStyleName("doListItem");
        _item = item;

        _status = new Label("");
        _status.addStyleName("Status");

        parentButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender)
            {
                // make sure the item is kosher; TODO: make this less of a hack
                if (_item.name.trim().length() == 0) {
                    parentStatus.setText(CInventory.msgs.errItemMissingName());
                    return;
                }
                if (_item.description.trim().length() == 0) {
                    parentStatus.setText(CInventory.msgs.errItemMissingDescrip());
                    return;
                }
                parentButton.setEnabled(false);
                show();

            }
        });

        String title = CInventory.msgs.doListHdrTop(Item.getTypeName(item.getType()));
        _header.add(createTitleLabel(title, null));
        HTML blurb = new HTML(CInventory.msgs.doListBlurb());
        blurb.addStyleName("Blurb");
        _content.add(blurb);

        FlowPanel flow = new FlowPanel();
        flow.addStyleName("RarityRow");
        flow.add(new InlineLabel(CInventory.msgs.doListHdrRarity(), false, false, true));

        final ListBox rarityBox = new ListBox();
        rarityBox.addStyleName("RarityBox");

        rarityBox.addItem(CInventory.msgs.doListRarityPlentiful());
        rarityBox.addItem(CInventory.msgs.doListRarityCommon());
        rarityBox.addItem(CInventory.msgs.doListRarityNormal());
        rarityBox.addItem(CInventory.msgs.dolistRarityUncommon());
        rarityBox.addItem(CInventory.msgs.doListRarityRare());

        rarityBox.setSelectedIndex(2);
        rarityBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                ListBox box = (ListBox) sender;
                _priceBox.setText(String.valueOf(_prices[box.getSelectedIndex()]));
            }
        });
        flow.add(rarityBox);
        _content.add(flow);

        flow = new FlowPanel();
        flow.addStyleName("PriceRow");
        flow.add(new InlineLabel(CInventory.msgs.doListHdrPrice(), false, false, true));

        flow.add(new Image("/images/header/symbol_flow.png"));
        _priceBox = new Label(String.valueOf(_prices[2]));
        _priceBox.addStyleName("Price");
        flow.add(_priceBox);
        _content.add(flow);

        _content.add(_status);

        Button cancel = new Button(CInventory.msgs.doListBtnCancel());
        cancel.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                parentButton.setEnabled(true);
                hide();
            }
        });
        _footer.add(cancel);

        Button listIt = new Button(CInventory.msgs.doListBtnListIt());
        new ClickCallback(listIt, _status) {
            public boolean callService () {
                int rarity = _rarities[Math.max(0, rarityBox.getSelectedIndex())];
                CInventory.catalogsvc.listItem(
                    CInventory.creds, _item.getIdent(), rarity, true, this);
                return true;
            }
            public boolean gotResult (Object result) {
                parentStatus.setText(CInventory.msgs.msgItemListed());
                // leave parentButton disabled
                hide();
                return false;
            }
        };
        _footer.add(listIt);
    }

    // @Override
    protected Widget createContents ()
    {
        _content = new VerticalPanel();
        _content.addStyleName("Content");
        _content.setVerticalAlignment(HasAlignment.ALIGN_TOP);
        return _content;
    }

    protected Label _status;
    protected Label _priceBox;
    protected VerticalPanel _content;
    protected Item _item;

    protected static final int[] _rarities = new int[] {
        CatalogListing.RARITY_PLENTIFUL, CatalogListing.RARITY_COMMON,
        CatalogListing.RARITY_NORMAL, CatalogListing.RARITY_UNCOMMON,
        CatalogListing.RARITY_RARE
    };
    
    protected static final int[] _prices = new int[] { 100, 200, 300, 400, 500 };
}
