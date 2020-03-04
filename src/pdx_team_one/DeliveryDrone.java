package pdx_team_one;
import battlecode.common.*;

import java.util.HashMap;
import java.util.HashSet;

public class DeliveryDrone extends Robot{

    private int PICKUP = 1;
    private int DROPOFF = 0;
    private HashMap<Integer, MapLocation[]> friends = new HashMap<>();
    private HashSet<MapLocation> water = new HashSet<>();
    private RobotInfo holding;
    private static Direction path = Direction.NORTH;
    private boolean minerDeliver;
    private boolean ds_secure = false;
    private MapLocation home;

    DeliveryDrone(RobotController r) throws GameActionException
    {
        super(r);
        for (int i = 1; i < rc.getRoundNum(); i++)
            parseBlockchain(i);
        home = rc.getLocation();
    }

    public void takeTurn() throws GameActionException {
        parseBlockchain(rc.getRoundNum()-1);
        if (rc.isReady()) {
            if (rc.isCurrentlyHoldingUnit()) {
                if (holding.team == rc.getTeam())
                    deliverFriend();
                else
                    destroyEnemy();
            } else
                findSomethingToDo();
        }
    }

    int deliverFriend() throws GameActionException {
        int res = 0;
        System.out.println("I have a friend! his ID is " + holding.ID);
        System.out.println("He needs to go to " + friends.get(holding.ID)[DROPOFF]);
        if (rc.getLocation().equals(friends.get(holding.ID)[DROPOFF])){
            for (Direction dir : directions)
                tryMove(dir);
        }
        else if (rc.getLocation().isAdjacentTo(friends.get(holding.ID)[DROPOFF])) {
            System.out.println("Im adjacent to his landing spot!");
            //todo: what if everywhere is flooded?
            if (rc.senseFlooding(friends.get(holding.ID)[DROPOFF])){
                for (Direction dir : directions){
                    if (rc.senseFlooding(rc.getLocation().add(dir)))
                        continue;
                    if(rc.canDropUnit(dir)) {
                        rc.dropUnit(dir);
                        friends.remove(holding.ID);
                        holding = null;
                        return 1;
                    }
                }
            }
            else if (rc.canDropUnit(rc.getLocation().directionTo(friends.get(holding.ID)[DROPOFF]))) {
                rc.dropUnit(rc.getLocation().directionTo(friends.get(holding.ID)[DROPOFF]));
                friends.remove(holding.ID);
                holding = null;
                System.out.println("Dropped him off!");
            }
            res = 1;
        }
        else {
            System.out.println("trying to move to " + friends.get(holding.ID)[DROPOFF]);
            pathTo(friends.get(holding.ID)[DROPOFF]);
            res = 2;
        }
        return res;
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
                friends.put(t.getMessage()[4], new MapLocation[]{new MapLocation(t.getMessage()[2], t.getMessage()[3]), new MapLocation(t.getMessage()[5], t.getMessage()[6])});
                res = 3;
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == NEED_DELIVERY) {
                friends.put(t.getMessage()[4], new MapLocation[]{new MapLocation(t.getMessage()[2], t.getMessage()[3]), new MapLocation(t.getMessage()[5], t.getMessage()[6])});
                res = 3;
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == DS_SECURE)
                ds_secure = true;
        }
        return res;
    }

    public int pickupUnit(MapLocation ml)throws GameActionException {
        if (rc.canSenseLocation(ml)) {
            RobotInfo r = rc.senseRobotAtLocation(ml);
            if (rc.canPickUpUnit(r.ID)) {
                System.out.println("Picking up " + r.ID);
                holding = r;
                rc.pickUpUnit(r.ID);
                return 1;
            }
        }
        pathTo(ml);
        return 0;
    }

    public int findSomethingToDo() throws GameActionException {
        for (RobotInfo r : rc.senseNearbyRobots(-1,rc.getTeam().opponent())) {
            if (enemyNG == null && r.type == RobotType.NET_GUN ) {
                enemyNG = r.location;
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = ENEMY_NG_FOUND;
                msg[2] = enemyNG.x;
                msg[3] = enemyNG.y;
                msg[4] = r.ID;
                sendMessage(msg, DEFCON1);
            } else if (enemyHQ == null && r.type == RobotType.HQ) {
                enemyHQ = r.location;
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = ENEMY_HQ_FOUND;
                msg[2] = enemyHQ.x;
                msg[3] = enemyHQ.y;
                msg[4] = r.ID;
                sendMessage(msg, DEFCON1);
            }
            else if (r.type == RobotType.LANDSCAPER || r.type == RobotType.MINER)
                return pickupUnit(r.location);
        }
        MapLocation target = null;
        for (RobotInfo r : rc.senseNearbyRobots(-1,rc.getTeam())){
            if (friends.containsKey(r.ID)){
                if (friends.get(r.ID)[DROPOFF].equals(r.location))
                    friends.remove(r.ID);
                else if (!ds_secure && r.type == RobotType.LANDSCAPER)
                    continue;
                else if (target == null)
                    target = r.location;
                else
                    target = closestLocation(new MapLocation[]{target, r.location});
            }
        }
        if (target != null)
            return pickupUnit(target);
        else if (friends.isEmpty()) {
            pathTo(home);
            return 666;
        }
        else {
            for (int id : friends.keySet()) {
                if (ds_secure && rc.getLocation().equals(friends.get(id)[PICKUP]))
                    friends.remove(id);
                else if (target == null)
                    target = friends.get(id)[PICKUP];
                else
                    target = closestLocation(new MapLocation[]{friends.get(id)[PICKUP],target});
            }
            if (target == null)
                pathTo(home);
            else
                pathTo(target);
        }
        return 0;
    }

    public int scout() throws GameActionException{
        while(!tryMove(path))
            path = randomDirection();
        return 0;
    }

    public int destroyEnemy() throws GameActionException{
        MapLocation m;
        for (Direction dir : directions) {
            m = rc.getLocation().add(dir);
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                if (rc.canDropUnit(dir)) {
                    rc.dropUnit(dir);
                    holding = null;
                    return 0;
                }
            }
        }
        for (Direction dir : directions) {
            m = rc.getLocation().add(dir).add(dir).add(dir);
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(rc.getLocation().add(dir).add(dir));
                return 1;
            }
            m = rc.getLocation().add(dir).add(dir).add(dir.rotateRight());
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(m);
                return 1;
            }
        }
        for (Direction dir : directions) {
            m = rc.getLocation().add(dir).add(dir).add(dir);
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(m);
                return 2;
            }
            m = rc.getLocation().add(dir).add(dir).add(dir.rotateLeft());
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(m);
                return 2;
            }
            m = rc.getLocation().add(dir).add(dir).add(dir.rotateRight());
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(m);
                return 2;
            }
        }
        if (water.isEmpty())
            scout();
        else
            pathTo(closestLocation(water.toArray(new MapLocation[0])));
        return 3;
    }
}