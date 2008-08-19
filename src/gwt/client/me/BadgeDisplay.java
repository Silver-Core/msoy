//
// $Id$

package client.me;

import java.util.Date;
import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;

import com.threerings.msoy.item.data.all.Item;

import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

class BadgeDisplay extends SmartTable
{
    public BadgeDisplay (Badge badge)
    {
        setStyleName("badgeDisplay");

        buildBasics(badge);

        if (badge instanceof EarnedBadge) {
            addEarnedBits((EarnedBadge) badge);
        } else if (badge instanceof InProgressBadge) {
            addInProgressBits((InProgressBadge) badge);
        }
    }

    protected void buildBasics (Badge badge)
    {
        setWidget(0, 0, MsoyUI.createImage(badge.imageUrl(), null));
        getFlexCellFormatter().setRowSpan(0, 0, 2);

        String hexCode = Integer.toHexString(badge.badgeCode);
        String badgeName = hexCode;
        try {
            badgeName = _dmsgs.getString("badge_" + hexCode);
        } catch (MissingResourceException mre) {
            // displaying the hex code is the failure case - make sure to test all new badges
            // before letting them out to production.
        }
        setText(0, 1, badgeName, 1, "StampName");

        String badgeDesc = null;
        try {
            // first look for a specific message for this level
            badgeDesc = _dmsgs.getString("badgeDesc" + badge.level + "_" + hexCode);
        } catch (MissingResourceException mre) {
            // No big deal, we'll check for the dynamic version next
        }

        if (badgeDesc == null) {
            try {
                badgeDesc =
                    _dmsgs.getString("badgeDescN_" + hexCode).replace("{0}", badge.levelUnits);
            } catch (MissingResourceException mre) {
                // again, this is a testing failure case - never let a badge make it to production
                // with this in the description field.
                badgeDesc = "MISSING DESCRIPTION [" + hexCode + "]";
            }
        }
        setText(2, 0, badgeDesc, 2, "StampDescription");
    }

    protected void addEarnedBits (EarnedBadge badge)
    {
        if (badge.whenEarned == null) {
            return;
        }

        Date earnedDate = new Date(badge.whenEarned);
        setText(3, 0, _msgs.passportFinishedSeries(MsoyUI.formatDate(earnedDate)), 2, null);
    }

    protected void addInProgressBits (InProgressBadge badge)
    {
        // TODO: display the little coin image too
        setText(1, 0, "" + badge.coinReward);

        setWidget(3, 0, new ProgressRow(badge, getGoListener(badge)), 2, null);
    }

    protected ClickListener getGoListener (Badge badge)
    {
        // TODO: this method of determining where to go for each badge is disappointing... this
        // is the only piece of badge UI code that needs changing when a new badge is added.
        // It would be great if this were done more dynamically...

        switch (badge.badgeCode) {
        // friendly
        case -990018741: return Link.createListener(Pages.WHIRLEDS, "");
        // magnet
        case -94886133: return Link.createListener(Pages.PEOPLE, "invites");
        // fixture
        case 983613172: return Link.createListener(Pages.WHIRLEDS, "");

        // gamer
        case 2138102039: // same as below
        // contender
        case -425662117: // same as below
        // collector
        case -1978012765: return Link.createListener(Pages.GAMES, "");

        // character designer
        case 1852244093: return Link.createListener(Pages.STUFF, "" + Item.AVATAR);
        // furniture builder
        case -255838771: return Link.createListener(Pages.STUFF, "" + Item.FURNITURE);
        // landscape painter
        case 292647383: return Link.createListener(Pages.STUFF, "" + Item.DECOR);
        // professional
        case 646396602: // same as below
        // artisan
        case 52819145: return null; // for now, don't display the go button.

        // judge
        case -424738396: // same as below
        // outspoken
        case 1017487473: // same as belw
        // shopper
        case 421773639: return Link.createListener(Pages.SHOP, "");
        }

        return null;
    }

    protected static class ProgressRow extends FlowPanel
    {
        public ProgressRow (InProgressBadge badge, ClickListener goListener)
        {
            // TODO: add yet more info to InProgressBadge to tell the client not to display a
            // progress meter for some first-level badges.
            add(new ProgressBar(badge.progress));

            if (goListener != null) {
                add(MsoyUI.createImageButton("GoButton", goListener));
            }
        }
    }

    protected static class ProgressBar extends HorizontalPanel
    {
        public ProgressBar (float progress)
        {
            setStyleName("ProgressBar");
            setHeight(PROGRESS_HEIGHT + "px");

            if (progress <= 0) {
                addCap(Section.LEFT, Type.EMPTY);
                addFill(Type.EMPTY, PROGRESS_WIDTH - END_CAP_WIDTH * 2);
                addCap(Section.RIGHT, Type.EMPTY);
                return;
            }

            addCap(Section.LEFT, Type.FULL);
            int fullWidth = Math.max(0, (int)(PROGRESS_WIDTH * (progress - END_CAP_PROGRESS * 2)));
            if (fullWidth > 0) {
                addFill(Type.FULL, fullWidth);
            }
            transitionToEnd(PROGRESS_WIDTH - fullWidth);
        }

        protected void addCap (Section section, Type type)
        {
            add(MsoyUI.createImage(section.getPath(type), null));
        }

        protected void addFill (Type type, int width)
        {
            SimplePanel fillPanel = new SimplePanel();
            fillPanel.setStyleName("Fill");
            fillPanel.setHeight(PROGRESS_HEIGHT + "px");
            fillPanel.setWidth(width + "px");
            DOM.setStyleAttribute(fillPanel.getElement(),
                "backgroundImage", "url(" + Section.FILL.getPath(type) + ")");
            add(fillPanel);
        }

        protected void transitionToEnd(int remainingWidth)
        {
            // if we only have room for one cap, add it and be done
            if (remainingWidth <= END_CAP_WIDTH) {
                addCap(Section.RIGHT, Type.FULL);
                return;
            }

            add(MsoyUI.createImage(Type.TRANSITION.getBasePath(), null));
            // if we've only got room for two caps, add the second and be done.
            if (remainingWidth <= END_CAP_WIDTH * 2) {
                addCap(Section.RIGHT, Type.EMPTY);
                return;
            }

            addFill(Type.EMPTY, remainingWidth - END_CAP_WIDTH * 2);
            addCap(Section.RIGHT, Type.EMPTY);
        }

        protected static enum Section {
            LEFT("left.png"), FILL("tile.png"), RIGHT("right.png");

            public String getPath (Type type) {
                return type.getBasePath() + _path;
            }

            Section (String path) {
                _path = path;
            }

            protected String _path;
        }

        protected static enum Type {
            EMPTY("empty_"), TRANSITION("transition.png"), FULL("");

            public String getBasePath () {
                return BASE_PATH + _path;
            }

            Type (String path) {
                _path = path;
            }

            protected String _path;

            protected static final String BASE_PATH = "/images/me/passport_progress_";
        }
    }

    protected VerticalPanel _nameColumn;

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);

    protected static final int PROGRESS_WIDTH = 90;
    protected static final int END_CAP_WIDTH = 9;
    protected static final int PROGRESS_HEIGHT = 17;
    protected static final float END_CAP_PROGRESS = END_CAP_WIDTH / (float)PROGRESS_WIDTH;
}
