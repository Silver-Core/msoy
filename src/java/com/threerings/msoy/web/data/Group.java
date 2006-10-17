//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Contains the details of a group.
 */
public class Group
    implements Streamable, IsSerializable
{
    public static final byte POLICY_PUBLIC = 1;
    public static final byte POLICY_INVITE_ONLY = 2;
    public static final byte POLICY_EXCLUSIVE = 3;

    /** The unique id of this group. */
    public int groupId;

    /** The name of the group. */
    public String name;

    /** The group's charter, or null if one has yet to be set. */
    public String charter;

    public MediaDesc logo;

    /** The member id of the person who created the group. */
    public int creatorId;
    
    public Date creationDate;

    public byte policy;
}
