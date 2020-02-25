package pdx_team_one;
import battlecode.common.*;

import java.util.HashMap;
import java.util.Map;

public class DeliveryDrone extends Robot{

    private HashMap<Integer, MapLocation> landscapers = new HashMap<>();
    private RobotInfo holding;

    DeliveryDrone(RobotController r) throws GameActionException
    {
        super(r);
//        map = new int[rc.getMapWidth()][rc.getMapHeight()];
        for (int i = 1; i < rc.getRoundNum(); i++)
            parseBlockchain(i);
    }

    public void setHolding(RobotInfo ri) {
        this.holding = ri;
    }

    public void addLandscaper(Integer key, MapLocation loc) {
        this.landscapers.put(key, loc);
    }

    public void takeTurn() throws GameActionException {
        parseBlockchain(rc.getRoundNum()-1);
        runDeliveryDrone();
    }

    public int parseBlockchain(int i) throws GameActionException {
        int res = 0;
        for (Transaction t : rc.getBlock(i)) {
            if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_LOCATION) {
                HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                t.getMessage()[4] = hqID;
                res = 1;
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == ENEMY_HQ_FOUND) {
                enemyHQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                t.getMessage()[4] = enemyHQID;
                res = 2;
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_TARGET_ACQUIRED) {
                landscapers.put(t.getMessage()[3], HQ.add(directions[t.getMessage()[2]]));
                res = 3;
            }
        }
        return res;
    }

    int adjacentHQMoves() throws GameActionException {
        int res = 0;
        //System.out.println("I need to get out of the way");
        if (rc.canMove(rc.getLocation().directionTo(HQ).opposite())) {
            tryMove(rc.getLocation().directionTo(HQ).opposite());
            res = 1;
        } else if (rc.canMove(rc.getLocation().directionTo(HQ).opposite().rotateLeft())) {
            tryMove(rc.getLocation().directionTo(HQ).opposite().rotateLeft());
            res = 2;
        } else if (rc.canMove(rc.getLocation().directionTo(HQ).opposite().rotateRight())) {
            tryMove(rc.getLocation().directionTo(HQ).opposite().rotateRight());
            res = 3;
        }
        return res;
    }

    int holdingFriend() throws GameActionException {
        int res = 0;
        if (rc.getLocation().isAdjacentTo(landscapers.get(holding.ID))) {
            // System.out.println("Im adjacent to his landing spot!");
            if (rc.canDropUnit(rc.getLocation().directionTo(landscapers.get(holding.ID)))) {
                rc.dropUnit(rc.getLocation().directionTo(landscapers.get(holding.ID)));
                holding = null;
                //System.out.println("Dropped him off!");
            }
            res = 1;
        }
        else {
            //System.out.println("trying to move to " + landscapers.get(holding.ID));
            pathTo(landscapers.get(holding.ID));
            res = 2;
        }
        return res;
    }

    boolean nearbyEnemy(RobotInfo r) throws GameActionException {
        boolean res = false;
        if (rc.getLocation().isAdjacentTo(r.getLocation())) {
            if (rc.canPickUpUnit(r.getID())) {
                rc.pickUpUnit(r.getID());
                holding = r;
                rc.move(randomDirection());
                rc.dropUnit(rc.getLocation().directionTo(HQ).opposite());
                res = true;
            } else {
                pathTo(r.location);
            }
        }
        return res;
    }

    int nearbyLandscapers(RobotInfo r) throws GameActionException {
        int res = 0;
        // System.out.println(r.ID + " needs to move to " + landscapers.get(r.ID));
        if (rc.getLocation().isAdjacentTo(r.location)) {
            //System.out.println("I'm right next to him!");
            if (rc.canPickUpUnit(r.getID())) {
                //System.out.println("Got him");
                rc.pickUpUnit(r.getID());
                holding = r;
                res = 1;
            }
        } else {
            //System.out.println("Moving toward him");
            pathTo(r.location);
            res =2;
        }
        return res;
    }

    public int runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int res = 0;
        if (rc.isCurrentlyHoldingUnit()) {
           // System.out.println("I am holding a unit!");
            if (holding.team == enemy) {
                //System.out.println("It's an enemy!");
                tryMove(randomDirection());
            }
            else {
                //System.out.println("It's a friend!");
                holdingFriend();
            }
        } else if (rc.isReady()) {
            //System.out.println("I'm empty handed");
            RobotInfo[] robots = rc.senseNearbyRobots();
            if (robots.length == 0) {
                //System.out.println("Nobody near me, let's head back to HQ");
                pathTo(HQ);
            }
            for (RobotInfo r : robots) {
                if (r.team == enemy) {
                    //System.out.println("There are enemies near by!");
                    boolean shouldBreak = nearbyEnemy(r);
                    if(shouldBreak)
                        break;
                } else if (r.type == RobotType.LANDSCAPER) {
                    // System.out.println("There are landscapers nearby");
                    if (landscapers.containsKey(r.ID) && !r.location.equals(landscapers.get(r.ID))) {
                        return nearbyLandscapers(r);
                    }
                }
            }
            if (rc.getLocation().isAdjacentTo(HQ)){
                res = adjacentHQMoves();
            }
        }
        return res;
    }
}