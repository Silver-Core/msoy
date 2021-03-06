//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;
import flash.external.ExternalInterface;
import flash.geom.Rectangle;

import mx.binding.utils.BindingUtils;
import mx.containers.Grid;
import mx.controls.Text;
import mx.controls.VSlider;
import mx.core.UIComponent;
import mx.events.CloseEvent;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.media.MediaContainer;

import com.threerings.flex.CommandComboBox;
import com.threerings.flex.FlexUtil;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyParameters;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.ui.FlyingPanel;

/**
 * Widgets used for editing a backdrop definition.
 */
public class DecorEditPanel extends FlyingPanel
{
    public function DecorEditPanel (ctx :MsoyContext, studioView :RoomStudioView)
    {
        super(ctx, Msgs.STUDIO.get("t.backdrop_props"));
        _studioView = studioView;
        _decor = studioView.getScene().getDecor();
        open();
    }

    override protected function didOpen () :void
    {
        // position ourselves
        var r :Rectangle = _ctx.getTopPanel().getPlaceViewBounds();
        this.x = r.right - width - PADDING;
        this.y = r.y + PADDING;

        super.didOpen();

        callLater(doHookup);
    }

    protected function doHookup () :void
    {
        // maybe we're being used as a tester in the SDK....
        var media :String = MsoyParameters.get()["media"] as String;
        if (media != null) {
            updateMedia(media, true);
            return;
        }

        // or, we're in the web site, being used to edit a decor
        try {
            ExternalInterface.addCallback("updateMedia", updateMedia);
            ExternalInterface.addCallback("updateParameters", updateParameters);

            ExternalInterface.call("updateDecorInit");

        } catch (e :Error) {
            log.warning("Unable to configure js bridge", e);
        }
    }

    override protected function handleClose (event :CloseEvent) :void
    {
        //var btn :Button = getCloseButton();
        //btn.selected = !btn.selected; // doesn't work
        FlexUtil.setVisible(_grid, !_grid.visible);
    }

    protected function saveChanges (... ignored) :void
    {
        if (_suppressSaves) {
            return;
        }

        readRoomType();
        _decor.depth = _depth.value;
        _decor.horizon = _horizon.value;
        _decor.actorScale = _actorScale.value;
        _decor.furniScale = _furniScale.value;

        if (_decor.getRawFurniMedia() != null) {
            updateDecorInViewer();
            updateDecorOnPage();
        }
    }

    protected function updateMedia (path :String, figureAttrs :Boolean) :void
    {
        _decor.setFurniMedia(new StudioMediaDesc(path));
        updateDecorInViewer();
        if (figureAttrs) {
            _studioView.getBackground().viz.addEventListener(
                MediaContainer.SIZE_KNOWN, handleSizeKnown);
        }
        if (_noMedia.visible) {
            // set things up to actually edit this decor now
            FlexUtil.setVisible(_noMedia, false);
            FlexUtil.setVisible(_grid, true);
            showCloseButton = true;
        }
    }

    protected function updateParameters (
        type :int, hideWalls :Boolean, width :int, height :int, depth :int,
        horizon :Number, actorScale :Number, furniScale :Number) :void
    {
        _decor.type = type;
        _decor.hideWalls = hideWalls;
        _decor.width = width;
        _decor.height = height;
        _decor.depth = depth;
        _decor.horizon = horizon;
        _decor.actorScale = actorScale;
        _decor.furniScale = furniScale;

        _suppressSaves = true;
        try {
            _roomType.selectedData = figureRoomType();
            _depth.maximum = Math.max(height * 8, depth); // in case there's legacy deepness
            _depth.value = depth;
            _depth.tickValues = [ height ];
            _horizon.value = horizon;
            _actorScale.value = actorScale;
            _furniScale.value = furniScale;
            _checkRoomTypes()
        } finally {
            _suppressSaves = false;
        }

        updateDecorInViewer();
    }

    protected function updateDecorInViewer () :void
    {
        var newScene :MsoyScene = _studioView.getScene().clone() as MsoyScene;
        var newModel :MsoySceneModel = newScene.getSceneModel() as MsoySceneModel;
        newModel.decor = _decor;

        _studioView.setScene(newScene);
        _studioView.updateBackground();
    }

    protected function updateDecorOnPage () :void
    {
        try {
            if (!ExternalInterface.available) {
                return;
            }

            ExternalInterface.call("updateDecor",
                _decor.type, _decor.hideWalls, _decor.width, _decor.height, _decor.depth,
                _decor.horizon, _decor.actorScale, _decor.furniScale);

        } catch (e :Error) {
            log.warning("Unable to send decor to hosting page.", e);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _noMedia = FlexUtil.createText(Msgs.STUDIO.get("m.no_media"), 150);
        addChild(_noMedia);

        _roomType = new CommandComboBox(saveChanges);
        var types :Array = [];
        for (var ii :int = 0; ii < TYPE_COUNT; ii++) {
            types[ii] = { label: Msgs.STUDIO.get(ROOM_KEYS[ii]), data: ii };
        }
        _roomType.dataProvider = types;

        _actorScale = new VSlider();
        _actorScale.liveDragging = true;
        _actorScale.maximum = 4;
        _actorScale.minimum = .25;
        _actorScale.tickValues = [ 1 ];

        _furniScale = new VSlider();
        _furniScale.liveDragging = true;
        _furniScale.maximum = 4;
        _furniScale.minimum = .25;
        _furniScale.tickValues = [ 1 ];

        _horizon = new VSlider();
        _horizon.liveDragging = true;
        _horizon.maximum = 1;
        _horizon.minimum = 0;
        _horizon.tickValues = [ .5 ];

        _depth = new VSlider();
        _depth.liveDragging = true;
        // _depth.maximum is set in updateParameters
        _depth.minimum = 0;

        var typeP :Grid = new Grid();
        GridUtil.addRow(typeP, Msgs.STUDIO.get("l.room_type"), _roomType);

        var slideP :Grid = new Grid();
        var ascaleP :Grid = new Grid();
        var fscaleP :Grid = new Grid();
        var horzP :Grid = new Grid();
        var depthP :Grid = new Grid();
        GridUtil.addRow(ascaleP, Msgs.STUDIO.get("l.actor_scale"));
        GridUtil.addRow(ascaleP, _actorScale);
        GridUtil.addRow(fscaleP, Msgs.STUDIO.get("l.furni_scale"));
        GridUtil.addRow(fscaleP, _furniScale);
        GridUtil.addRow(horzP, Msgs.STUDIO.get("l.horizon"));
        GridUtil.addRow(horzP, _horizon);
        GridUtil.addRow(depthP, Msgs.STUDIO.get("l.depth"));
        GridUtil.addRow(depthP, _depth);
        GridUtil.addRow(slideP, depthP, horzP, ascaleP, fscaleP);

        _checkRoomTypes = function ( ... ignored) :void {
            // turn depth and horizon off for flatland
            const on :Boolean =
                _roomType.selectedData != ROOM_FLATLAND && _roomType.selectedData != ROOM_TOPDOWN;
            for each (var comp :UIComponent in [ horzP, depthP ]){
                FlexUtil.setVisible(comp, on);
            }
        };
        _roomType.addEventListener(Event.CHANGE, _checkRoomTypes);
        _roomType.selectedData = ROOM_FLATLAND; // flatland is smallest layout
        _checkRoomTypes();

        GridUtil.addRow(_grid, typeP);
        GridUtil.addRow(_grid, slideP);
        addChild(_grid);
        FlexUtil.setVisible(_grid, false);

        // now bind everything up so that these widgets change things
        _suppressSaves = true;
        try {
            BindingUtils.bindSetter(saveChanges, _actorScale, "value");
            BindingUtils.bindSetter(saveChanges, _furniScale, "value");
            BindingUtils.bindSetter(saveChanges, _horizon, "value");
            BindingUtils.bindSetter(saveChanges, _depth, "value");
        } finally {
            _suppressSaves = false;
        }
    }

    /**
     * Return the room type, according to the decor properties.
     */
    protected function figureRoomType () :int
    {
        if (_decor.type == Decor.FLAT_LAYOUT) {
            return ROOM_FLATLAND;

        } else if (_decor.type == Decor.TOPDOWN_LAYOUT) {
            return ROOM_TOPDOWN;

        } else if ((_decor.type == Decor.IMAGE_OVERLAY) && _decor.hideWalls) {
            return ROOM_NO_WALLS;

        } else {
            // some old decors may be other types, but we f'n don't care
            return ROOM_NORMAL;
        }
    }

    /**
     * Turn the selected room type back into decor properties, according to ROOM_TYPES.
     */
    protected function readRoomType () :void
    {
        switch (_roomType.selectedData) {
        case ROOM_NORMAL:
        case ROOM_NO_WALLS:
            _decor.type = Decor.IMAGE_OVERLAY;
            _decor.hideWalls = (_roomType.selectedData == ROOM_NO_WALLS);
            break;

        case ROOM_FLATLAND:
            _decor.type = Decor.FLAT_LAYOUT;
            // WEIRD, flatland seems to freak out if the depth is not normalish, so reset that
            _suppressSaves = true;
            try {
                _depth.value = _decor.height;
            } finally {
                _suppressSaves = false;
            }
            break;

        case ROOM_TOPDOWN:
            _decor.type = Decor.TOPDOWN_LAYOUT;
            break;

        default:
            log.warning("Unhandled room type!");
            break;
        }
    }

    /**
     * Called when we know the size of the backdrop media, and are supposed to fill-in the
     * initial values of the decor ourselves.
     */
    protected function handleSizeKnown (event :ValueEvent) :void
    {
        const w :int = int(event.value[0]);
        const h :int = int(event.value[1]);
        updateParameters(Decor.IMAGE_OVERLAY, false, w, h, h, .5, 1, 1);
        updateDecorOnPage();
    }

    protected var log :Log = Log.getLog(this);

    protected var _suppressSaves :Boolean;

    protected var _decor :Decor;

    protected var _studioView :RoomStudioView;

    // NOTE: There is no support for FIXED_IMAGE right now,
    // because it doesn't play well with zooming
    /** The room types we support. */

    protected const ROOM_NORMAL :int = 0;
    protected const ROOM_NO_WALLS :int = 1;
    protected const ROOM_FLATLAND :int = 2;
    protected const ROOM_TOPDOWN :int = 3;
    protected const TYPE_COUNT :int = 4;

    protected const ROOM_KEYS :Array =
        [ "m.room_normal", "m.room_no_walls", "m.room_flat", "m.room_topdown" ];

    protected var _noMedia :Text;
    protected var _roomType :CommandComboBox;
    protected var _actorScale :VSlider;
    protected var _furniScale :VSlider;
    protected var _horizon :VSlider;
    protected var _depth :VSlider;

    protected var _grid :Grid = new Grid();

    protected var _checkRoomTypes :Function;
}
}
