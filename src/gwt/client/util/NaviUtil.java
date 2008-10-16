//
// $Id$

package client.util;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.web.client.Args;
import com.threerings.msoy.web.client.Pages;


/**
 * A place where we can encapsulate the creation of arguments that link to complex pages in
 * Whirled. We do this here so that we don't have references between otherwise unrelated classes
 * and services introduced by the fact that we want a Hyperlink from somewhere in Whirled to
 * somewhere (from a code standpoint) totally unrelated.
 */
public class NaviUtil
{
    public enum GameDetails {
        INSTRUCTIONS("i"), COMMENTS("c"), TROPHIES("t"), MYRANKINGS("mr"),
        TOPRANKINGS("tr"), METRICS("m"), LOGS("l");

        public String code () {
            return _code;
        }

        GameDetails (String code) {
            _code = code;
        }

        /**
         * Look up a GameDetails by its code.
         */
        public static GameDetails getByCode (String code)
        {
            // we could store these in a map, but why bother?
            for (GameDetails detail : values()) {
                if (detail.code().equals(code)) {
                    return detail;
                }
            }
            return GameDetails.INSTRUCTIONS;
        }

        protected String _code;
    }        

    public static String gameDetail (int gameId, GameDetails tab)
    {
        return Args.compose("d", gameId, tab.code());
    }

    public static ClickListener onCreateItem (byte type, byte ptype, int pitemId)
    {
        return Link.createListener(Pages.STUFF, Args.compose("c", type, ptype, pitemId));
    }

    public static ClickListener onEditItem (byte type, int itemId)
    {
        return Link.createListener(Pages.STUFF, Args.compose("e", type, itemId));
    }

    public static ClickListener onRemixItem (byte type, int itemId)
    {
        return Link.createListener(Pages.STUFF, Args.compose("r", type, itemId));
    }

    public static ClickListener onViewTransactions (ReportType report)
    {
        return Link.createListener(Pages.ME, Args.compose("transactions", report.toIndex()));
    }

    /**
     * When clicked, popup up a window to billing to buy bars.
     */
    public static ClickListener onBuyBars ()
    {
        return new ClickListener() {
            public void onClick (Widget sender) {
                Window.open(Link.billingURL(), "_blank",
                    // For those silly browsers that open this in a new window instead of a new
                    // tab, enable all the chrome options on the new window.
                    "resizable=1,menubar=1,toolbar=1,location=1,status=1,scrollbars=1");
            }
        };
    }
}
