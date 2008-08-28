//
// $Id$

package com.threerings.msoy.money.server;

import java.util.List;

import com.google.inject.Inject;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.MoneyType;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.HistoryListResult;
import com.threerings.msoy.money.server.persist.MoneyRepository;

/**
 * Provides the server implementation of {@link MoneyService}.
 */
public class MoneyServlet extends MsoyServiceServlet
    implements MoneyService
{
    public HistoryListResult getTransactionHistory (int memberId, MoneyType type,
                                                    int from, int count)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (mrec.memberId != memberId && !mrec.isSupport()) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        HistoryListResult ofTheJedi = new HistoryListResult();
        ofTheJedi.history = _moneyLogic.getLog(memberId, type, from, count, true);
        ofTheJedi.totalCount = 50; // TODO
        return ofTheJedi;
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
}
