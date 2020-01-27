package pdx_team_one;
import battlecode.common.*;

//Parent class for all other robots

public abstract class Robot {
    static RobotController rc;

    static final int TEAM_ID = 11111111;
    static final int HQ_LOCATION = 0;
    static final int HQ_FLOOD_DANGER = 1;
    static final int DESIGN_SCHOOL_BUILT = 2;

    public Robot(RobotController r) {
        rc = r;
    }

    public abstract void takeTurn() throws GameActionException;

    static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static void sendMessage(int[] message,int cost) throws GameActionException {
        if (rc.canSubmitTransaction(message,  cost))
            rc.submitTransaction(message, cost);
    }

    static void pathTo(MapLocation ml)throws GameActionException{
        Direction toward = rc.getLocation().directionTo(ml);
        while (!tryMove(toward)){
            toward = toward.rotateLeft();
        }
    }
}
