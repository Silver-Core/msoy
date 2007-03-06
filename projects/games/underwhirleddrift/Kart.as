package {
import flash.display.Sprite;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.events.Event;

import flash.utils.describeType;

import mx.core.MovieClipAsset;

public class Kart extends KartSprite
{
    public function Kart (kartType :String, camera :Camera, ground :Ground) 
    {
        super(kartType);
        _camera = camera;
        _ground = ground;

        addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    /**
     * Get an update object to send to the other clients.
     */
    public function getUpdate () :Object
    {
        var loc :Point = _ground.getKartLocation();
        return {
            posX: loc.x,
            posY: loc.y,
            angle: _camera.angle,
            speed: _currentSpeed,
            movement: _movement
        };
    }

    public function moveForward (moving :Boolean) :void 
    { 
        keyAction(moving, MOVEMENT_FORWARD);
    }

    public function moveBackward (moving :Boolean) :void
    {
        if (moving && _currentSpeed > 0) {
            _braking = true;
        } else if (!moving && _braking) {
            _braking = false;
        }
        keyAction(moving, MOVEMENT_BACKWARD);
    }

    public function turnLeft (turning :Boolean) :void
    {
        keyAction(turning, MOVEMENT_LEFT);
        if (!turning) keyAction(false, MOVEMENT_DRIFT);
    }

    public function turnRight (turning :Boolean) :void
    {
        keyAction(turning, MOVEMENT_RIGHT);
        if (!turning) keyAction(false, MOVEMENT_DRIFT);
    }

    public function jump () :void
    {
        if (_jumpFrameCount == 0) {
            _jumpFrameCount = JUMP_DURATION;
            y -= JUMP_HEIGHT;
        }
    }

    public function enterFrame (event :Event) :void
    {
        // alter camera angle
        if (_movement & (MOVEMENT_RIGHT | MOVEMENT_LEFT)) {
            if (_movement & MOVEMENT_RIGHT) {
                _current3PAngle = Math.min(MAX_TURN_ANGLE, _current3PAngle + TURN_ACCELERATION);
            } else {
                _current3PAngle = Math.max(-MAX_TURN_ANGLE, _current3PAngle - TURN_ACCELERATION);
            }
        } else {
            if (_current3PAngle > 0) {
                _current3PAngle = Math.max (0, _current3PAngle - TURN_ACCELERATION);
            } else if (_current3PAngle < 0) {
                _current3PAngle = Math.min (0, _current3PAngle + TURN_ACCELERATION);
            }
        }
        if (_currentSpeed > 0) {
            _camera.angle = (_camera.angle + _current3PAngle) % (Math.PI * 2);
        } else if (_currentSpeed < 0) {
            _camera.angle = (_camera.angle - _current3PAngle + Math.PI * 2) % (Math.PI * 2);
        }
        // update turn animation
        var max_view_angle :int = _movement & MOVEMENT_DRIFT ? DRIFT_VIEW_ANGLE : TURN_VIEW_ANGLE;
        var frame :int = Math.ceil((Math.abs(_current3PAngle) / MAX_TURN_ANGLE) * max_view_angle);
        if (_current3PAngle > 0) {
            frame = 360 - frame;
        } else if (_current3PAngle == 0) {
            frame = 1;
        }
        if (_kart.currentFrame != frame) {
            _kart.gotoAndStop(frame);
        }

        // alter camera location
        /*var speedConfig :Object = {
            gasAccel: _movementConstants.accelGas,
            coastAccel: _movementConstants.accelCoast,
            brakeAccel: _movementConstants.accelBrake,
            minSpeed: SPEED_MIN,
            maxSpeed: _movementConstants.maxSpeed
        };
        if (!_ground.drivingOnRoad()) {
            speedConfig.gasAccel *= TERRAIN_SPEED_FACTOR;
            speedConfig.coastAccel /= TERRAIN_SPEED_FACTOR;
            speedConfig.brakeAccel /= TERRAIN_SPEED_FACTOR;
            speedConfig.minSpeed *= TERRAIN_SPEED_FACTOR;
            speedConfig.maxSpeed *= TERRAIN_SPEED_FACTOR;
        }*/
        // TODO: this will clearly need more intelligent processing
        //if (_ground.drivingIntoWall()) {
            //_currentSpeed = 0;
        //}
        _camera.position = calculateNewPosition(_camera.position, _camera.angle);
        // deal with a jump
        if (_jumpFrameCount > 0) {
            _jumpFrameCount--;
            if (_jumpFrameCount == 0) {
                y += JUMP_HEIGHT;
                if (_movement & (MOVEMENT_RIGHT | MOVEMENT_LEFT)) {
                    keyAction(true, MOVEMENT_DRIFT);
                }
            }
        }
    }

    protected function keyAction (inMotion :Boolean, flag :int) :void
    {
        if (inMotion) {
            _movement |= flag;
        } else {
            // if we're turning off drifting and it was on), cut back _currentSpeed
            if (flag == MOVEMENT_DRIFT && (_movement & MOVEMENT_DRIFT)) {
                _currentSpeed *= (DRIFT_X_SPEED_FACTOR + DRIFT_Y_SPEED_FACTOR) / 2;
            }
            _movement &= ~flag;
        }
    }

   
    /** reference to the camera object */
    protected var _camera :Camera;

    /** reference to the ground object */
    protected var _ground :Ground;

    /** The current angle viewed on the kart by a player in the 3rd person, i.e. the one driving */
    protected var _current3PAngle :Number = 0;

    /** turning constants */
    protected static const TURN_VIEW_ANGLE :int = 15; // in degrees
    protected static const DRIFT_VIEW_ANGLE :int = 45; // in degrees

    /** values to control jumping */
    protected static const JUMP_DURATION :int = 3;
    protected static const JUMP_HEIGHT :int = 15;

    /** Factor to cut speed by when driving off-road */
    protected static const TERRAIN_SPEED_FACTOR :Number = 0.2;
}
}
