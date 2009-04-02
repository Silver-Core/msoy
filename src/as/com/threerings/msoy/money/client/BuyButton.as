//
// $Id$

package com.threerings.msoy.money.client {

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

// TODO: fix jumpy layout of LoadedAsset
public class BuyButton extends CommandButton
{
    /**
     * Construct a BuyButton. Arg is going to be appended to two other args: the currency
     * and approved price.
     */
    public function BuyButton (currency :Currency, cmdOrFn :* = null, arg :Object = null)
    {
        super(null, cmdOrFn, arg);

        if (currency == Currency.BARS) {
            styleName = "orangeButton";
        }
        setStyle("icon", currency.getLargeIconClass());
        setStyle("fontSize", 24);
        setStyle("fontFamily", "_sans");

        _currency = currency;
        setValue(-1);
    }

    public function setPriceQuote (quote :PriceQuote) :void
    {
        setValue(quote.getAmount(_currency));
    }

    public function setValue (value :int) :void
    {
        _value = value;
        FlexUtil.setVisible(this, (value >= 0));
        this.label = (value == 0) ? Msgs.GENERAL.get("b.free") : String(value);
        checkEnabled();
    }

    override public function getArg () :Object
    {
        var arg :Object = super.getArg();
        var args :Array = [ _currency, _value ];
        if (arg is Array) {
            args.push.apply(null, arg as Array);
        } else if (arg != null) {
            args.push(arg);
        }
        return args;
    }

    override public function get enabled () :Boolean
    {
        // return our true enabled state
        return super.enabled;
    }

    override public function set enabled (on :Boolean) :void
    {
        _enabled = on;
        checkEnabled();
    }

    protected function checkEnabled () :void
    {
        super.enabled = _enabled && (_value >= 0);
    }

    protected var _currency :Currency;
    protected var _value :int;

    protected var _enabled :Boolean = true;
}
}
