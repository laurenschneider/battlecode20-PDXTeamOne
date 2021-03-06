package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

//Parent class for all other robots

public abstract class Robot {
    static RobotController rc;

    static int TEAM_ID = 1111; // must be 4 digits for map transmission to work
    static final int HQ_LOCATION = 0;
    static final int ENEMY_NG_FOUND = 2;
    static final int ENEMY_HQ_FOUND = 3;
    static final int SOUPS_FOUND = 4;
    static final int HQ_TARGET_ACQUIRED = 5;
    static final int REFINERY_BUILT = 7;
    static final int ATTACK = 8;
    static final int DEFENSE = 9;
    static final int FC_SECURE = 10;
    static final int DS_SECURE = 11;
    static final int START_PHASE_2 = 12;
    static final int NEED_DELIVERY = 13;
    static final int INNER_SPOTS_FILLED = 14;
    static final int WALL_SPOTS_FILLED = 15;
    static final int VAPORATOR_BUILT = 16;
    static final int DRONE_HOME = 17;

    //message importance
    //HQ location
    static final int DEFCON5 = 1;
    //
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
    static MapLocation enemyNG = null;
    static int enemyHQID = 0;
    static int lastBlockRead = 1;

    static boolean constriction = false;

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

    static Direction[] corners = {
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST

    };

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if(dir == Direction.CENTER)
            return false;
        if (rc.isReady() && rc.canMove(dir)) {
            if (rc.senseFlooding(rc.adjacentLocation(dir)) && rc.getType() != RobotType.DELIVERY_DRONE)
                return false;
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
        if (!rc.isReady())
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


            if (curr.location.equals(target) || Clock.getBytecodesLeft() < 1500) {
                if (rc.canMove(move))
                    tryMove(move);
                return;
            }

            openList.remove(curr);
            closedList.add(curr.location);

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

    static public boolean defense() throws GameActionException {
        RobotInfo target = null;
        for (RobotInfo e : rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, rc.getTeam().opponent())) {
            if (rc.canShootUnit(e.ID)) {
                if (target == null)
                    target = e;
                else if (target.isCurrentlyHoldingUnit() == e.isCurrentlyHoldingUnit())
                    if (e.location.equals(closestLocation(new MapLocation[] {e.location,target.location})))
                        target = e;
                else if (e.isCurrentlyHoldingUnit())
                    target = e;
            }
        }
        if (target!=null) {
            rc.shootUnit(target.ID);
            return true;
        }
        return false;
    }

    static public MapLocation closestLocation(MapLocation[] m){
        MapLocation ret = m[0];
        for (MapLocation ml : m){
            if (rc.getLocation().distanceSquaredTo(ml) < rc.getLocation().distanceSquaredTo(ret))
                ret = ml;
        }
        return ret;
    }
    //broadcast undiscovered soup locations
    public boolean broadcastSoup(MapLocation[] soups) throws GameActionException {
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = SOUPS_FOUND;
        int i;
        for (i = 0; i < soups.length && i < 5; i++)
            msg[i+2] = 100*soups[i].x + soups[i].y;
        return sendMessage(msg, DEFCON5);
    }

    public HashSet<MapLocation> initInnerSpots(){
        boolean left,right,upper,lower;
        left = (HQ.x < 4);
        right = (rc.getMapWidth() - HQ.x <= 4);
        upper = (rc.getMapHeight() - HQ.y <= 4);
        lower = (HQ.y < 4);
        HashSet<MapLocation> spots = new HashSet<>();
        for (Direction dir : corners) {
            if(rc.onTheMap(HQ.add(dir)))
                spots.add(HQ.add(dir));
        }

        if (((HQ.x == 3 && HQ.y == 36) || (HQ.x == 36 && HQ.y == 3)) && hqElevation == 5) {
            constriction = true;
            return spots;
        }
        System.out.println(HQ.x + " " + HQ.y + " " + hqElevation);
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

    public HashSet<MapLocation> initWallSpots() {
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

        if (((HQ.x == 3 && HQ.y == 36) || (HQ.x == 36 && HQ.y == 3)) && hqElevation == 5)
            return spots;
        boolean left,right,upper,lower;
        left = (HQ.x < 4);
        right = (rc.getMapWidth() - HQ.x <= 4);
        upper = (rc.getMapHeight() - HQ.y <= 4);
        lower = (HQ.y < 4);

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

    public HashSet<MapLocation> initOuterSpots() {
        boolean left,right,upper,lower;
        left = (HQ.x < 4);
        right = (rc.getMapWidth() - HQ.x <= 4);
        upper = (rc.getMapHeight() - HQ.y <= 4);
        lower = (HQ.y < 4);
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

        if (((HQ.x == 3 && HQ.y == 36) || (HQ.x == 36 && HQ.y == 3)) && hqElevation == 5)
            return spots;
        if(left) {
            for (int i = -2; i <= 2; i++)
                spots.remove(HQ.translate(-3, i));
            if(lower)
                spots.remove(HQ.translate(-3,-3));
            if (upper)
                spots.remove(HQ.translate(-3,3));
        }
        if(right) {
            for (int i = -2; i <= 2; i++)
                spots.remove(HQ.translate(3, i));
            if(lower)
                spots.remove(HQ.translate(3,-3));
            if (upper)
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

    public int elevationDiff(MapLocation a, MapLocation b)throws GameActionException{
        int ae = rc.senseElevation(a);
        int be = rc.senseElevation(b);
        if (ae > be)
            return (ae-be);
        return (be-ae);
    }
}
