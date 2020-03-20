package pdx_team_one;
import battlecode.common.*;
import java.util.HashSet;

//Parent class for all other robots

public abstract class Robot {
    static RobotController rc;

    //communications messages
    protected int TEAM_ID;
    protected final int HQ_LOCATION = 0;
    protected final int SOUPS_FOUND = 1;
    protected final int REFINERY_BUILT = 2;
    protected final int DEFENSE = 3;
    protected final int DS_SECURE = 4;
    protected final int START_PHASE_2 = 5;
    protected final int NEED_DELIVERY = 6;
    protected final int INNER_SPOTS_FILLED = 7;
    protected final int WALL_SPOTS_FILLED = 8;
    protected final int VAPORATOR_BUILT = 9;
    protected final int DRONE_HOME = 10;

    //message importance
    protected final int DEFCON5 = 1;
    protected final int DEFCON4 = 2;

    protected int hqElevation;
    protected MapLocation HQ = null;
    protected int lastBlockRead = 1;

    protected boolean left, right, upper, lower;

    protected boolean constriction = false;

    //TEAM_ID here is only set differently so the bot can scrimmage itself without conflict
    public Robot(RobotController r) {
        rc = r;
        if (rc.getTeam() == Team.A)
            TEAM_ID = 1111;
        else
            TEAM_ID = 2222;
    }

    public abstract void takeTurn() throws GameActionException;

    protected static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    protected static Direction[] corners = {
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST

    };

    //returns a random direction
    protected static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    //try to build a bot in the given direction
    protected boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    //try to send a blockchain message
    protected boolean sendMessage(int[] message, int cost) throws GameActionException {
        if (cost > rc.getTeamSoup())
            cost = rc.getTeamSoup();
        if (rc.canSubmitTransaction(message, cost)) {
            rc.submitTransaction(message, cost);
            return true;
        }
        return false;
    }

    //returns the closest map location from an array of maplocations
    protected MapLocation closestLocation(MapLocation[] m){
        MapLocation ret = m[0];
        for (MapLocation ml : m){
            if (rc.getLocation().distanceSquaredTo(ml) < rc.getLocation().distanceSquaredTo(ret))
                ret = ml;
        }
        return ret;
    }
    //broadcast undiscovered soup locations
    protected void broadcastSoup(MapLocation[] soups) throws GameActionException {
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = SOUPS_FOUND;
        int i;
        for (i = 0; i < soups.length && i < 5; i++)
            msg[i+2] = 100*soups[i].x + soups[i].y;
        sendMessage(msg, DEFCON5);
    }

    //determines whether HQ is close to an edge
    private void initCorner(){
        left = (HQ.x < 4);
        right = (rc.getMapWidth() - HQ.x <= 4);
        upper = (rc.getMapHeight() - HQ.y <= 4);
        lower = (HQ.y < 4);
    }

    //determines the innerspots for landscapers
    protected HashSet<MapLocation> initInnerSpots(){
        initCorner();
        HashSet<MapLocation> spots = new HashSet<>();
        for (Direction dir : corners) {
            if(rc.onTheMap(HQ.add(dir)))
                spots.add(HQ.add(dir));
        }

        //special case for a dumb map called 'constriction'
        if (((HQ.x == 3 && HQ.y == 36) || (HQ.x == 36 && HQ.y == 3)) && hqElevation == 5) {
            constriction = true;
            return spots;
        }

        if (upper && !right && rc.onTheMap(HQ.translate(1,3)))
            spots.add(HQ.translate(1,3));
        if (upper && !left && rc.onTheMap(HQ.translate(-1,3)))
            spots.add(HQ.translate(-1,3));
        if (lower && !right && rc.onTheMap(HQ.translate(1,-3)))
            spots.add(HQ.translate(1,-3));
        if (lower && !left && rc.onTheMap(HQ.translate(-1,-3)))
            spots.add(HQ.translate(-1,-3));
        if (right && !upper && rc.onTheMap(HQ.translate(3,1)))
            spots.add(HQ.translate(3,1));
        if (right && !lower && rc.onTheMap(HQ.translate(3,-1)))
            spots.add(HQ.translate(3,-1));
        if (left && !upper && rc.onTheMap(HQ.translate(-3,1)))
            spots.add(HQ.translate(-3,1));
        if (left && !lower && rc.onTheMap(HQ.translate(-3,-1)))
            spots.add(HQ.translate(-3,-1));

        if (upper && right)
            spots.remove(HQ.translate(1,1));
        if (upper && left)
            spots.remove(HQ.translate(-1,1));
        if (lower && right)
            spots.remove(HQ.translate(1, -1));
        if (lower && left)
            spots.remove(HQ.translate(-1, -1));
        return spots;
    }

    //determines where the wall will be built
    protected HashSet<MapLocation> initWallSpots() {
        HashSet<MapLocation> spots = new HashSet<>();
        MapLocation m;
        for (Direction dir : directions) {
            m = HQ.add(dir).add(dir);
            if (rc.onTheMap(m))
                spots.add(m);
            m = HQ.add(dir).add(dir.rotateRight());
            if (rc.onTheMap(m))
                spots.add(m);
        }

        //special case of a stupid map called 'constriction
        if (((HQ.x == 3 && HQ.y == 36) || (HQ.x == 36 && HQ.y == 3)) && hqElevation == 5)
            return spots;
        initCorner();

        //check left bounds
        if(left) {
            spots.remove(HQ.translate(-2, -1));
            spots.remove(HQ.translate(-2, 0));
            spots.remove(HQ.translate(-2, 1));
            if (rc.onTheMap(HQ.translate(-3, -2)) && !lower)
                spots.add(HQ.translate(-3, -2));
            if (rc.onTheMap(HQ.translate(-3, 2)) && !upper)
                spots.add(HQ.translate(-3, 2));
        }

        //check right bounds
        if(right) {
            spots.remove(HQ.translate(2, -1));
            spots.remove(HQ.translate(2, 0));
            spots.remove(HQ.translate(2, 1));
            if (rc.onTheMap(HQ.translate(3, -2)) && !lower)
                spots.add(HQ.translate(3, -2));
            if (rc.onTheMap(HQ.translate(3, 2)) && !upper)
                spots.add(HQ.translate(3, 2));
        }


        //check upper bounds
        if(upper) {
            spots.remove(HQ.translate(-1, 2));
            spots.remove(HQ.translate(0, 2));
            spots.remove(HQ.translate(1, 2));
            if (rc.onTheMap(HQ.translate(-2, 3)) && !left)
                spots.add(HQ.translate(-2, 3));
            if (rc.onTheMap(HQ.translate(2, 3)) && !right)
                spots.add(HQ.translate(2, 3));
        }

        //check lower bounds
        if(lower) {
            spots.remove(HQ.translate(-1, -2));
            spots.remove(HQ.translate(0, -2));
            spots.remove(HQ.translate(1, -2));
            if (rc.onTheMap(HQ.translate(-2, -3)) && !left)
                spots.add(HQ.translate(-2, -3));
            if (rc.onTheMap(HQ.translate(2, -3)) && !right)
                spots.add(HQ.translate(2, -3));
        }

        if (upper && right)
            spots.remove(HQ.translate(2,2));
        if (upper && left)
            spots.remove(HQ.translate(-2,2));
        if (lower && right)
            spots.remove(HQ.translate(2,-2));
        if (lower && left)
            spots.remove(HQ.translate(-2,-2));
        return spots;
    }

    //init the outside wall spots
    protected HashSet<MapLocation> initOuterSpots() {
        initCorner();
        HashSet<MapLocation> spots = new HashSet<>();
        MapLocation m;
        for (Direction dir : directions) {
            m = HQ.add(dir).add(dir).add(dir);
            if (rc.onTheMap(m))
                spots.add(m);
            m = HQ.add(dir).add(dir).add(dir.rotateRight());
            if (rc.onTheMap(m))
                spots.add(m);
            m = HQ.add(dir).add(dir).add(dir.rotateLeft());
            if (rc.onTheMap(m))
                spots.add(m);
        }

        //yep, it's 'constriction' again
        if (((HQ.x == 3 && HQ.y == 36) || (HQ.x == 36 && HQ.y == 3)) && hqElevation == 5)
            return spots;

        if(left) {
            for (int i = -2; i <= 2; i++)
                spots.remove(HQ.translate(-3, i));
            if(lower)
                spots.remove(HQ.translate(-3,-3));
            else if (upper)
                spots.remove(HQ.translate(-3,3));
        }
        if(right) {
            for (int i = -2; i <= 2; i++)
                spots.remove(HQ.translate(3, i));
            if(lower)
                spots.remove(HQ.translate(3,-3));
            else if (upper)
                spots.remove(HQ.translate(3,3));
        }
        if(upper) {
            for (int i = -2; i <= 2; i++)
                spots.remove(HQ.translate(i, 3));
        }
        if(lower) {
            for (int i = -2; i <= 2; i++)
                spots.remove(HQ.translate(i, -3));
        }
        return spots;
    }

    //returns the elevation difference between two maplocations
    protected int elevationDiff(MapLocation a, MapLocation b)throws GameActionException{
        int ae = rc.senseElevation(a);
        int be = rc.senseElevation(b);
        if (ae > be)
            return (ae-be);
        return (be-ae);
    }
}
