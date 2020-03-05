package pdx_team_one;
import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;

public class Miner extends Robot{

    private static int soup_threshold =  RobotType.MINER.soupLimit/2;
    public static boolean builder = false;
    private static ArrayList<MapLocation> blockSoups = new ArrayList<>();
    public static ArrayList<MapLocation> refineries = new ArrayList<>();
    private static Direction scoutPath = randomDirection();
    public static Queue<MapLocation> vaporators;
    public static Queue<MapLocation> netGuns;
    public static MapLocation fc = null,ds = null;
    public static MapLocation pickup = null;
    public static boolean attack;

    Miner(RobotController r) throws GameActionException{
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (rc.getRoundNum() == 3) {
            builder = true;
            vaporators = new ArrayDeque<>();
            netGuns = new ArrayDeque<>();
            if (attack){
                vaporators.add(HQ.add(Direction.NORTH).add(Direction.NORTHWEST));
                vaporators.add(HQ.add(Direction.NORTH).add(Direction.NORTHEAST));
                vaporators.add(HQ.add(Direction.EAST).add(Direction.NORTHEAST));
                vaporators.add(HQ.add(Direction.EAST).add(Direction.SOUTHEAST));
                vaporators.add(HQ.add(Direction.SOUTH).add(Direction.SOUTHEAST));
                vaporators.add(HQ.add(Direction.SOUTH).add(Direction.SOUTHWEST));
                vaporators.add(HQ.add(Direction.WEST).add(Direction.SOUTHWEST));
                vaporators.add(HQ.add(Direction.WEST).add(Direction.NORTHWEST));

                netGuns.add(HQ.add(Direction.NORTHWEST).add(Direction.NORTHWEST));
                netGuns.add(HQ.add(Direction.NORTHEAST).add(Direction.NORTHEAST));
                netGuns.add(HQ.add(Direction.SOUTHEAST).add(Direction.SOUTHEAST));
                netGuns.add(HQ.add(Direction.SOUTHWEST).add(Direction.SOUTHWEST));
            }
            else{
                MapLocation[] edges = new MapLocation[4];
                edges[0] = new MapLocation(0,HQ.y);
                edges[1] = new MapLocation(rc.getMapWidth(),HQ.y);
                edges[2] = new MapLocation(HQ.x,0);
                edges[3] = new MapLocation(HQ.x,rc.getMapHeight());
                MapLocation v = HQ.add(HQ.directionTo(closestLocation(edges)));
                vaporators.add(v);
                for (Direction dir : Direction.cardinalDirections()){
                    if (!v.equals(HQ.add(dir)))
                        netGuns.add(HQ.add(dir));
                }
            }
        }
        refineries.add(HQ);
        //scoutPath = rc.getLocation().directionTo(HQ).opposite();
    }

    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        MapLocation [] soups = rc.senseNearbySoup();
        if (soups.length == 0) {
            ArrayList<MapLocation> toRemove = new ArrayList<>();
            for (MapLocation soup : blockSoups) {
                if (rc.canSenseLocation(soup))
                    toRemove.add(soup);
            }
            blockSoups.removeAll(toRemove);
        }
        if (builder)
            doBuilderThings(soups);
        else
            doMinerThings(soups);
    }

    public int doBuilderThings(MapLocation [] soups) throws GameActionException {
        System.out.println("let's build shit");//let's build shit
        if (!design_school && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
            System.out.println("let's build a design school");//let's build shit
            buildDesignSchool();
            return 6;
        }else if (!fulfillment_center && design_school &&rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
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
        } else if (netGuns.isEmpty() && vaporators.isEmpty()) {
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
        } else if(rc.getLocation().isAdjacentTo(HQ)){
            for (Direction dir : directions)
                tryMove(dir);
        } else
            pathTo(HQ);
        return 5;
    }

    public int doMinerThings(MapLocation[] soups) throws GameActionException{
        if (rc.getLocation().distanceSquaredTo(HQ) <=18 && rc.senseElevation(rc.getLocation()) - hqElevation >= 15) {
            if (pickup == null || pickup != rc.getLocation()) {
                MapLocation dropoff;
                if (!blockSoups.isEmpty())
                    dropoff = closestLocation(blockSoups.toArray(new MapLocation[0]));
                else {
                    dropoff = HQ.translate(-5, 0);
                    if (dropoff.x < 0)
                        dropoff = HQ.translate(5, 0);
                }
                if(askForDrone(dropoff))
                    pickup = rc.getLocation();
                return 0;
            }
        }
        for (RobotInfo r : rc.senseNearbyRobots()){
            if (r.type == RobotType.REFINERY && !refineries.contains(r.location))
                refineries.add(r.location);
        }
        //if soups in range
        if (soups.length > 0) {
            //if we can carry more soup, then go get more soup
            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit)
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

    public boolean askForDrone(MapLocation target)throws GameActionException {
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = NEED_DELIVERY;
        msg[2] = target.x;
        msg[3] = target.y;
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
                    //fc = new MapLocation(t.getMessage()[2],t.getMessage()[3]);
                    ds = new MapLocation(t.getMessage()[4],t.getMessage()[5]);
                } else if (t.getMessage()[1] == START_PHASE_2){
                    refineries.remove(HQ);
                }
            }
        }
        return res;
    }

    private boolean buildRefinery() throws GameActionException {
        for (Direction dir : directions) {
            if (tryBuild(RobotType.REFINERY, dir)) {
                for (RobotInfo r : rc.senseNearbyRobots()) {
                    if (r.type == RobotType.REFINERY && !refineries.contains(r.location))
                        refineries.add(r.location);
                }
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = REFINERY_BUILT;
                msg[2] = rc.getLocation().add(dir).x;
                msg[3] = rc.getLocation().add(dir).y;
                sendMessage(msg, DEFCON4);
                return true;
            }
        }
        return false;
    }

    private void findSoup() throws GameActionException{
        if (blockSoups.isEmpty())
            scout();
        else
            pathTo(closestLocation(blockSoups.toArray(new MapLocation[0])));
    }

    private void scout() throws GameActionException{
        if (rc.isReady()) {
            while(!tryMove(scoutPath))
                scoutPath = randomDirection();
        }
    }

    private boolean buildDesignSchool()throws GameActionException {
        if (rc.getLocation().equals(ds)) {
            for (Direction dir : directions)
                tryMove(dir);
        }
        if (rc.canSenseLocation(ds) && rc.getLocation().isAdjacentTo(ds)) {
            if (tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(ds))) {
                design_school = true;
                return true;
            }
        } else
            pathTo(ds);
        return false;

    }

    private boolean buildFulfillmentCenter()throws GameActionException{
        /*
        if (rc.getLocation().equals(fc)){
            for (Direction dir: directions)
                tryMove(dir);
        }
        else if (rc.getLocation().isAdjacentTo(fc)) {
            if (tryBuild(RobotType.FULFILLMENT_CENTER, rc.getLocation().directionTo(fc))) {
                fulfillment_center = true;
                return true;
            }
        }
        else
            pathTo(fc);
            */
        for (Direction dir : directions){
            if (rc.getLocation().add(dir).distanceSquaredTo(HQ) <= 13 || rc.getLocation().add(dir).distanceSquaredTo(HQ) == 18)
                continue;
            else if (rc.getLocation().add(dir).isAdjacentTo(ds))
                continue;
            if (tryBuild(RobotType.FULFILLMENT_CENTER,dir)){
                fulfillment_center = true;
                return true;
            }
        }
        Direction dir = rc.getLocation().directionTo(HQ).opposite();
        for (int i = 0; i < 8; i++){
            tryMove(dir);
            dir = dir.rotateLeft();
        }
        return false;
    }

    private boolean buildNetGun()throws GameActionException {
        MapLocation target = closestLocation(netGuns.toArray(new MapLocation[0]));
        if (rc.getLocation().equals(target)){
            for (Direction dir : directions)
                tryMove(dir);
        }
        else if (rc.getLocation().isAdjacentTo(target)) {
            if (tryBuild(RobotType.NET_GUN, rc.getLocation().directionTo(target))){
                netGuns.remove(target);
                return true;
            }
            return false;
        }
        else
            pathTo(target);
        return false;
    }

    private boolean buildVaporator()throws GameActionException {
        MapLocation target = closestLocation(vaporators.toArray(new MapLocation[0]));
        if (rc.getLocation().equals(target)){
            for (Direction dir : directions)
                tryMove(dir);
        }
        else if (rc.getLocation().isAdjacentTo(target)) {
            if (tryBuild(RobotType.VAPORATOR, rc.getLocation().directionTo(target))){
                vaporators.remove(target);
                return true;
            }
            return false;
        }
        else
            pathTo(target);
        return false;
    }

    //broadcast undiscovered soup locations
    private boolean broadcastSoup(MapLocation[] soups) throws GameActionException {
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = SOUPS_FOUND;
        int i;
        for (i = 0; i < soups.length && i < 5; i++)
            msg[i+2] = 100*soups[i].x + soups[i].y;
        return sendMessage(msg, DEFCON5);
    }

    private void mineSoup(MapLocation[] soups) throws GameActionException{
        ArrayList<MapLocation> newSoup = new ArrayList<>();
        for (MapLocation soup : soups) {
            if (!blockSoups.contains(soup)) {
                blockSoups.add(soup);
                newSoup.add(soup);
            }
            if (rc.getLocation().isAdjacentTo(soup))
                tryMine(rc.getLocation().directionTo(soup));
        }
        if(newSoup.size() > 0)
            broadcastSoup(newSoup.toArray(new MapLocation[0]));
        if (rc.isReady())
            pathTo(closestLocation(soups));
    }

    private void findRefinery(MapLocation[] soups)throws GameActionException{
        if (refineries.isEmpty()){
            if (rc.senseNearbySoup().length > 0){
                if (rc.getTeamSoup() >= RobotType.REFINERY.cost)
                    buildRefinery();
            }
            else
                findSoup();
        }
        else{
            MapLocation target = closestLocation(refineries.toArray(new MapLocation[0]));
            if (rc.canSenseLocation(target) && rc.getLocation().isAdjacentTo(target))
                tryRefine(rc.getLocation().directionTo(target));
            if (rc.getLocation().distanceSquaredTo(target) < 100)
                pathTo(target);
            else if (soups.length > 0 && rc.getSoupCarrying() == RobotType.MINER.soupLimit && rc.getTeamSoup() >= RobotType.REFINERY.cost)
                buildRefinery();
            else if (rc.getSoupCarrying() < RobotType.MINER.soupLimit)
                findSoup();
        }
    }
}
