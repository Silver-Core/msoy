//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.server.persist.RatingRecord;

/**
 * Rating records for games.
 */
public class MsoyGameRatingRecord extends RatingRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MsoyGameRatingRecord> _R = MsoyGameRatingRecord.class;
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp RATING = colexp(_R, "rating");
    // AUTO-GENERATED: FIELDS END


    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MsoyGameRatingRecord}
     * with the supplied key values.
     */
    public static Key<MsoyGameRatingRecord> getKey (int targetId, int memberId)
    {
        return new Key<MsoyGameRatingRecord>(
                MsoyGameRatingRecord.class,
                new ColumnExp[] { TARGET_ID, MEMBER_ID },
                new Comparable[] { targetId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}