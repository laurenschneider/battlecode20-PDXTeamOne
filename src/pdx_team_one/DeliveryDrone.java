package pdx_team_one;
import battlecode.common.*;
import java.util.HashMap;
import java.util.HashSet;


//Delivery drones pickup units and deliver them to locations
public class DeliveryDrone extends Unit{

    private HashMap<Integer, MapLocation> friends = new HashMap<>();
    private HashSet<MapLocation> water = new HashSet<>();
    private RobotInfo holding;
    private static Direction path = Direction.NORTH;
    private boolean ds_secure = false;
    private MapLocation home = null;
    private boolean innerSpotsFilled;
    private boolean wallSpotsFilled;
    private HashSet<MapLocation> innerSpots = new HashSet<>();
    private HashSet<MapLocation> initialInnerSpots;
    private HashSet<MapLocation> wallSpots = new HashSet<>();
    private HashSet<MapLocation> initialWallSpots;
    private HashSet<MapLocation> outerSpots;
    private HashSet<MapLocation> initialOuterSpots = new HashSet<>();
    private HashSet<MapLocation> blockSoups = new HashSet<>();
    private MapLocation builderDropoff = null;
    private MapLocation ds = null, fc = null;
    private int builderID = -1;
    private MapLocation builderPickup = null;
    private boolean cowSearch;
    private int turnsHeld;
    private int turnsStuck = 0;

    //initializes basic map data and updated game state information
    DeliveryDrone(RobotController r) throws GameActionException {
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (!innerSpotsFilled)
            innerSpots = initInnerSpots();
        if (!wallSpotsFilled)
            wallSpots = initWallSpots();
        initialInnerSpots = initInnerSpots();
        initialWallSpots = initWallSpots();
        outerSpots = initOuterSpots();
        outerSpots.remove(fc);
        outerSpots.remove(ds);
        initialOuterSpots.addAll(outerSpots);
        if (home == null)
            home = rc.getLocation();
        if(constriction){
            HashSet<MapLocation> toRemove = new HashSet<>();
            for (MapLocation m : outerSpots){
                if (m.x == 0)
                    toRemove.add(m);
                else if (m.x == rc.getMapWidth() - 1 )
                    toRemove.add(m);
                else if (m.y == 0)
                    toRemove.add(m);
                else if (m.y == rc.getMapHeight() - 1 )
                    toRemove.add(m);
            }
            outerSpots.removeAll(toRemove);
            initialOuterSpots.removeAll(toRemove);
        }
    }

    public void takeTurn() throws GameActionException {
        //get the latest blockChain info
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        //home is where the drones hang out when not doing anything. That initializes to being near the Design School
        //but if the Design School is destroyed, then we switch to HQ
        if(rc.canSenseLocation(ds) && (rc.senseRobotAtLocation(ds) == null || rc.senseRobotAtLocation(ds).type != RobotType.DESIGN_SCHOOL))
            home = HQ;
        //if we're impeding the wall progress, we need to either get out of the way or self-destruct
        if (!home.equals(HQ) && (innerSpots.contains(rc.getLocation()) || wallSpots.contains(rc.getLocation()) || wallSpots.contains(rc.getLocation()))){
            turnsStuck++;
            if (turnsStuck >= 25)
                scout();
            if (turnsStuck >= 50)
                rc.disintegrate();
        }
        else
            turnsStuck = 0;
        //decide what to do based on if we're holding someone or not
        if (rc.isReady()) {
            if (rc.isCurrentlyHoldingUnit()) {
                if (holding.team == rc.getTeam())
                    deliverFriend();
                else
                    destroyEnemy();
            } else {
                turnsHeld = 0;
                findSomethingToDo();
            }
        }
        if (Clock.getBytecodesLeft() > 2000)
            scan();
        if (rc.isReady())
            scout();
    }

    //this function tells the drone to deliver a Landscaper to one of the locations in spots
    private boolean deliverLS(HashSet<MapLocation> spots) throws GameActionException{
        HashSet<MapLocation> toRemove = new HashSet<>();
        MapLocation target = null;
        for (MapLocation m : spots) {
            if (rc.canSenseLocation(m)) {
                //if there's a landscaper already in m and/or it's flooded beyond repair, remove it from the list
                RobotInfo r = rc.senseRobotAtLocation(m);
                if (r != null && r.type == RobotType.LANDSCAPER)
                    toRemove.add(m);
                else if (rc.senseFlooding(m) && rc.senseElevation(m) < -1000)
                    toRemove.add(m);
                //otherwise, pick the closest spot
                else if (!rc.senseFlooding(m)) {
                    if (target == null)
                        target = m;
                    else if (rc.senseElevation(m) < rc.senseElevation(target))
                        target = m;
                }
            }
        }
        spots.removeAll(toRemove);
        //if we have a valid target, either drop our landscaper off or move towards it
        if (target != null){
            if (rc.getLocation().equals(target)){
                for (Direction dir : directions)
                    tryMove(dir);
            }
            else if (rc.getLocation().isAdjacentTo(target) && rc.canDropUnit(rc.getLocation().directionTo(target))) {
                rc.dropUnit(rc.getLocation().directionTo(target));
                spots.remove(target);
                holding = null;
                return true;
            }
            else
                pathTo(target);
        }
        //or if there are still spots available, then head towards the closest one
        else if (!spots.isEmpty()) {
            target = closestLocation(spots.toArray(new MapLocation[0]));
            if (rc.getLocation().isAdjacentTo(target)){
                for (Direction dir : directions)
                    tryMove(dir);
            }
            pathTo(target);
            return true;
        }
        //return whether or not there are available spots
        return (!spots.isEmpty());
    }

    //this block tells the drone how to deliver a friend
    private void deliverFriend() throws GameActionException {
        //if we've held a friend for too long, just drop him
        if (holding.ID != builderID)
            turnsHeld++;
        if (turnsHeld >= 50) {
            for (Direction dir : directions) {
                if (rc.canDropUnit(dir)) {
                    rc.dropUnit(dir);
                    turnsHeld = 0;
                }
            }
        }
        //if there's too much pollution, the senseLocation() function won't work so we need to get out of there
        if (rc.sensePollution(rc.getLocation()) > 5000)
            scout();
        //the builder tells the drone where he needs to go, so the drone goes there
        else if (holding.ID == builderID && builderDropoff != null) {
            if (rc.getLocation().equals(builderDropoff)) {
                for (Direction dir : directions)
                    tryMove(dir);
            } else if (rc.getLocation().isAdjacentTo(builderDropoff)) {
                if (rc.canDropUnit(rc.getLocation().directionTo(builderDropoff))) {
                    rc.dropUnit(rc.getLocation().directionTo(builderDropoff));
                    builderID = -1;
                    builderDropoff = null;
                } else if (rc.senseRobotAtLocation(builderDropoff) != null) {
                    for (Direction dir : directions) {
                        if (rc.canDropUnit(dir) && rc.getLocation().add(dir).isAdjacentTo(builderDropoff)) {
                            rc.dropUnit(dir);
                            builderID = -1;
                            builderDropoff = null;
                            return;
                        }
                        return;
                    }
                }
            } else if (builderDropoff.equals(ds)) {
                if (rc.getLocation().add(rc.getLocation().directionTo(ds)).isAdjacentTo(ds)) {
                    for (Direction dir : directions) {
                        if (rc.onTheMap(rc.getLocation().add(dir)) && rc.getLocation().add(dir).isAdjacentTo(ds) && rc.canDropUnit(dir) && elevationDiff(rc.getLocation().add(dir),ds) <= 3) {
                            rc.dropUnit(dir);
                            builderID = -1;
                            builderDropoff = null;
                            return;
                        }
                    }
                }
                pathTo(ds);
            } else {
                pathTo(builderDropoff);
            }
        //landscapers go to landscaper spots
        } else if (holding.type == RobotType.LANDSCAPER) {
            if (!innerSpots.isEmpty() && deliverLS(innerSpots))
                return;
            if (!wallSpots.isEmpty() && deliverLS(wallSpots))
                return;
            if (!outerSpots.isEmpty())
                deliverLS(outerSpots);
        //and other miners just go to soup
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
            if (target == null) {
                if (blockSoups.isEmpty())
                    scout();
                else {
                    target = closestLocation(blockSoups.toArray(new MapLocation[0]));
                    if (rc.canSenseLocation(target)){
                        blockSoups.remove(target);
                        if (blockSoups.isEmpty()){
                            scout();
                            return;
                        }
                    }
                    pathTo(closestLocation(blockSoups.toArray(new MapLocation[0])));
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
            else
                pathTo(target);
        }
    }

    //gets all the latest hot goss
    private void parseBlockchain(int i) throws GameActionException {
        for (Transaction t : rc.getBlock(i)) {
            if (t.getMessage()[0] == TEAM_ID) {
                switch (t.getMessage()[1]) {
                    case HQ_LOCATION:
                        HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        hqElevation = t.getMessage()[5];
                        break;
                    case NEED_DELIVERY:
                        if (t.getMessage()[2] != -1) {
                            builderID = t.getMessage()[4];
                            builderPickup = new MapLocation(t.getMessage()[5], t.getMessage()[6]);
                            builderDropoff = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        } else
                            friends.put(t.getMessage()[4], new MapLocation(t.getMessage()[5], t.getMessage()[6]));
                        break;
                    case DS_SECURE:
                        ds_secure = true;
                        break;
                    case DEFENSE:
                        ds = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
                        fc = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        for (Direction dir : directions) {
                            if (rc.onTheMap(ds.add(dir).add(dir))) {
                                home = ds.add(dir).add(dir);
                                break;
                            }
                        }
                        break;
                    case SOUPS_FOUND:
                        for (int j = 2; j < 7 && t.getMessage()[j] != 0; j++)
                            blockSoups.add(new MapLocation(t.getMessage()[j] / 100, t.getMessage()[j] % 100));
                        break;
                    case INNER_SPOTS_FILLED:
                        innerSpotsFilled = true;
                        innerSpots.clear();
                        cowSearch = true;
                        break;
                    case WALL_SPOTS_FILLED:
                        wallSpotsFilled = true;
                        wallSpots.clear();
                        break;
                    case DRONE_HOME:
                        home = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        break;
                }
            }
        }
    }

    //either picks up the unit at ml or moves toward it
    private void pickupUnit(MapLocation ml)throws GameActionException {
        if (rc.canSenseLocation(ml)) {
            RobotInfo r = rc.senseRobotAtLocation(ml);
            if (r != null && rc.getLocation().isAdjacentTo(ml) && rc.canPickUpUnit(r.ID)) {
                holding = r;
                rc.pickUpUnit(r.ID);
                friends.remove(r.ID);
                return;
            }
        }
        pathTo(ml);
    }

    //finds the nearest enemy
    private MapLocation nearestEnemy(){
        //but only after we've filled our inner spots with landscapers
        if(!innerSpotsFilled)
            return null;
        MapLocation target = null;
        for (RobotInfo r : rc.senseNearbyRobots(-1,rc.getTeam().opponent())) {
            if (r.type == RobotType.LANDSCAPER || r.type == RobotType.MINER){
                if (target == null)
                    target = r.location;
                else if (HQ.distanceSquaredTo(r.location) < HQ.distanceSquaredTo(target))
                    target = r.location;
            }
        }
        return target;
    }

    //find the nearest landscaper/miner that needs to move
    private MapLocation nearestFriendInNeed(){
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
                    if (!initialInnerSpots.contains(r.location)) {
                        if (target == null)
                            target = r.location;
                        else
                            target = closestLocation(new MapLocation[]{target, r.location});
                    }
                }
                else if (!wallSpots.isEmpty()) {
                    if (!initialInnerSpots.contains(r.location) && !initialWallSpots.contains(r.location)) {
                        if (target == null)
                            target = r.location;
                        else
                            target = closestLocation(new MapLocation[]{target, r.location});
                    }
                }
                else if (!outerSpots.isEmpty()) {
                    if (!initialInnerSpots.contains(r.location) && !initialWallSpots.contains(r.location) && !initialOuterSpots.contains(r.location)) {
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

    //findACow finds a cow
    public MapLocation findACow(){
        //but only if we've taken care of other things first
        if(!cowSearch)
            return null;
        HashSet <MapLocation> cow = new HashSet<>();
        for (RobotInfo r : rc.senseNearbyRobots(-1,Team.NEUTRAL))
            cow.add(r.location);
        if (cow.isEmpty())
            return null;
        return closestLocation(cow.toArray(new MapLocation[0]));
    }

    //if we're not holding anybody
    private void findSomethingToDo() throws GameActionException {
        //see if the builder needs help
        if (builderID != -1){
            if(rc.canSenseLocation(builderPickup)) {
                RobotInfo r = rc.senseRobotAtLocation(builderPickup);
                if (r == null || r.ID != builderID)
                    builderID = -1;
                else
                    pickupUnit(builderPickup);
            }
            else
                pathTo(builderPickup);
        }

        //try to find a friend, then enemy, then cow
        MapLocation target = nearestFriendInNeed();
        if (target == null)
            target = nearestEnemy();
        if (target == null)
            target = findACow();
        if (target != null) {
            pickupUnit(target);
            return;
        }
        //if nothing, check the friends log from the blockchain and move to the closest one if available
        if (!friends.isEmpty()) {
            HashSet<Integer> toRemove = new HashSet<>();
            for (int key : friends.keySet()) {
                if (rc.canSenseLocation(friends.get(key)))
                    toRemove.add(key);
            }
            for (int key : toRemove)
                friends.remove(key);
            if (!friends.isEmpty()) {
                pathTo(closestLocation(friends.values().toArray(new MapLocation[0])));
                return;
            }
        }
        //we don't want the drones to just sit in one spot in case they're blocking anything
        if (rc.getLocation().equals(home))
            scout();
        //but otherwise head home
        else
            pathTo(home);
    }

    //drones move in a pre-determined direction
    private void scout() throws GameActionException{
        //if the drone senses enemy netgun/HQ, then move out of the way
        for (RobotInfo r: rc.senseNearbyRobots(-1,rc.getTeam().opponent())) {
            if (r.type == RobotType.NET_GUN || r.type == RobotType.HQ){
                path = rc.getLocation().directionTo(r.location).opposite();
                tryMove(path);
                tryMove(path.rotateRight());
                tryMove(path.rotateLeft());
                return;
            }
        }
        //if there's too much pollution, we need to get out of there too
        if (rc.sensePollution(rc.getLocation()) > 5000){
            Direction d = null;
            for (Direction dir : directions){
                MapLocation m = rc.getLocation().add(dir);
                if (rc.canMove(dir) && (d == null || rc.sensePollution(m) < rc.sensePollution(rc.getLocation().add(d))))
                    d = dir;
            }
            tryMove(d);
        }
        //if all else fails, move in a random direction
        else {
            while (rc.isReady() && !tryMove(path))
                path = randomDirection();
        }
    }

    //if we're holding an enemy, find the nearest source of water and drop him off in it
    private void destroyEnemy() throws GameActionException{
        MapLocation m;
        for (Direction dir : directions) {
            m = rc.getLocation().add(dir);
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                if (rc.canDropUnit(dir)) {
                    rc.dropUnit(dir);
                    holding = null;
                    return;
                }
            }
        }
        for (Direction dir : directions) {
            m = rc.getLocation().add(dir).add(dir).add(dir);
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(rc.getLocation().add(dir).add(dir));
                return;
            }
            m = rc.getLocation().add(dir).add(dir).add(dir.rotateRight());
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(m);
                return;
            }
        }
        for (Direction dir : directions) {
            m = rc.getLocation().add(dir).add(dir).add(dir);
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(m);
                return;
            }
            m = rc.getLocation().add(dir).add(dir).add(dir.rotateLeft());
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(m);
                return;
            }
            m = rc.getLocation().add(dir).add(dir).add(dir.rotateRight());
            if (rc.canSenseLocation(m) && rc.senseFlooding(m)) {
                water.add(m);
                pathTo(m);
                return;
            }
        }
        if (water.isEmpty())
            scout();
        else
            pathTo(closestLocation(water.toArray(new MapLocation[0])));
    }

    //update maps by checking local surroundings
    private void scan() throws GameActionException{
        MapLocation current = rc.getLocation();
        for (Direction dir : directions){
            MapLocation check = current.add(dir);
            if (rc.onTheMap(check)){
                RobotInfo r  = rc.senseRobotAtLocation(check);
                if (rc.senseFlooding(check))
                    water.add(check);
                else if (r == null && initialInnerSpots.contains(check))
                    innerSpots.add(check);
                else if (r == null && initialWallSpots.contains(check))
                    wallSpots.add(check);
                else if (r == null && initialOuterSpots.contains(check))
                    outerSpots.add(check);
            }
        }
    }

}