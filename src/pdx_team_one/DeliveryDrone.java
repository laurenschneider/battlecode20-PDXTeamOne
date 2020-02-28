package pdx_team_one;
import battlecode.common.*;

import java.util.HashMap;

public class DeliveryDrone extends Robot{

    private HashMap<Integer, MapLocation> landscapers = new HashMap<>();
    private RobotInfo holding;
    private static Direction waterPath = Direction.NORTH;

    DeliveryDrone(RobotController r) throws GameActionException
    {
        super(r);
        for (int i = 1; i < rc.getRoundNum(); i++)
            parseBlockchain(i);
    }

    public void takeTurn() throws GameActionException {
        parseBlockchain(rc.getRoundNum()-1);
        runDeliveryDrone();
    }

    public void setHolding(RobotInfo ri) {
        this.holding = ri;
    }

    public void addLandscaper(Integer key, MapLocation loc) {
        this.landscapers.put(key, loc);
    }


    public int parseBlockchain(int i) throws GameActionException {
        int res = 0;
        for (Transaction t : rc.getBlock(i)) {
            if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_LOCATION) {
                HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                hqID = t.getMessage()[4];
                res = 1;
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == ENEMY_HQ_FOUND) {
                enemyHQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                enemyHQID = t.getMessage()[4];
                res = 2;
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == ENEMY_NG_FOUND) {
                enemyNG = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_TARGET_ACQUIRED) {
                landscapers.put(t.getMessage()[4], new MapLocation(t.getMessage()[2],t.getMessage()[3]));
                res = 3;
            }
        }
        return res;
    }


    int holdingFriend() throws GameActionException {
        int res = 0;
        if (rc.getLocation().equals(landscapers.get(holding.ID))){
            for (Direction dir : directions)
                tryMove(dir);
        }
        else if (rc.getLocation().isAdjacentTo(landscapers.get(holding.ID))) {
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
        if (enemyHQ == null) {
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if (r.team == enemy && r.type == RobotType.NET_GUN){
                    enemyNG = r.location;
                    int[] msg = new int[7];
                    msg[0] = TEAM_ID;
                    msg[1] = ENEMY_NG_FOUND;
                    msg[2] = enemyNG.x;
                    msg[3] = enemyNG.y;
                    msg[4] = r.ID;
                    sendMessage(msg,DEFCON1);
                }
                if (r.team == enemy && r.type == RobotType.HQ) {
                    enemyHQ = r.location;
                    int[] msg = new int[7];
                    msg[0] = TEAM_ID;
                    msg[1] = ENEMY_HQ_FOUND;
                    msg[2] = enemyHQ.x;
                    msg[3] = enemyHQ.y;
                    msg[4] = r.ID;
                    sendMessage(msg,DEFCON1);
                }
            }
        }
        int res = 0;
        if (rc.isCurrentlyHoldingUnit()) {
           // System.out.println("I am holding a unit!");
            if (holding.team == enemy) {
                //System.out.println("It's an enemy!");
                for (Direction dir: directions){
                    if (rc.canSenseLocation(rc.getLocation().add(dir)) && rc.senseFlooding(rc.getLocation().add(dir))){
                        rc.dropUnit(dir);
                        return 9;
                    }
                }
                if (rc.getCooldownTurns() < 1) {
                    while (!tryMove(waterPath))
                        waterPath = randomDirection();
                }
            }
            else {
                //System.out.println("It's a friend!");
                holdingFriend();
            }
        } else if (rc.isReady()) {
            //System.out.println("I'm empty handed");
            RobotInfo[] robots = rc.senseNearbyRobots();
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
            if (enemyHQ == null && enemyNG == null){
                if (rc.getCooldownTurns() < 1) {
                    while (!tryMove(waterPath))
                        waterPath = randomDirection();
                }
            }
            else
                attack();
        }
        return res;
    }

    public void attack() throws GameActionException{
        if (enemyHQ != null)
            pathTo(enemyHQ);
        else
            pathTo(enemyNG);
    }
}