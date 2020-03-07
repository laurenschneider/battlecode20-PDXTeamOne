package pdx_team_one;
import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DeliveryDrone extends Robot{

    private HashMap<Integer, MapLocation> friends = new HashMap<>();
    private HashSet<MapLocation> water = new HashSet<>();
    private RobotInfo holding;
    private static Direction path = Direction.NORTH;
    private boolean ds_secure = false;
    private MapLocation home = null;
    private boolean attackStrat;
    private HashSet<MapLocation> innerSpots = new HashSet<>();
    private HashSet<MapLocation> wallSpots = new HashSet<>();
    private HashSet<MapLocation> outerSpots = new HashSet<>();
    private ArrayList<MapLocation> blockSoups = new ArrayList<>();

    DeliveryDrone(RobotController r) throws GameActionException
    {
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (home == null)
            home = rc.getLocation();
        innerSpots = initInnerSpots();
        wallSpots = initWallSpots();
        outerSpots = initOuterSpots();
    }

    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        scanSoup();
        if (rc.isReady()) {
            if (rc.isCurrentlyHoldingUnit()) {
                System.out.println("I am holding a unit!");
                if (holding.team == rc.getTeam())
                    deliverFriend();
                else
                    destroyEnemy();
            } else
                findSomethingToDo();
        }
    }

    public void scanSoup() throws GameActionException{
        MapLocation[] soups = rc.senseNearbySoup();
        ArrayList<MapLocation> toRemove = new ArrayList<>();
        if (soups.length > 0) {
            ArrayList<MapLocation> newSoup = new ArrayList<>();
            for (MapLocation soup : soups) {
                if (rc.senseFlooding(soup))
                    toRemove.add(soup);
                else if (!blockSoups.contains(soup))
                    newSoup.add(soup);
            }
            if (!newSoup.isEmpty())
                broadcastSoup(newSoup.toArray(new MapLocation[0]));
        }/*
        else{
            for (MapLocation soup : blockSoups){
                if (rc.canSenseLocation(soup))
                    toRemove.add(soup);
            }
        }*/
        blockSoups.removeAll(toRemove);
        System.out.println(Clock.getBytecodesLeft());
        return;
    }

     boolean deliverLS(HashSet<MapLocation> spots) throws GameActionException{
        ArrayDeque<MapLocation> toRemove = new ArrayDeque<>();
        MapLocation target = null;
        for (MapLocation m : spots) {
            System.out.println("Maybe my friend wants to go to " + m);
            if (rc.canSenseLocation(m)) {
                RobotInfo r = rc.senseRobotAtLocation(m);
                if (r != null && r.type == RobotType.LANDSCAPER)
                    toRemove.add(m);
                else if (target == null)
                    target = m;
                else if (rc.senseElevation(m) < rc.senseElevation(target))
                    target = m;
            }
        }
        spots.removeAll(toRemove);
        if (target != null){
            if (rc.getLocation().equals(target)){
                for (Direction dir : directions)
                    tryMove(dir);
            }
            else if (rc.getLocation().isAdjacentTo(target) && rc.canDropUnit(rc.getLocation().directionTo(target))) {
                System.out.println("Dropping him off at " + target);
                rc.dropUnit(rc.getLocation().directionTo(target));
                spots.remove(target);
                holding = null;
                return true;
            }
            else
                pathTo(target);
        }
        else if (!spots.isEmpty()) {
            target = closestLocation(spots.toArray(new MapLocation[0]));
            if (rc.getLocation().isAdjacentTo(target)){
                for (Direction dir : directions)
                    tryMove(dir);
            }
            System.out.println("Carrying friend to " + target);
            System.out.println(Clock.getBytecodesLeft() + "  " + rc.isReady());
            pathTo(target);
            return true;
        }
        return (!spots.isEmpty());
    }

    int deliverFriend() throws GameActionException {
        if (holding.type == RobotType.LANDSCAPER) {
            if (!innerSpots.isEmpty() && deliverLS(innerSpots))
                return 0;
            if (!wallSpots.isEmpty() && deliverLS(wallSpots))
                return 1;
            if (!outerSpots.isEmpty() && deliverLS(outerSpots))
                return 2;
            return 3;
        } else {
            ArrayList<MapLocation> toRemove = new ArrayList<>();
            for (MapLocation m : blockSoups) {
                if(rc.canSenseLocation(m) && rc.senseSoup(m) == 0)
                    toRemove.add(m);
                else if (!rc.getLocation().equals(m) && rc.getLocation().isAdjacentTo(m) && rc.canDropUnit(rc.getLocation().directionTo(m))) {
                    rc.dropUnit(rc.getLocation().directionTo(m));
                    return 4;
                }
            }
            blockSoups.removeAll(toRemove);
            if (blockSoups.isEmpty())
                scout();
            else
                pathTo(closestLocation(blockSoups.toArray(new MapLocation[0])));
            return 5;
        }
    }


    public void parseBlockchain(int i) throws GameActionException {
        for (Transaction t : rc.getBlock(i)) {
            if (t.getMessage()[0] == TEAM_ID) {
                if (t.getMessage()[1] == HQ_LOCATION) {
                    HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    hqID = t.getMessage()[4];
                } else if (t.getMessage()[1] == ENEMY_HQ_FOUND) {
                    enemyHQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    enemyHQID = t.getMessage()[4];
                } else if (t.getMessage()[1] == ENEMY_NG_FOUND) {
                    enemyNG = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                } else if (t.getMessage()[1] == NEED_DELIVERY) {
                    friends.put(t.getMessage()[4], new MapLocation(t.getMessage()[5], t.getMessage()[6]));
                    System.out.println("Incoming message! Friend " + t.getMessage()[4] + " is located at " + friends.get(t.getMessage()[4]));
                } else if (t.getMessage()[1] == DS_SECURE) {
                    ds_secure = true;
                } else if (t.getMessage()[1] == DEFENSE) {
                    MapLocation ds = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
                    for (Direction dir : directions) {
                        if (rc.onTheMap(ds.add(dir).add(dir))) {
                            home = ds.add(dir).add(dir);
                            break;
                        }
                    }
                } else if (t.getMessage()[1] == SOUPS_FOUND) {
                    for (int j = 2; j < 7 && t.getMessage()[j] != 0; j++)
                        blockSoups.add(new MapLocation(t.getMessage()[j] / 100, t.getMessage()[j] % 100));
                } else if (t.getMessage()[1] == INNER_SPOTS_FILLED) {
                    innerSpots.clear();
                } else if (t.getMessage()[1] == WALL_SPOTS_FILLED) {
                    wallSpots.clear();
                }
            }
        }
    }

    public int pickupUnit(MapLocation ml)throws GameActionException {
        System.out.println("Gonna go pick up a bot at " + ml);
        if (rc.canSenseLocation(ml)) {
            RobotInfo r = rc.senseRobotAtLocation(ml);
            if (rc.getLocation().isAdjacentTo(ml) && rc.canPickUpUnit(r.ID)) {
                System.out.println("Picking up " + r.ID);
                holding = r;
                rc.pickUpUnit(r.ID);
                if (friends.containsKey(r.ID))
                    friends.remove(r.ID);
                return 1;
            }
        }
        pathTo(ml);
        return 0;
    }

    public MapLocation nearestEnemy() throws GameActionException{
        System.out.println("Checking for enemies");
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
                return r.location;
        }
        return null;
    }

    public MapLocation nearestFriendInNeed(){
        System.out.println("Checking for friends");
        MapLocation target = null;
        for (RobotInfo r : rc.senseNearbyRobots(-1,rc.getTeam())) {
            if (friends.containsKey(r.ID)) {
                if (friends.get(r.ID).equals(r.location)) {
                    if (target == null)
                        target = r.location;
                    else
                        target = closestLocation(new MapLocation[]{target, r.location});
                }
                else
                    friends.remove(r.ID);
            } else if (ds_secure && r.type == RobotType.LANDSCAPER) {
                if (!innerSpots.isEmpty()){
                    if (!r.location.isAdjacentTo(HQ)) {
                        System.out.println("The bot at " + r.location + " needs to be picked up");
                        if (target == null)
                            target = r.location;
                        else
                            target = closestLocation(new MapLocation[]{target, r.location});
                    }
                }
                else if (!wallSpots.isEmpty()) {
                    if (r.location.distanceSquaredTo(HQ) > 8) {
                        System.out.println("The bot at " + r.location + " needs to be picked up");
                        if (target == null)
                            target = r.location;
                        else
                            target = closestLocation(new MapLocation[]{target, r.location});
                    }
                }
                else if (!outerSpots.isEmpty()) {
                    if (r.location.distanceSquaredTo(HQ) > 13 && r.location.distanceSquaredTo(HQ) != 18) {
                        System.out.println("The bot at " + r.location + " needs to be picked up");
                        if (target == null)
                            target = r.location;
                        else
                            target = closestLocation(new MapLocation[]{target, r.location});
                    }
                }
            }
        }
        return target;
    }

    public MapLocation findACow(){
        System.out.println("Checking for cows");
        ArrayList <MapLocation> cow = new ArrayList<>();
        for (RobotInfo r : rc.senseNearbyRobots(-1,Team.NEUTRAL)){
            cow.add(r.location);
        }
        if (cow.isEmpty())
            return null;
        return closestLocation(cow.toArray(new MapLocation[0]));
    }

    public int findSomethingToDo() throws GameActionException {
        MapLocation target = nearestEnemy();
        if (target == null)
            target = nearestFriendInNeed();
        if (target == null)
            target = findACow();
        if (target != null)
            return pickupUnit(target);
        if (rc.getCurrentSensorRadiusSquared() <= 1) {
            for (Direction dir : directions)
                tryMove(dir);
            return 2435;
        }
        if (!friends.isEmpty()) {
            for (int key : friends.keySet()) {
                if (rc.canSenseLocation(friends.get(key)))
                    friends.remove(key);
            }
            if (!friends.isEmpty()) {
                pathTo(closestLocation(friends.values().toArray(new MapLocation[0])));
                return 253452345;
            }
        }
        /*if (rc.getLocation().distanceSquaredTo(home) < 10)
            scout();
        else*/
        pathTo(home);
        return 666;
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