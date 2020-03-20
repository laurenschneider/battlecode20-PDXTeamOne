package pdx_team_one;
import battlecode.common.*;
import java.util.HashSet;

//the miner builds all building (sans HQ), and mines soup
public class Miner extends Unit {

    private int soup_threshold = RobotType.MINER.soupLimit / 2;
    private boolean builder = false;
    private HashSet<MapLocation> blockSoups = new HashSet<>();
    private HashSet<MapLocation> refineries = new HashSet<>();
    private Direction scoutPath = randomDirection();
    private HashSet<MapLocation> vaporators;
    private HashSet<MapLocation> netGuns;
    private MapLocation fc = null, ds = null;
    private MapLocation pickup = null;
    private int turnsStuck = 0;
    private int phase = 0;
    private int LS;
    private HashSet<MapLocation> wall;
    private HashSet<MapLocation> innerSpots;
    private HashSet<MapLocation> outerSpots;
    private boolean design_school, fulfillment_center, buildNetGuns;

    //initialize various map locations
    Miner(RobotController r) throws GameActionException {
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        innerSpots = initInnerSpots();
        //only 1 miner gets to be the builder, he has a lot more initialization to do for vaporators and net guns
        if (rc.getRoundNum() == 3) {
            builder = true;
            vaporators = new HashSet<>();
            netGuns = new HashSet<>();
            if(!constriction) {
                if (upper)
                    vaporators.add(HQ.add(Direction.NORTH));
                if (right)
                    vaporators.add(HQ.add(Direction.EAST));
                if (left)
                    vaporators.add(HQ.add(Direction.WEST));
                if (lower)
                    vaporators.add(HQ.add(Direction.SOUTH));
            }
            if(vaporators.isEmpty()) {
                MapLocation[] edges = new MapLocation[4];
                edges[0] = new MapLocation(0, HQ.y);
                edges[1] = new MapLocation(rc.getMapWidth() - 1, HQ.y);
                edges[2] = new MapLocation(HQ.x, 0);
                edges[3] = new MapLocation(HQ.x, rc.getMapHeight() - 1);
                MapLocation v = edges[0];
                for (MapLocation edge : edges) {
                    if (HQ.distanceSquaredTo(edge) < HQ.distanceSquaredTo(v))
                        v = edge;
                }
                MapLocation m = null;
                for (Direction dir : Direction.cardinalDirections()) {
                    if (rc.onTheMap(HQ.add(dir)))
                        if (m == null)
                            m = HQ.add(dir);
                        else if (HQ.add(dir).distanceSquaredTo(v) < m.distanceSquaredTo(v))
                            m = HQ.add(dir);
                }
                vaporators.add(m);
            }

            for (Direction dir : Direction.cardinalDirections()) {
                if (rc.onTheMap(HQ.add(dir))&& !vaporators.contains(HQ.add(dir)))
                    netGuns.add(HQ.add(dir));
            }

            if(!constriction) {
                if (left) {
                    if (!lower && rc.onTheMap(HQ.translate(-2, -1)))
                        netGuns.add(HQ.translate(-2, -1));
                    if (!upper && rc.onTheMap(HQ.translate(-2, 1)))
                        netGuns.add(HQ.translate(-2, 1));
                }
                if (right) {
                    if (!lower && rc.onTheMap(HQ.translate(2, -1)))
                        netGuns.add(HQ.translate(2, -1));
                    if (!upper && rc.onTheMap(HQ.translate(2, 1)))
                        netGuns.add(HQ.translate(2, 1));
                }
                if (upper) {
                    if (!right && rc.onTheMap(HQ.translate(1, 2)))
                        netGuns.add(HQ.translate(1, 2));
                    if (!left && rc.onTheMap(HQ.translate(-1, 2)))
                        netGuns.add(HQ.translate(-1, 2));
                }
                if (lower) {
                    if (!right && rc.onTheMap(HQ.translate(1, -2)))
                        netGuns.add(HQ.translate(1, -2));
                    if (!left && rc.onTheMap(HQ.translate(-1, -2)))
                        netGuns.add(HQ.translate(-1, -2));
                }

                if (HQ.x <= 1)
                    vaporators.remove(HQ.add(Direction.WEST));
                if (rc.getMapWidth() - HQ.x <= 2)
                    vaporators.remove(HQ.add(Direction.EAST));
                if (HQ.y <= 1)
                    vaporators.remove(HQ.add(Direction.SOUTH));
                if (rc.getMapHeight() - HQ.y <= 2)
                    vaporators.remove(HQ.add(Direction.NORTH));
            }
            LS = initInnerSpots().size();
        }
        wall = initWallSpots();
        outerSpots = initOuterSpots();
        refineries.add(HQ);
    }

    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        MapLocation[] soups = rc.senseNearbySoup();
        //if you're in the inner spots, get out of the way for the landscapers
        if (rc.getRoundNum() > 100 && innerSpots.contains(rc.getLocation()) && rc.getSoupCarrying() < soup_threshold) {
            if(turnsStuck >= 5) {
                scout();
                turnsStuck = 0;
                return;
            }
            else
                turnsStuck++;
        }
        if (builder)
            doBuilderThings(soups);
        else
            doMinerThings(soups);
    }

    //the builder builds things
    private void doBuilderThings(MapLocation[] soups) throws GameActionException {
        checkPhase();

        //the builder builds things based on a few conditional checks
        if (!design_school && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost)
            buildDesignSchool();
        else if (!fulfillment_center && design_school && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost)
            buildFulfillmentCenter();
        else if (design_school && !vaporators.isEmpty() && rc.getTeamSoup() >= RobotType.VAPORATOR.cost)
            buildVaporator();
        else if (buildNetGuns && vaporators.isEmpty() && !netGuns.isEmpty() && rc.getTeamSoup() >= RobotType.NET_GUN.cost)
            buildNetGun();
        else if (netGuns.isEmpty() && vaporators.isEmpty()) {
            //if there's nothing left to build, then he becomes a miner
            builder = false;
            doMinerThings(soups);
        } else if (!design_school && soups.length > 0) {
            //if we can carry more soup, then go get more soup
            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit)
                mineSoup(soups);
                //otherwise either find a refinery or build a refinery
            else
                findRefinery(soups);
        } else if (rc.getSoupCarrying() > 0)
            findRefinery(soups);
        else
            builderMove();
    }

    //the miners go and find soup
    private void doMinerThings(MapLocation[] soups) throws GameActionException {
        //if they're stuck in the wall and it's being built up, call a drone for help
        if ((innerSpots.contains(rc.getLocation()) || wall.contains(rc.getLocation()) || outerSpots.contains(rc.getLocation()) || rc.getLocation().isAdjacentTo(HQ)) && elevationDiff(rc.getLocation(),HQ) >= 10) {
            if (pickup == null || pickup != rc.getLocation()) {
                turnsStuck = 0;
                if (askForDrone()) {
                    pickup = rc.getLocation();
                }
                return;
            }
            //if it's been too long tho, just self-destruct
            else if (turnsStuck < 20)
                turnsStuck++;
            else
                rc.disintegrate();
            return;
        }
        // don't get in the way of the fulfillment center
        if (fc != null && fc.equals(rc.getLocation())){
            scout();
            return;
        }
        //add refineries to the log
        for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (r.type == RobotType.REFINERY)
                refineries.add(r.location);
        }
        //if soups in range
        if (soups.length > 0) {
            MapLocation m = closestLocation(soups);
            if (m.isAdjacentTo(rc.getLocation()) && (refineries.isEmpty() || rc.getLocation().distanceSquaredTo(closestLocation(refineries.toArray(new MapLocation[0]))) > 100))
                buildRefinery();
            //if we can carry more soup, then go get more soup
            else if (rc.getSoupCarrying() < RobotType.MINER.soupLimit)
                mineSoup(soups);
                //otherwise either find a refinery or build a refinery
            else
                findRefinery(soups);
        }
        //if no soups in range
        else {
            //if we're carrying enough soup worth refining
            if (rc.getSoupCarrying() > soup_threshold)
                findRefinery(soups);
                //go explore for soup
            else
                findSoup();
        }
    }

    //calls a drone for help
    private boolean askForDrone() throws GameActionException {
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = NEED_DELIVERY;
        msg[2] = -1;
        msg[3] = -1;
        msg[4] = rc.getID();
        msg[5] = rc.getLocation().x;
        msg[6] = rc.getLocation().y;
        return sendMessage(msg, DEFCON5);
    }

    //calls a drone for help to a specific location
    private boolean askForDrone(MapLocation m) throws GameActionException {
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = NEED_DELIVERY;
        msg[2] = m.x;
        msg[3] = m.y;
        msg[4] = rc.getID();
        msg[5] = rc.getLocation().x;
        msg[6] = rc.getLocation().y;
        return sendMessage(msg, DEFCON5);
    }

    //refines soup
    private void tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
        }
    }

    //mines soup
    private void tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
        }
    }

    //gets the latest hot goss
    private void parseBlockchain(int round) throws GameActionException {
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID) {
                switch (t.getMessage()[1]) {
                    case HQ_LOCATION:
                        HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        hqElevation = t.getMessage()[5];
                        break;
                    case REFINERY_BUILT:
                        MapLocation r = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        refineries.add(r);
                        break;
                    case SOUPS_FOUND:
                        for (int j = 2; j < 7 && t.getMessage()[j] != 0; j++)
                            blockSoups.add(new MapLocation(t.getMessage()[j] / 100, t.getMessage()[j] % 100));
                        break;
                    case DEFENSE:
                        fc = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        ds = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
                        break;
                    case START_PHASE_2:
                        refineries.remove(HQ);
                        break;
                    case INNER_SPOTS_FILLED:
                        buildNetGuns = true;
                        break;
                }
            }
        }
    }

    //builds a refinery on the highest elevation adjacent to the miner
    private void buildRefinery() throws GameActionException {
        Direction target = Direction.NORTH;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(RobotType.REFINERY, dir)) {
                if (rc.senseElevation(rc.getLocation().add(dir)) > rc.senseElevation(rc.getLocation().add(target)))
                    target = dir;
            }
        }
        if (tryBuild(RobotType.REFINERY, target)) {
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if (r.type == RobotType.REFINERY)
                    refineries.add(r.location);
            }
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = REFINERY_BUILT;
            msg[2] = rc.getLocation().add(target).x;
            msg[3] = rc.getLocation().add(target).y;
            sendMessage(msg, DEFCON4);
        }

    }

    //looks for soup by checking the list of soups from the blockchain, or wandering if none exist
    private void findSoup() throws GameActionException {
        if (blockSoups.isEmpty())
            scout();
        else {
            HashSet<MapLocation> toRemove = new HashSet<>();
            for (MapLocation m : blockSoups) {
                if (rc.canSenseLocation(m))
                    toRemove.add(m);
            }
            blockSoups.removeAll(toRemove);
            if (blockSoups.isEmpty())
                scout();
            else
                pathTo(closestLocation(blockSoups.toArray(new MapLocation[0])));
        }
    }

    //walks in a random direction
    private void scout() throws GameActionException {
        if (rc.isReady()) {
            while (Clock.getBytecodesLeft() > 500 && !tryMove(scoutPath))
                scoutPath = randomDirection();
        }
    }

    //builds a design school
    private void buildDesignSchool() throws GameActionException {
        //if we haven't reached the designated location in 15 turns, then just find a spot
        if (turnsStuck >= 15){
            Direction d = null;
            if (rc.getLocation().distanceSquaredTo(HQ) <= 18) {
                tryMove(rc.getLocation().directionTo(HQ).opposite());
                tryMove(rc.getLocation().directionTo(HQ).opposite().rotateLeft());
                tryMove(rc.getLocation().directionTo(HQ).opposite().rotateRight());
                return;
            }
            for (Direction dir : directions){
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL,dir)){
                    if (d == null || rc.senseElevation(rc.getLocation().add(dir)) > rc.senseElevation(rc.getLocation().add(d)))
                        d = dir;
                }
            }
            if (d== null)
                scout();
            else if (tryBuild(RobotType.DESIGN_SCHOOL,d)){
                ds = rc.getLocation().add(d);
                int []msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = DEFENSE;
                msg[2] = fc.x;
                msg[3] = fc.y;
                msg[4] = ds.x;
                msg[5] = ds.y;
                sendMessage(msg, DEFCON5);
                design_school = true;
                turnsStuck = 0;
            }
        }
        //otherwise get out of the way if we're in the spot we're supposed to build on, or move to it, or build on it
        else if (rc.getLocation().equals(ds)) {
            for (Direction dir : directions)
                tryMove(dir);
            turnsStuck++;
        }
        else if (rc.canSenseLocation(ds) && rc.getLocation().isAdjacentTo(ds)) {
            if (tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(ds))) {
                design_school = true;
                turnsStuck = 0;
            } else {
                for (Direction dir : directions)
                    tryMove(dir);
                turnsStuck++;
            }
        } else {
            turnsStuck++;
            pathTo(ds);
        }

    }

    //copy and paste the comments for the above design school code, but apply it to the fulfillment center instead
    private void buildFulfillmentCenter() throws GameActionException {
        if (turnsStuck >= 15){
            Direction d = null;
            for (Direction dir : directions){
                if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER,dir)){
                    if (d == null || rc.senseElevation(rc.getLocation().add(dir)) > rc.senseElevation(rc.getLocation().add(d)))
                        d = dir;
                }
            }
            if (d== null)
                scout();
            else if (tryBuild(RobotType.FULFILLMENT_CENTER,d)){
                fc = rc.getLocation().add(d);
                int []msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = DEFENSE;
                msg[2] = fc.x;
                msg[3] = fc.y;
                msg[4] = ds.x;
                msg[5] = ds.y;
                sendMessage(msg, DEFCON5);
                fulfillment_center = true;
                turnsStuck = 0;
            }
        }
        else if (rc.getLocation().equals(fc)) {
            for (Direction dir : directions)
                tryMove(dir);
            turnsStuck++;
        }
        else if (rc.canSenseLocation(fc) && rc.getLocation().isAdjacentTo(fc)) {
            if (tryBuild(RobotType.FULFILLMENT_CENTER, rc.getLocation().directionTo(fc))) {
                fulfillment_center = true;
                turnsStuck = 0;
            } else {
                for (Direction dir : directions)
                    tryMove(dir);
                turnsStuck++;
            }
        } else {
            pathTo(fc);
            turnsStuck++;
        }
    }

    //build the closest net gun from our net gun list
    private void buildNetGun() throws GameActionException {
        for (MapLocation m : netGuns) {
            if (!rc.getLocation().equals(m) && rc.getLocation().isAdjacentTo(m)) {
                if (tryBuild(RobotType.NET_GUN, rc.getLocation().directionTo(m))) {
                    netGuns.remove(m);
                    return;
                }
            }
        }
        builderMove();
    }

    //build the closest vaporator from our vaporator list
    private void buildVaporator() throws GameActionException {
        MapLocation target = closestLocation(vaporators.toArray(new MapLocation[0]));
        if (rc.getLocation().equals(target)) {
            for (Direction dir : directions)
                tryMove(dir);
        } else if (rc.getLocation().isAdjacentTo(target)) {
            if (tryBuild(RobotType.VAPORATOR, rc.getLocation().directionTo(target))) {
                vaporators.remove(target);
            }
        } else
            pathTo(target);
    }

    //since the builder will be moving in and out of where the landscapers are building, it's easier to just have a
    //drone pick them up and carry them to where they need to go. this code determines if they need that help
    //and then calls for it
    private void builderMove() throws GameActionException {
        MapLocation target = rc.getLocation();
        if (!vaporators.isEmpty())
            target = closestLocation(vaporators.toArray(new MapLocation[0]));
        else if (!netGuns.isEmpty())
            target = closestLocation(netGuns.toArray(new MapLocation[0]));
        if (rc.getLocation().equals(target)) {
            for (Direction dir : directions) {
                MapLocation m = rc.getLocation().add(dir);
                if (wall.contains(m) && rc.canMove(dir))
                    if(tryMove(dir))
                        return;
            }
        } else if (rc.getLocation().isAdjacentTo(target)) {
            if (elevationDiff(target,rc.getLocation()) >= 5) {
                if (pickup == null || !pickup.equals(rc.getLocation())) {
                    MapLocation adj = rc.getLocation();
                    for (Direction dir : directions) {
                        MapLocation m = target.add(dir);
                        if (!innerSpots.contains(m) && rc.senseRobotAtLocation(m) == null && elevationDiff(m, target) < elevationDiff(adj, target))
                            adj = m;
                    }
                   // //System.out.println(adj + " is the best spot for me to build from");
                    if (!adj.equals(rc.getLocation()) && askForDrone(adj)) {
                        pickup = rc.getLocation();
                        return;
                    }
                }
            }
            return;
        }
        for (Direction dir : directions) {
            MapLocation m = rc.getLocation().add(dir);
            if (m.isAdjacentTo(target) && rc.canMove(rc.getLocation().directionTo(target)))
                tryMove(rc.getLocation().directionTo(target));
        }
        if (rc.isReady()){
            if (pickup == null || !pickup.equals(rc.getLocation())) {
                MapLocation adj = null;
                for (Direction dir : directions) {
                    MapLocation m = target.add(dir);
                    if (rc.canSenseLocation(m) && m.isAdjacentTo(target) && rc.senseRobotAtLocation(m) == null && !innerSpots.contains(m)) {
                        if (adj == null)
                            adj = m;
                        else if (elevationDiff(m, target) < elevationDiff(adj, target))
                            adj = m;
                    }
                }
                if (adj != null &&!adj.equals(rc.getLocation()) && askForDrone(adj))
                    pickup = rc.getLocation();
            }
        }
    }

    //either heads to the closest soup or mines an adjacent soup
    private void mineSoup(MapLocation[] soups) throws GameActionException {
        if (!rc.isReady())
            return;
        HashSet<MapLocation> newSoup = new HashSet<>();
        HashSet<MapLocation> toRemove = new HashSet<>();
        for (MapLocation soup : soups) {
            if (elevationDiff(rc.getLocation(), soup) >= 10)
                blockSoups.remove(soup);
            else if (rc.senseFlooding(soup)) {
                boolean remove = true;
                for (Direction dir : directions) {
                    if (rc.canSenseLocation(soup.add(dir)) && !rc.senseFlooding(soup.add(dir))) {
                        blockSoups.add(soup);
                        remove = false;
                    }
                }
                if (remove)
                    blockSoups.remove(soup);
            } else if (!blockSoups.contains(soup)) {
                blockSoups.add(soup);
                newSoup.add(soup);
            }
            if (rc.getLocation().isAdjacentTo(soup))
                tryMine(rc.getLocation().directionTo(soup));
        }
        if (newSoup.size() > 0)
            broadcastSoup(newSoup.toArray(new MapLocation[0]));
        for (MapLocation m : blockSoups) {
            if (rc.canSenseLocation(m) && rc.senseSoup(m) == 0)
                toRemove.add(m);
        }
        blockSoups.removeAll(toRemove);
        if (!blockSoups.isEmpty())
            pathTo(closestLocation(blockSoups.toArray(new MapLocation[0])));
        else if (rc.isReady())
            scout();
    }

    //finds the nearest refinery or builds one if there isn't one nearby
    private void findRefinery(MapLocation[] soups) throws GameActionException {
        if (refineries.isEmpty()) {
            if (rc.senseNearbySoup().length > 0) {
                if (rc.getTeamSoup() >= RobotType.REFINERY.cost)
                    buildRefinery();
            } else
                findSoup();
        } else {
            MapLocation target = closestLocation(refineries.toArray(new MapLocation[0]));
            if (rc.canSenseLocation(target)){
                RobotInfo r = rc.senseRobotAtLocation(target);
                if (r == null || (r.type != RobotType.REFINERY && r.type != RobotType.HQ)){
                    refineries.remove(target);
                    findRefinery(soups);
                    return;
                }
            }
            if (rc.canSenseLocation(target) && rc.getLocation().isAdjacentTo(target))
                tryRefine(rc.getLocation().directionTo(target));
            else if (rc.getLocation().distanceSquaredTo(target) < 50)
                pathTo(target);
            else if (soups.length > 0 && rc.getSoupCarrying() == RobotType.MINER.soupLimit && rc.getTeamSoup() >= RobotType.REFINERY.cost)
                buildRefinery();
            else if (rc.getSoupCarrying() < RobotType.MINER.soupLimit)
                findSoup();
        }
    }

    //determines what phase it is and sends a message if there's a change
    private void checkPhase() throws GameActionException{
        if(phase == 0) {
            if (vaporators.isEmpty()) {
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = VAPORATOR_BUILT;
                msg[2] = LS;
                if (sendMessage(msg, DEFCON5))
                    phase++;
            }
        }
        else if (phase == 1){
            if(netGuns.isEmpty()) {
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = START_PHASE_2;
                if (sendMessage(msg, DEFCON5))
                    phase++;
            }
        }
    }
}
