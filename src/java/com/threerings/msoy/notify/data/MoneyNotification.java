//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Base class for money notifications.
 */
@com.threerings.util.ActionScript(omit=true)
public class MoneyNotification extends Notification
{
    /** The type of money this notification is concerned with. */
    public Currency currency;

    /** The amount that it's changed. */
    public int delta;

    public MoneyNotification ()
    {
    }

    public MoneyNotification (String announcement)
    {
        _msg = announcement;
    }

    @Override
    public String getAnnouncement ()
    {
        return null; // client-side only
    }

    /** The translation key, or null to use a generic message. */
    protected String _msg;
}
