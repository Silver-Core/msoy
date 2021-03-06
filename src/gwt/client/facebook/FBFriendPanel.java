//
// $Id$

package client.facebook;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.AbsoluteCSSPanel;

import com.threerings.msoy.facebook.gwt.FacebookFriendInfo.Thumbnail;
import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays a single {@link FacebookFriendInfo}.
 */
public class FBFriendPanel extends AbsoluteCSSPanel
{
    /**
     * Creates a new friend panel.
     */
    public FBFriendPanel (FacebookFriendInfo info, int rank)
    {
        // provide a context in case parent doesn't set it to "absolute" later
        super("friendInfo", "fixed");
        if (info == null) {
            addStyleName("friendInfoEmpty");
            add(MsoyUI.createImageButton("InviteButton",
                Link.createHandler(Pages.FACEBOOK, "invite")));
            add(MsoyUI.createLabel(_msgs.inviteTip(), "InviteTip"));
            return;
        }

        addStyleName(rank % 2 == 0 ? "friendInfoEven" : "friendInfoOdd");

        int halfSize = MediaDescSize.HALF_THUMBNAIL_SIZE;
        add(MsoyUI.createFlowPanel("Name", FBMLPanel.makeName(info.facebookUid)));

        if (rank <= 3) {
            FlowPanel rankIcon = MsoyUI.createFlowPanel("RankIcon");
            rankIcon.getElement().setAttribute("gamerank", String.valueOf(rank));
            add(rankIcon);
        } else {
            add(MsoyUI.createLabel("" + rank, "Rank"));
        }
        add(MsoyUI.createFlowPanel("Photo", FBMLPanel.makeProfilePic(info.facebookUid)));
        add(MsoyUI.createLabel(_msgs.level(convertToText(info.level)), "Level"));
        Thumbnail lastGame = info.lastGame;
        if (lastGame != null) {
            add(new ThumbBox(lastGame.media, halfSize, Pages.GAMES, "d", lastGame.id));
            add(Link.create(lastGame.name, "Game", Pages.GAMES, "d", lastGame.id));
        }
        add(MsoyUI.createLabel(convertToText(info.trophyCount), "Trophies"));
        add(MsoyUI.createFlowPanel("TrophyIcon"));
    }

    protected static String convertToText (int number)
    {
        if (number < 1000) {
            return String.valueOf(number);
        }
        if (number < 1000000) {
            return _msgs.kilo(String.valueOf(number / 1000));
        }
        if (number < 1000000000) {
            return _msgs.mega(String.valueOf(number / 1000000));
        }
        return _msgs.giga(String.valueOf(number / 1000000000));
    }

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
}
