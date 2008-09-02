//
// $Id$

package com.threerings.msoy.badge.server.persist;

import java.util.Set;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

@Singleton @BlockingThread
public class BadgeRepository extends DepotRepository
{
    @Inject public BadgeRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }
        
    /**
     * Stores the supplied badge record in the database.
     *
     * @return true if the record was created, false if it was updated.
     */
    public boolean storeBadge (EarnedBadgeRecord badge)
        throws PersistenceException
    {
        return store(badge);
    }

    /**
     * Stores the supplied in-progress badge record in the database.
     */
    public void storeInProgressBadge (InProgressBadgeRecord badge)
        throws PersistenceException
    {
        store(badge);
    }

    /**
     * Loads all of the specific member's earned badges.
     */
    public List<EarnedBadgeRecord> loadEarnedBadges (int memberId)
        throws PersistenceException
    {
        return findAll(EarnedBadgeRecord.class, new Where(EarnedBadgeRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Returns up to limit badges, order by date descending.
     */
    public List<EarnedBadgeRecord> loadRecentEarnedBadges (int memberId, int limit)
        throws PersistenceException
    {
        return findAll(EarnedBadgeRecord.class, new Where(EarnedBadgeRecord.MEMBER_ID_C, memberId),
            new Limit(0, limit), OrderBy.descending(EarnedBadgeRecord.WHEN_EARNED_C));
    }

    /**
     * Loads all of the specified member's in-progress badges.
     */
    public List<InProgressBadgeRecord> loadInProgressBadges (int memberId)
        throws PersistenceException
    {
        return findAll(InProgressBadgeRecord.class, new Where(InProgressBadgeRecord.MEMBER_ID_C,
            memberId));
    }

    /**
     * Loads the EarnedBadgeRecord, if it exists, for the specified member and badge type.
     */
    public EarnedBadgeRecord loadEarnedBadge (int memberId, int badgeCode)
        throws PersistenceException
    {
        return load(EarnedBadgeRecord.class, EarnedBadgeRecord.getKey(memberId, badgeCode));
    }

    /**
     * Loads the InProgressBadgeRecord, if it exists, for the specified member and badge type.
     */
    public InProgressBadgeRecord loadInProgressBadge (int memberId, int badgeCode)
        throws PersistenceException
    {
        return load(InProgressBadgeRecord.class, InProgressBadgeRecord.getKey(memberId, badgeCode));
    }

    /**
     * Deletes the InProgressBadgeRecord, if it exists, for the specified member and badge type.
     *
     * @return true if the record was deleted; false if it did not exist
     */
    public boolean deleteInProgressBadge (int memberId, int badgeCode)
        throws PersistenceException
    {
        return (delete(InProgressBadgeRecord.class, InProgressBadgeRecord.getKey(memberId,
            badgeCode)) > 0);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(EarnedBadgeRecord.class);
        classes.add(InProgressBadgeRecord.class);
    }
}
