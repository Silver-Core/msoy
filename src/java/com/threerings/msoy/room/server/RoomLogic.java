//
// $Id$

package com.threerings.msoy.room.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ResultListener;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.whirled.server.SceneRegistry;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.room.data.MsoySceneModel;

import com.threerings.msoy.room.server.MsoySceneRegistry;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

@Singleton @BlockingThread
public class RoomLogic
{
    /**
     * Validate that the user may gift the specified room.
     * Returns without exception if gifting is permitted.
     */
    public void checkCanGiftRoom (MemberRecord mrec, int sceneId)
        throws ServiceException
    {
        if (mrec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        if (mrec.homeSceneId == sceneId) {
            throw new ServiceException("errCantGiftHome"); // TODO?
        }
        // make sure they own the room
        SceneRecord screc = _sceneRepo.loadScene(sceneId);
        if (screc == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        if ((screc.ownerType != MsoySceneModel.OWNER_TYPE_MEMBER) ||
                (screc.ownerId != mrec.memberId)) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
    }

    public void processRoomGift (final int senderId, final int receiverId, final int sceneId)
        throws ServiceException
    {
        final MemberRecord receiver = _memberRepo.loadMember(receiverId);
        if (receiver == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        MemberRecord sender = _memberRepo.loadMember(senderId);
        checkCanGiftRoom(sender, sceneId);

        _omgr.postRunnable(new Runnable() {
            public void run () {
                ((MsoySceneRegistry) _sceneReg).transferOwnership(sceneId,
                    MsoySceneModel.OWNER_TYPE_MEMBER, receiverId, receiver.getName(), true,
                    new ResultListener<Void>() {
                        public void requestCompleted (Void result) {}
                        public void requestFailed (Exception cause) {
                            log.warning("Unable to transfer room", cause);
                        }
                    });
            }
        });

        // TODO
        // - change room's access policy to ACCESS_OWNER_ONLY
        // - reflect any changes in the runtime representation of the room
        // - find all items owned by the sender that are in use in the room, and update the owner
        // to be the recipient. Items not owned by the sender are not transferred.
        // - reflect item changes in the runtime
    }

    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected SceneRegistry _sceneReg;
    @Inject protected PresentsDObjectMgr _omgr;
}
