package pdx_team_one;
import battlecode.common.*;

//Parent class for all other robots

public abstract class Robot {
    static RobotController rc;

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

    static void sendMessage(int message[],int cost) throws GameActionException {
        if (rc.canSubmitTransaction(message,  cost))
            rc.submitTransaction(message, cost);
    }
}
