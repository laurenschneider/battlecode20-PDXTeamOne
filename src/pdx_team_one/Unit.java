package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.HashSet;

//abstract class for units. Contains moving functions
public abstract class Unit extends Robot{

    public Unit(RobotController rc){
        super(rc);
    }

    //try to move in a given direction
    protected boolean tryMove(Direction dir) throws GameActionException {
        if(dir == Direction.CENTER)
            return false;
        if (rc.isReady() && rc.canMove(dir)) {
            if (rc.senseFlooding(rc.adjacentLocation(dir)) && rc.getType() != RobotType.DELIVERY_DRONE)
                return false;
            rc.move(dir);
            return true;
        } else return false;
    }

    //attempt #1 at A* pathfinding
    //works great for short distances, takes too long if there are too many obstacles though
    //must be done in 1 turn; a changing map means calculations can't carry over
    protected void pathTo(MapLocation target) throws GameActionException {
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


        //ArrayList chosen because we need to insert at specific locations. Otherwise, Queue would've been used
        //Custom queue structure probably would've been best
        ArrayList<Node> openList = new ArrayList<>();

        //only need to check if location has been visited, so HashSet works fine. Boolean 2D array would have
        //faster lookup, but with large map it takes too long to initialize.
        HashSet<MapLocation> closedList = new HashSet<>();

        Node start = new Node(rc.getLocation(), null);

        openList.add(start);

        Direction move = rc.getLocation().directionTo(target);

        while (!openList.isEmpty()) {
            Node curr = openList.get(0);

            if (curr.parent == start)
                move = rc.getLocation().directionTo(curr.location);


            if (curr.location.equals(target) || Clock.getBytecodesLeft() < 1500) {
                tryMove(move);
                return;
            }

            openList.remove(curr);
            closedList.add(curr.location);

            for (Direction dir : directions) {
                if (closedList.contains(curr.location.add(dir)))
                    continue;
                if (!canTraverse(curr.location, curr.location.add(dir), target))
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
    protected boolean canTraverse(MapLocation a, MapLocation b, MapLocation target) throws GameActionException {
        if (!rc.onTheMap(b))
            return false;
        if (!rc.canSenseLocation(a) || !rc.canSenseLocation(b))
            return true;
        if (rc.canSenseLocation(b) && rc.isLocationOccupied(b) && !b.equals(target))
            return false;
        if (rc.getType() == RobotType.DELIVERY_DRONE)
            return true;
        return (elevationDiff(a,b) <= 3);
    }
}
