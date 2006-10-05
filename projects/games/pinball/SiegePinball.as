package {

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;

import org.cove.flade.surfaces.*;
import org.cove.flade.constraints.*;
import org.cove.flade.composites.*;
import org.cove.flade.primitives.*;
import org.cove.flade.DynamicsEngine;

import flash.display.*;
import flash.text.*;
import flash.events.*;
import flash.ui.*;


[SWF(width="640", height="480")]
public class SiegePinball extends Sprite
    implements Game, PropertyChangedListener, MessageReceivedListener
{
    protected var engine:DynamicsEngine;

    public function SiegePinball () {
        graphics.beginFill(0xEEDDCC);
        graphics.drawRect(0, 0, 640, 580);

        engine = new DynamicsEngine(this);

        engine.setDamping(1.0);
        engine.setGravity(0.0, 0.03);
        engine.setSurfaceBounce(1.0);
        engine.setSurfaceFriction(0.1);

        // platform
//        engine.addSurface(new LineSurface(535, 5, 635, 5));
        engine.addSurface(new LineSurface(635, 475, 635, 5));
//        engine.addSurface(new LineSurface(635, 475, 5, 475));
        engine.addSurface(new LineSurface(5, 5, 5, 475));

        // circles
        for (var i :int = 0; i < 20; i ++) {
            var tile :CircleTile = new CircleTile(
                100 + Math.random() * 420,
                80 + Math.random() * 320,
                20 + Math.random() * 20);
            engine.addSurface(tile);
            tile.onContactListener = function() :void {
            }
        }
        engine.paintSurfaces();

        // TODO
        setGameObject(null);
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObject = gameObj;

        addEventListener(Event.ENTER_FRAME, enterFrame);
        this.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyDown);
        this.stage.addEventListener(KeyboardEvent.KEY_UP, keyUp);
    }

    public function enterFrame (event:Event) :void {
        run();
    }

    public function keyDown (event:KeyboardEvent) :void {
        if (event.keyCode == Keyboard.LEFT) {
            fireCow();
        }
    }



    protected function fireCow () :void {
        new Cow(engine, 120 + Math.random()*380, 10);
//            cow.prev.x = 6;
//            cow.prev.y = 455;
//            engine.addPrimitive(cow);
    }

    public function keyUp (event:KeyboardEvent) :void {
    }

    public function run () :void {
        engine.timeStep();
        engine.timeStep();
        engine.paintPrimitives();
        engine.paintConstraints();          
    }       
    protected var _gameObject :EZGame;
}
}
