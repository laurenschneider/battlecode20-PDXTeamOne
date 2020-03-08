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
    private boolean innerSpotsFilled;
    private boolean wallSpotsFilled;
    private HashSet<MapLocation> innerSpots = new HashSet<>();
    private HashSet<MapLocation> wallSpots = new HashSet<>();
    private HashSet<MapLocation> outerSpots;
    private ArrayList<MapLocation> blockSoups = new ArrayList<>();
    private MapLocation builderDropoff = null;
    private MapLocation ds = null, fc = null;
    private int builderID = -1;
    private MapLocation builderPickup = null;
    private boolean cowSearch;
    int turnsHeld;

    DeliveryDrone(RobotController r) throws GameActionException {
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (!innerSpotsFilled)
            innerSpots = initInnerSpots();
        if (!wallSpotsFilled)
            wallSpots = initWallSpots();
        outerSpots = initOuterSpots();
        outerSpots.remove(fc);
        outerSpots.remove(ds);
        if (home == null)
            home = rc.getLocation();
    }

    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (rc.isReady()) {
            if (rc.isCurrentlyHoldingUnit()) {
                //System.out.println("I am holding a unit!");
                if (holding.team == rc.getTeam())
                    deliverFriend();
                else
                    destroyEnemy();
            } else {
                turnsHeld = 0;
                findSomethingToDo();
            }
        }
    }

    boolean deliverLS(HashSet<MapLocation> spots) throws GameActionException{
        ArrayDeque<MapLocation> toRemove = new ArrayDeque<>();
        MapLocation target = null;
        for (MapLocation m : spots) {
           // System.out.println("Maybe my friend wants to go to " + m);
            if (rc.canSenseLocation(m)) {
                RobotInfo r = rc.senseRobotAtLocation(m);
                if (r != null && r.type == RobotType.LANDSCAPER)
                    toRemove.add(m);
                else if (rc.senseFlooding(m) && rc.senseElevation(m) < -1000)
                    toRemove.add(m);
                else if (rc.senseFlooding(m))
                    continue;
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
                //System.out.println("Dropping him off at " + target);
                rc.dropUnit(rc.getLocation().directionTo(target));
                spots.remove(target);
                holding = null;
                return true;
            }
            else {
               // System.out.println("Before: " + Clock.getBytecodesLeft());
                pathTo(target);
                //System.out.println("After:  " + Clock.getBytecodesLeft());
            }
        }
        else if (!spots.isEmpty()) {
            target = closestLocation(spots.toArray(new MapLocation[0]));
            if (rc.getLocation().isAdjacentTo(target)){
                for (Direction dir : directions)
                    tryMove(dir);
            }
            //System.out.println("Carrying friend to " + target);
            //System.out.println(Clock.getBytecodesLeft() + "  " + rc.isReady());
            pathTo(target);
            return true;
        }
        return (!spots.isEmpty());
    }

    int deliverFriend() throws GameActionException {
        turnsHeld++;
        if (turnsHeld >= 50) {
            for (Direction dir : directions) {
                if (rc.canDropUnit(dir)) {
                    rc.dropUnit(dir);
                    turnsHeld = 0;
                }
            }
        }
        if (holding.ID == builderID && builderDropoff != null) {
            //System.out.println("Build needs to move to " + builderDropoff);
            if (rc.getLocation().equals(builderDropoff)) {
                for (Direction dir : directions)
                    tryMove(dir);
            } else if (rc.getLocation().isAdjacentTo(builderDropoff)) {
                if (rc.canDropUnit(rc.getLocation().directionTo(builderDropoff))) {
                    rc.dropUnit(rc.getLocation().directionTo(builderDropoff));
                    builderID = -1;
                    builderDropoff = null;
                    return 112;
                }
            } else
                pathTo(builderDropoff);
        } else if (holding.type == RobotType.LANDSCAPER) {
            if (!innerSpots.isEmpty() && deliverLS(innerSpots))
                return 0;
            if (!wallSpots.isEmpty() && deliverLS(wallSpots))
                return 1;
            if (!outerSpots.isEmpty() && deliverLS(outerSpots))
                return 2;
            return 3;
        } else {
            MapLocation target = null;
            for (MapLocation soup : rc.senseNearbySoup()) {
                if (rc.senseFlooding(soup) || soup.distanceSquaredTo(HQ) <= 18)
                    blockSoups.remove(soup);
                else if (target == null)
                    target = soup;
                else
                    target = closestLocation(new MapLocation[]{target,soup});
            }
            /*
            if (target == null && !blockSoups.isEmpty()) {
                ArrayList<MapLocation> toRemove = new ArrayList<>();
                for (MapLocation soup : blockSoups){
                    if (rc.canSenseLocation(soup))
                        toRemove.add(soup);
                    else if (target == null)
                    target = soup;
                    else
                        target = closestLocation(new MapLocation[]{target,soup});
                }
                blockSoups.removeAll(toRemove);
            }*/

            if (target == null) {
                if (blockSoups.isEmpty())
                    scout();
                else {
                    target = closestLocation(blockSoups.toArray(new MapLocation[0]));
                    if (rc.canSenseLocation(target)){
                        blockSoups.remove(target);
                        if (blockSoups.isEmpty()){
                            scout();
                            return 134;
                        }
                        else
                            target = closestLocation(blockSoups.toArray(new MapLocation[0]));
                    }
                   // System.out.println("Soup out of range. Taking my buddy to " + target + " with " + Clock.getBytecodesLeft());
                    pathTo(closestLocation(blockSoups.toArray(new MapLocation[0])));
                  //  System.out.println("Ending with " + Clock.getBytecodesLeft());

                }
            }
            else if (rc.getLocation().isAdjacentTo(target)){
                if(rc.canDropUnit(rc.getLocation().directionTo(target))){
                    rc.dropUnit(rc.getLocation().directionTo(target));
                    holding = null;
                }
                for (Direction dir : directions){
                    if (!rc.senseFlooding(rc.getLocation().add(dir)) &&rc.canDropUnit(dir)){
                        rc.dropUnit(dir);
                        holding = null;
                    }
                }
            }
            else {
               // System.out.println("Soup in range. Taking my buddy to " + target + " with " + Clock.getBytecodesLeft());
                pathTo(target);
                //System.out.println("Ending with " + Clock.getBytecodesLeft());
            }
            return 5;
        }
        return 999;
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
                    if (t.getMessage()[2] != -1) {
                        builderID = t.getMessage()[4];
                        builderPickup = new MapLocation(t.getMessage()[5], t.getMessage()[6]);
                        builderDropoff = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    } else {
                        friends.put(t.getMessage()[4], new MapLocation(t.getMessage()[5], t.getMessage()[6]));
                       // System.out.println("Incoming message! Friend " + t.getMessage()[4] + " is located at " + friends.get(t.getMessage()[4]));
                    }
                } else if (t.getMessage()[1] == DS_SECURE) {
                    ds_secure = true;
                } else if (t.getMessage()[1] == DEFENSE) {
                    ds = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
                    fc = new MapLocation(t.getMessage()[2],t.getMessage()[3]);
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
                    innerSpotsFilled = true;
                    innerSpots.clear();
                } else if (t.getMessage()[1] == WALL_SPOTS_FILLED) {
                    wallSpotsFilled = true;
                    wallSpots.clear();
                } else if (t.getMessage()[1] == START_PHASE_2) {
                    cowSearch = true;
                } else if (t.getMessage()[1] == DRONE_HOME) {
                    home = new MapLocation(t.getMessage()[2],t.getMessage()[3]);
                }
            }
        }
    }

    public int pickupUnit(MapLocation ml)throws GameActionException {
     //   System.out.println("Gonna go pick up a bot at " + ml);
        if (rc.canSenseLocation(ml)) {
            RobotInfo r = rc.senseRobotAtLocation(ml);
            if (rc.getLocation().isAdjacentTo(ml) && rc.canPickUpUnit(r.ID)) {
              //  System.out.println("Picking up " + r.ID);
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
      //  System.out.println("Checking for enemies");
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
       // System.out.println("Checking for friends");
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
                       // System.out.println("The bot at " + r.location + " needs to be picked up");
                        if (target == null)
                            target = r.location;
                        else
                            target = closestLocation(new MapLocation[]{target, r.location});
                    }
                }
                else if (!wallSpots.isEmpty()) {
                    if (r.location.distanceSquaredTo(HQ) > 8) {
                       // System.out.println("The bot at " + r.location + " needs to be picked up");
                        if (target == null)
                            target = r.location;
                        else
                            target = closestLocation(new MapLocation[]{target, r.location});
                    }
                }
                else if (!outerSpots.isEmpty()) {
                    if (r.location.distanceSquaredTo(HQ) > 13 && r.location.distanceSquaredTo(HQ) != 18) {
                      //  System.out.println("The bot at " + r.location + " needs to be picked up");
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
        if(!cowSearch)
            return null;
       // System.out.println("Checking for cows");
        ArrayList <MapLocation> cow = new ArrayList<>();
        for (RobotInfo r : rc.senseNearbyRobots(-1,Team.NEUTRAL)){
            cow.add(r.location);
        }
        if (cow.isEmpty())
            return null;
        return closestLocation(cow.toArray(new MapLocation[0]));
    }

    public int findSomethingToDo() throws GameActionException {
        if (builderID != -1){
            if(rc.canSenseLocation(builderPickup)) {
                RobotInfo r = rc.senseRobotAtLocation(builderPickup);
                if (r == null || r.ID != builderID)
                    builderID = -1;
                else
                    return pickupUnit(builderPickup);
            }
            else
                pathTo(builderPickup);
        }

        MapLocation target = nearestEnemy();
        if (target == null)
            target = nearestFriendInNeed();
        if (target == null)
            target = findACow();
        if (target != null)
            return pickupUnit(target);
        if (!friends.isEmpty()) {
            HashSet<Integer> toRemove = new HashSet();
            for (int key : friends.keySet()) {
                if (rc.canSenseLocation(friends.get(key)))
                    toRemove.add(key);
            }
            for (int key : toRemove)
                friends.remove(key);
            if (!friends.isEmpty()) {
                pathTo(closestLocation(friends.values().toArray(new MapLocation[0])));
                return 253452345;
            }
        }
        /*if (rc.getLocation().distanceSquaredTo(home) < 10)
            scout();
        else*/
        if (rc.getLocation().equals(home))
            scout();
        else
            pathTo(home);
        return 666;
    }

    public int scout() throws GameActionException{
        for (RobotInfo r: rc.senseNearbyRobots(-1,rc.getTeam().opponent())) {
            if (r.type == RobotType.NET_GUN || r.type == RobotType.HQ){
                path = rc.getLocation().directionTo(r.location).opposite();
                tryMove(path);
                tryMove(path.rotateRight());
                tryMove(path.rotateLeft());
                return 4235;
            }
        }
        if (rc.sensePollution(rc.getLocation()) > 5000){
            Direction d = null;
            for (Direction dir : directions){
                MapLocation m = rc.getLocation().add(dir);
                if (rc.canMove(dir) && (d == null || rc.sensePollution(m) < rc.sensePollution(rc.getLocation().add(d))))
                    d = dir;
            }
        }
        else {
            while (!tryMove(path))
                path = randomDirection();
        }
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