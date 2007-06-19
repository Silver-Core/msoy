//
// $Id$

package client.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MemberInviteStatus;

/**
 * Displays the various services available to support and admin personnel.
 */
public class PlayerBrowserPanel extends HorizontalPanel
{
    public PlayerBrowserPanel ()
    {
        setStyleName("playerBrowser");
        setSpacing(10);
        _playerLists = new ArrayList();

        // first, load up the list of players that don't have an inviterId (defaults to 0)
        CAdmin.adminsvc.getPlayerList(CAdmin.ident, 0, new AsyncCallback() {
            public void onSuccess (Object result) {
                _playerLists.add(new PlayerList((List) result));
                forward();
            }
            public void onFailure (Throwable cause) {
                add(new Label(CAdmin.serverError(cause)));
            }
        });
    }

    /**
     * Shifts the panels such that the second panel is shifted to the first position, and the
     * next panel on the list is shifted to the second position.
     */
    protected void forward ()
    {
        int size = _playerLists.size();
        int ii = size - 1;
        for (; ii > 0; ii--) {
            if (getWidgetIndex((Widget) _playerLists.get(ii)) != -1) {
                break;
            }
        }
        clear();
        add((Widget) _playerLists.get(ii));
        if (ii < size - 1) {
            add((Widget) _playerLists.get(ii+1));
        }
    }

    /**
     * Shifts the panels such that the first panel is shifted to the second position, and the
     * panel before it is in the list shifted to the first position.
     */
    protected void back ()
    {
        int ii = 0;
        for (; ii < _playerLists.size() - 1; ii++) {
            if (getWidgetIndex((Widget) _playerLists.get(ii)) != -1) {
                break;
            }
        }
        clear();
        if (ii > 0) {
            add((Widget) _playerLists.get(ii-1));
        }
        add((Widget) _playerLists.get(ii));
    }

    protected class PlayerList extends FlexTable
    {
        public PlayerList (List players)
        {
            setStyleName("PlayerList");
            int row = 0;
            getFlexCellFormatter().setColSpan(row, 1, 3);
            getFlexCellFormatter().addStyleName(row, 1, "Last");
            setText(row++, 1, CAdmin.msgs.browserInvites());
            for (int ii = 0; ii < NUM_COLUMNS; ii++) {
                getFlexCellFormatter().addStyleName(row, ii, "Separator");
            }
            setText(row, 0, CAdmin.msgs.browserName());
            setText(row, 1, CAdmin.msgs.browserAvailable());
            setText(row, 2, CAdmin.msgs.browserUsed());
            getFlexCellFormatter().addStyleName(row, 3, "Last");
            setText(row++, 3, CAdmin.msgs.browserTotal());

            Iterator iter = players.iterator();
            while (iter.hasNext()) {
                final MemberInviteStatus member = (MemberInviteStatus) iter.next();
                Label nameLabel = new Label(member.name);
                nameLabel.addClickListener(new ClickListener() {
                    public void onClick (final Widget sender) {
                        CAdmin.adminsvc.getPlayerList(CAdmin.ident, member.memberId, 
                            new AsyncCallback() {
                                public void onSuccess (Object result) {
                                    if (_activeLabel != null) {
                                        _activeLabel.removeStyleName("Highlighted");
                                    }
                                    (_activeLabel = (Label) sender).addStyleName("Highlighted");
                                    _playerLists.add(new PlayerList((List) result));
                                    forward();
                                }
                                public void onFailure (Throwable cause) {
                                    add(new Label(CAdmin.serverError(cause)));
                                }
                            }
                        );
                    }
                });
                nameLabel.addStyleName("Clickable");
                setWidget(row, 0, nameLabel);
                setText(row, 1, "" + member.invitesGranted);
                setText(row, 2, "" + member.invitesSent);
                getFlexCellFormatter().addStyleName(row, 3, "Last");
                setText(row++, 3, "" + (member.invitesGranted + member.invitesSent));
            }
            for (int ii = 0; ii < NUM_COLUMNS; ii++) {
                getFlexCellFormatter().addStyleName(row-1, ii, "Bottom");
            }
        }

        protected static final int NUM_COLUMNS = 4;

        protected Label _activeLabel;
    }

    // ArrayList<PlayerList>
    protected ArrayList _playerLists;
    protected PlayerList _primaryList;
    protected PlayerList _secondaryList;
}
