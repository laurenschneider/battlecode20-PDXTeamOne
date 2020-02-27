package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;

//Parent class for all other robots

public abstract class Robot {
    static RobotController rc;

    static int TEAM_ID = 1111; // must be 4 digits for map transmission to work
    static final int HQ_LOCATION = 0;
    static final int DESIGN_SCHOOL_BUILT = 2;
    static final int ENEMY_HQ_FOUND = 3;
    static final int SOUPS_FOUND = 4;
    static final int HQ_TARGET_ACQUIRED = 5;
    static final int FULFILLMENT_CENTER_BUILT = 6;
    static final int REFINERY_BUILT = 7;

    //message importance
    //map locations
    static final int DEFCON5 = 1;
    //HQ location
    static final int DEFCON4 = 2;
    //soup locations, design school built, landscaper target acquired
    static final int DEFCON3 = 3;
    //
    static final int DEFCON2 = 4;
    //HQ in trouble/enemy HQ found
    static final int DEFCON1 = 5;

    static int hqElevation;
    static MapLocation HQ = null;
    static int hqID = 0;
    static boolean design_school = false;
    public static boolean fulfillment_center = false;
    static MapLocation enemyHQ = null;
    static int enemyHQID = 0;

    //TEAM_ID here is changed here for debugging because I can't figure out how
    //to scrimmage against a different team locally
    public Robot(RobotController r) {
        rc = r;
        if (rc.getTeam() == Team.A)
            TEAM_ID = 1111;
        else
            TEAM_ID = 2222;
    }

    public abstract void takeTurn() throws GameActionException;

    //strictly for debugging and measuring bytecode performance
    static private int start, end, startturn, endturn;

    static void start() {
        start = Clock.getBytecodesLeft();
        startturn = rc.getRoundNum();
    }

    static void end(String s) {
        end = Clock.getBytecodesLeft();
        endturn = rc.getRoundNum();
        System.out.println("It took me " + (endturn - startturn) + " turns and " + (start - end) + " bytecodes to " + s);
    }


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
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static boolean sendMessage(int[] message, int cost) throws GameActionException {
        if (cost > rc.getTeamSoup())
            cost = rc.getTeamSoup();
        if (rc.canSubmitTransaction(message, cost)) {
            rc.submitTransaction(message, cost);
            return true;
        }
        return false;
    }

    //todo: improve this
    //attempt #1 at A* pathfinding
    //works great for short distances, takes too long if there are too many obstacles though
    //must be done in 1 turn; a changing map means calculations can't carry over
    public void pathTo(MapLocation target) throws GameActionException {
        if (rc.getCooldownTurns() >= 1)
            return;

        class Node {
            private int g = 0;
            private int h = 0;
            private int f = 0;
            private MapLocation location;
            private Node parent;

            private Node(MapLocation ml, Node p) {
                location = ml;
                parent = p;
            }
        }


        ArrayList<Node> openList = new ArrayList<>();
        ArrayList<MapLocation> closedList = new ArrayList<>();

        Node start = new Node(rc.getLocation(), null);

        openList.add(start);

        Direction move = rc.getLocation().directionTo(target);

        while (!openList.isEmpty()) {
            Node curr = openList.get(0);

            if (curr.parent == start)
                move = rc.getLocation().directionTo(curr.location);

            openList.remove(curr);
            closedList.add(curr.location);

            if (curr.location.equals(target) || Clock.getBytecodesLeft() < 1000) {
                if (rc.canMove(move))
                    tryMove(move);
                return;
            }

            for (Direction dir : directions) {
                if (!canTraverse(curr.location, curr.location.add(dir), target))
                    continue;
                if (closedList.contains(curr.location.add(dir)))
                    continue;
                Node child = new Node(curr.location.add(dir), curr);
                child.g = curr.g + 1;
                child.h = child.location.distanceSquaredTo(target);
                child.f = child.g + child.h;
                int i;
                for (i = 0; i < openList.size(); i++) {
                    if (child.f < openList.get(i).f) {
                        break;
                    }
                }
                openList.add(i, child);
            }
        }
    }
    //returns whether it's possible to move from point a to point b
    static private boolean canTraverse(MapLocation a, MapLocation b, MapLocation target) throws GameActionException {
        if (!rc.onTheMap(b))
            return false;
        if (!rc.canSenseLocation(a) || !rc.canSenseLocation(b))
            return true;
        if (rc.canSenseLocation(b) && rc.isLocationOccupied(b) && !b.equals(target))
            return false;
        if (rc.getType() == RobotType.DELIVERY_DRONE)
            return true;
        int diff = rc.senseElevation(a) - rc.senseElevation(b);
        return (diff >= -3 && diff <= 3);
    }
}
