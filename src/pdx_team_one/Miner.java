package pdx_team_one;
import battlecode.common.*;

import java.util.*;

public class Miner extends Robot {

    private static int soup_threshold = RobotType.MINER.soupLimit / 2;
    public static boolean builder = false;
    private static HashSet<MapLocation> blockSoups = new HashSet<>();
    public static ArrayList<MapLocation> refineries = new ArrayList<>();
    private static Direction scoutPath = randomDirection();
    public static Queue<MapLocation> vaporators;
    public static Queue<MapLocation> netGuns;
    public static MapLocation fc = null, ds = null;
    public static MapLocation pickup = null;
    public static boolean attack;
    public static HashSet<MapLocation> wall;
    public boolean buildNetGuns;
    public int turnsStuck = 0;

    Miner(RobotController r) throws GameActionException {
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (rc.getRoundNum() == 3) {
            builder = true;
            vaporators = new ArrayDeque<>();
            netGuns = new ArrayDeque<>();
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
            v = HQ.add(HQ.directionTo(v));
            vaporators.add(v);
            for (Direction dir : Direction.cardinalDirections()) {
                if (!v.equals(HQ.add(dir)))
                    netGuns.add(HQ.add(dir));
            }
            wall = initWallSpots();
        }
        refineries.add(HQ);
    }

    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        MapLocation[] soups = rc.senseNearbySoup();
        if (builder)
            doBuilderThings(soups);
        else
            doMinerThings(soups);
    }

    public int doBuilderThings(MapLocation[] soups) throws GameActionException {
        if (!design_school && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
            System.out.println("let's build a design school");//let's build shit
            buildDesignSchool();
            return 6;
        } else if (!fulfillment_center && design_school && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
            System.out.println("let's build a fulfillment center");//let's build shit
            buildFulfillmentCenter();
            return 4;
        } else if (design_school && !vaporators.isEmpty() && rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
            System.out.println("let's build vaporators");//let's build shit
            buildVaporator();
            return 1;
        } else if (vaporators.isEmpty() && !netGuns.isEmpty() && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
            System.out.println("let's build net guns");//let's build shit
            buildNetGun();
            return 7;
        } else if (buildNetGuns && netGuns.isEmpty() && vaporators.isEmpty()) {
            builder = false;
            return doMinerThings(soups);
        } else if (!design_school && soups.length > 0) {
            //if we can carry more soup, then go get more soup
            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit)
                mineSoup(soups);
                //otherwise either find a refinery or build a refinery
            else
                findRefinery(soups);
            return 2;
        } else if (rc.getSoupCarrying() > 0) {
            findRefinery(soups);
            return 8;
        } else if (rc.getLocation().isAdjacentTo(HQ)) {
            for (Direction dir : directions)
                tryMove(dir);
        } else if (wall.contains(rc.getLocation()))
            builderMove();
        else
            pathTo(HQ);
        return 5;
    }

    public int doMinerThings(MapLocation[] soups) throws GameActionException {
        if (rc.getLocation().distanceSquaredTo(HQ) <= 18 && rc.senseElevation(rc.getLocation()) - hqElevation >= 15) {
            if (pickup == null || pickup != rc.getLocation()) {
                if (askForDrone()) {
                    turnsStuck = 0;
                    pickup = rc.getLocation();
                }
                return 0;
            }
            else if (turnsStuck < 20)
                turnsStuck++;
            else
                rc.disintegrate();
            return 1;
        }
        if (fc != null && fc.equals(rc.getLocation())){
            scout();
            return 55;
        }
        if (rc.getLocation().isAdjacentTo(HQ) && rc.getSoupCarrying() < soup_threshold) {
            if(turnsStuck >= 5) {
                scout();
                turnsStuck = 0;
                return 34;
            }
            else
                turnsStuck++;
        }
        for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (r.type == RobotType.REFINERY && !refineries.contains(r.location))
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
            return 3;
        }
        //if no soups in range
        else {
            //if we're carrying enough soup worth refining
            if (rc.getSoupCarrying() > soup_threshold)
                findRefinery(soups);
                //go explore for soup
            else
                findSoup();
            return 4;
        }
    }

    public boolean askForDrone() throws GameActionException {
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

    public boolean askForDrone(MapLocation m) throws GameActionException {
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

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    public static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    public int parseBlockchain(int round) throws GameActionException {
        int res = 0;
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID) {
                if (t.getMessage()[1] == HQ_LOCATION) {
                    HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    hqID = t.getMessage()[4];
                    hqElevation = t.getMessage()[5];
                    res = 3;
                } else if (t.getMessage()[1] == REFINERY_BUILT) {
                    MapLocation r = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    if (!refineries.contains(r))
                        refineries.add(r);
                    res = 4;
                } else if (t.getMessage()[1] == SOUPS_FOUND) {
                    for (int j = 2; j < 7 && t.getMessage()[j] != 0; j++)
                        blockSoups.add(new MapLocation(t.getMessage()[j] / 100, t.getMessage()[j] % 100));
                    res = 5;
                } else if (t.getMessage()[1] == ATTACK) {
                    attack = true;
                    System.out.println(HQ);
                    fc = HQ.add(Direction.NORTH);
                    ds = HQ.add(Direction.SOUTH);
                } else if (t.getMessage()[1] == DEFENSE) {
                    fc = new MapLocation(t.getMessage()[2],t.getMessage()[3]);
                    ds = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
                } else if (t.getMessage()[1] == START_PHASE_2) {
                    refineries.remove(HQ);
                } else if (t.getMessage()[1] == INNER_SPOTS_FILLED){
                    buildNetGuns = true;
                }
            }
        }
        return res;
    }

    private boolean buildRefinery() throws GameActionException {
        Direction target = Direction.NORTH;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(RobotType.REFINERY, dir)) {
                if (rc.senseElevation(rc.getLocation().add(dir)) > rc.senseElevation(rc.getLocation().add(target)))
                    target = dir;
            }
        }
        if (tryBuild(RobotType.REFINERY, target)) {
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if (r.type == RobotType.REFINERY && !refineries.contains(r.location))
                    refineries.add(r.location);
            }
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = REFINERY_BUILT;
            msg[2] = rc.getLocation().add(target).x;
            msg[3] = rc.getLocation().add(target).y;
            sendMessage(msg, DEFCON4);
            return true;
        }

        return false;
    }

    private void findSoup() throws GameActionException {
        if (blockSoups.isEmpty())
            scout();
        else {
            ArrayList<MapLocation> toRemove = new ArrayList<>();
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

    private void scout() throws GameActionException {
        if (rc.isReady()) {
            while (!tryMove(scoutPath))
                scoutPath = randomDirection();
        }
    }

    private boolean buildDesignSchool() throws GameActionException {
        if (turnsStuck >= 10){
            Direction d = null;
            if (rc.getLocation().distanceSquaredTo(HQ) <= 18) {
                tryMove(rc.getLocation().directionTo(HQ).opposite());
                tryMove(rc.getLocation().directionTo(HQ).opposite().rotateLeft());
                tryMove(rc.getLocation().directionTo(HQ).opposite().rotateRight());
                return false;
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
                design_school = true;
                turnsStuck = 0;
                return true;
            }
        }
        else if (rc.getLocation().equals(ds)) {
            for (Direction dir : directions)
                tryMove(dir);
            turnsStuck++;
        }
        else if (rc.canSenseLocation(ds) && rc.getLocation().isAdjacentTo(ds)) {
            if (tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(ds))) {
                design_school = true;
                turnsStuck = 0;
                return true;
            } else {
                for (Direction dir : directions)
                    tryMove(dir);
                turnsStuck++;
            }
        } else {
            turnsStuck++;
            pathTo(ds);
        }
        return false;

    }

    private boolean buildFulfillmentCenter() throws GameActionException {
        if (turnsStuck >= 10){
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
                fulfillment_center = true;
                turnsStuck = 0;
                return true;
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
                return true;
            } else {
                for (Direction dir : directions)
                    tryMove(dir);
                turnsStuck++;
            }
        } else {
            pathTo(fc);
            turnsStuck++;
        }
        return false;


        /*for (Direction dir : directions) {
            MapLocation fc = rc.getLocation().add(dir);
            if (!rc.onTheMap(fc))
                continue;
            else if (fc.distanceSquaredTo(HQ) <= 8)
                continue;
            else if (fc.isAdjacentTo(ds))
                continue;
            //else if (rc.senseElevation(fc) < 3)
              //  continue;
            if (tryBuild(RobotType.FULFILLMENT_CENTER, dir)) {
                fulfillment_center = true;
                return true;
            }
        }
        Direction dir = rc.getLocation().directionTo(HQ).opposite();
        for (int i = 0; i < 8; i++) {
            tryMove(dir);
            dir = dir.rotateLeft();
        }
        return false;*/
    }

    private boolean buildNetGun() throws GameActionException {
        for (MapLocation m : netGuns) {
            if (!rc.getLocation().equals(m) && rc.getLocation().isAdjacentTo(m)) {
                if (tryBuild(RobotType.NET_GUN, rc.getLocation().directionTo(m))) {
                    netGuns.remove(m);
                    return true;
                }
            }
        }
        builderMove();
        return false;
    }

    private boolean buildVaporator() throws GameActionException {
        MapLocation target = closestLocation(vaporators.toArray(new MapLocation[0]));
        if (rc.getLocation().equals(target)) {
            builderMove();
        } else if (rc.getLocation().isAdjacentTo(target)) {
            if (tryBuild(RobotType.VAPORATOR, rc.getLocation().directionTo(target))) {
                vaporators.remove(target);
                return true;
            }
        } else
            pathTo(target);
        return false;
    }

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
            if (rc.senseElevation(rc.getLocation()) - rc.senseElevation(target) >= 5 || rc.senseElevation(target) - rc.senseElevation(rc.getLocation()) >= 5) {
                if (pickup == null || !pickup.equals(rc.getLocation())) {
                    MapLocation adj = rc.getLocation();
                    for (MapLocation m : wall) {
                        if (elevationDiff(m,target) < elevationDiff(adj,target))
                            adj = m;
                    }
                    if (!adj.equals(rc.getLocation()) && askForDrone(target.add(HQ.directionTo(target)))) {
                        pickup = rc.getLocation();
                        return;
                    }
                }
            }
            return;
        }
        Direction move = Direction.CENTER;
        for (Direction dir : directions) {
            MapLocation m = rc.getLocation().add(dir);
            if (wall.contains(m) && rc.canMove(dir) && m.distanceSquaredTo(target) < rc.getLocation().distanceSquaredTo(target))
                move = dir;
        }
        if (move == Direction.CENTER || !tryMove(move)) {
            if (pickup == null || !pickup.equals(rc.getLocation())) {
                MapLocation adj = null;
                for (MapLocation m : wall) {
                    if (rc.canSenseLocation(m) && m.isAdjacentTo(target))
                        if (adj == null)
                            adj = m;
                        else if(elevationDiff(m,target) < elevationDiff(adj,target))
                            adj = m;
                }
                if (!adj.equals(rc.getLocation()) && askForDrone(adj)) {
                    pickup = rc.getLocation();
                    return;
                }
            }
        }

    }

    private void mineSoup(MapLocation[] soups) throws GameActionException {
        ArrayList<MapLocation> newSoup = new ArrayList<>();
        ArrayList<MapLocation> toRemove = new ArrayList<>();
        for (MapLocation soup : soups) {
            if (rc.senseElevation(soup) - rc.senseElevation(rc.getLocation()) >= 10 || rc.senseElevation(rc.getLocation()) - rc.senseElevation(soup) >= 10)
                blockSoups.remove(soup);
            else if (rc.senseFlooding(soup)){
                boolean remove = true;
                for (Direction dir : directions){
                    if (rc.canSenseLocation(soup.add(dir)) && !rc.senseFlooding(soup.add(dir))){
                        if (!blockSoups.contains(soup))
                            blockSoups.add(soup);
                        remove = false;
                    }
                }
                if (remove)
                    blockSoups.remove(soup);
            }
            else if (!blockSoups.contains(soup)) {
                blockSoups.add(soup);
                newSoup.add(soup);
            }
            if (rc.getLocation().isAdjacentTo(soup))
                tryMine(rc.getLocation().directionTo(soup));
        }
        if (newSoup.size() > 0)
            broadcastSoup(newSoup.toArray(new MapLocation[0]));
        for (MapLocation m : blockSoups){
            if (rc.canSenseLocation(m) && rc.senseSoup(m) == 0)
                toRemove.add(m);
        }
        blockSoups.removeAll(toRemove);
        //if(rc.isReady()) {
            if (!blockSoups.isEmpty())
                pathTo(closestLocation(blockSoups.toArray(new MapLocation[0])));
            else if (rc.isReady())
                scout();
        //}
    }

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
}
