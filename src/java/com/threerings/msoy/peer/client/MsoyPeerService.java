//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.stats.data.StatSet;

/**
 * A service implemented by MetaSOY peer nodes.
 */
public interface MsoyPeerService extends InvocationService
{
    /**
     * Forwards a resolved member object to a server to which the member is about to connect.
     */
    public void forwardMemberObject (Client client, MemberObject memobj, 
        String actorState, StatSet stats, PlayerMetrics metrics);
}
