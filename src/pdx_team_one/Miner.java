package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;

public class Miner extends Robot{

    private static int soup_threshold =  RobotType.MINER.soupLimit/2;
    private static boolean scout = false;
    private static boolean enemy_design_school = false;
    private static ArrayList<MapLocation> blockSoups = new ArrayList<>();
    private static ArrayList<MapLocation> refineries = new ArrayList<>();
    private static Direction scoutPath = Direction.NORTH;

    Miner(RobotController r) throws GameActionException{
        super(r);
        if (rc.getRoundNum() == 2)
            scout = true;
        map = new int[rc.getMapWidth()][rc.getMapHeight()];
        for (int i = 1; i < rc.getRoundNum(); i++)
            parseBlockchain(i);
        refineries.add(HQ);
        updateLocalMap(false);
    }
    public void takeTurn() throws GameActionException {
        parseBlockchain(rc.getRoundNum() - 1);
        if (scout)
            doScoutThings();
        else
            doMinerThings();
    }

    private void doScoutThings() throws GameActionException{
        //if it knows where enemyHQ use, build a design school to bury it
        if (enemyHQ != null && !enemy_design_school){
            Direction dir = rc.getLocation().directionTo(enemyHQ);
            for (int i = 0; i < 8; i++) {
                if (tryBuild(RobotType.DESIGN_SCHOOL, dir))
                    enemy_design_school = true;
                else
                    dir = dir.rotateLeft();
            }
            return;
        }
        //look for enemy HQ
        else if (enemyHQ == null) {
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if (r.team != rc.getTeam() && r.type == RobotType.HQ) {
                    int[] msg = new int[7];
                    msg[0] = TEAM_ID;
                    msg[1] = ENEMY_HQ_FOUND;
                    msg[2] = r.getLocation().x;
                    msg[3] = r.getLocation().y;
                    msg[4] = r.getID();
                    sendMessage(msg, DEFCON1);
                    enemyHQ = new MapLocation(r.getLocation().x, r.getLocation().y);
                    Direction dir = rc.getLocation().directionTo(enemyHQ);
                    for (int i = 0; i < 8; i++) {
                        if (tryBuild(RobotType.DESIGN_SCHOOL, dir))
                            enemy_design_school = true;
                        else
                            dir = dir.rotateLeft();
                    }
                    return;
                }
            }
        }

        //broadcast undiscovered soup locations
        MapLocation [] soups = rc.senseNearbySoup();
        if (soups.length > 0) {
            int count = 0;
            int [] msg = new int[7];
            for (MapLocation soup : soups) {
                if (!blockSoups.contains(soup)) {
                    blockSoups.add(soup);
                    if (!rc.canSenseLocation(HQ))
                        count++;
                    msg[count+1] = 100*soup.x + soup.y;
                    if (count == 5)
                        break;
                }
            }
            if (count > 0){
                msg[0] = TEAM_ID;
                msg[1] = SOUPS_FOUND;
                sendMessage(msg,DEFCON4);
            }
        }

        //walk in a direction until he hits a wall, then tries a different direction
        if (rc.getCooldownTurns() < 1) {
            while (!tryMove(scoutPath))
                scoutPath = scoutPath.rotateLeft();
        }


        if (Clock.getBytecodesLeft() < 500)
            return;
        //look for enemy HQ again after moving
        if (enemyHQ == null) {
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if (r.team != rc.getTeam() && r.type == RobotType.HQ && enemyHQ == null) {
                    int[] msg = new int[7];
                    msg[0] = TEAM_ID;
                    msg[1] = ENEMY_HQ_FOUND;
                    msg[2] = r.getLocation().x;
                    msg[3] = r.getLocation().y;
                    sendMessage(msg, DEFCON5);
                    enemyHQ = new MapLocation(r.getLocation().x, r.getLocation().y);
                }
            }
        }
    }

    private void doMinerThings() throws GameActionException{
        for (RobotInfo r : rc.senseNearbyRobots()){
            if (r.type == RobotType.DESIGN_SCHOOL)
                design_school = true;
            if (r.type == RobotType.REFINERY && !refineries.contains(r.location))
                refineries.add(r.location);
        }
        //around Round 100, miners get in the way of landscapers, so at this point miners need to move on
        if(rc.getRoundNum() > 100)
            refineries.remove(HQ);
        //try to build a design school
        if (!design_school && rc.getLocation().distanceSquaredTo(HQ) >= 4) {
            Direction dir = rc.getLocation().directionTo(HQ).opposite();
            for (int i = 0; i < 8; i++) {
                if (tryBuild(RobotType.DESIGN_SCHOOL, dir)) {
                    design_school = true;
                    int[] msg = new int[7];
                    msg[0] = TEAM_ID;
                    msg[1] = DESIGN_SCHOOL_BUILT;
                    msg[2] = rc.adjacentLocation(dir).x;
                    msg[3] = rc.adjacentLocation(dir).y;
                    sendMessage(msg, DEFCON3);
                    return;
                }
                dir = dir.rotateRight();
            }
        }

        if (!fulfillment_center && design_school && rc.getLocation().distanceSquaredTo(HQ) >= 6) {
            Direction dir = rc.getLocation().directionTo(HQ).opposite();
            for (int i = 0; i < 8; i++) {
                if (tryBuild(RobotType.FULFILLMENT_CENTER, dir)) {
                    fulfillment_center = true;
                    int[] msg = new int[7];
                    msg[0] = TEAM_ID;
                    msg[1] = FULFILLMENT_CENTER_BUILT;
                    msg[2] = rc.adjacentLocation(dir).x;
                    msg[3] = rc.adjacentLocation(dir).y;
                    sendMessage(msg, DEFCON3);
                    return;
                }
                dir = dir.rotateRight();
            }
        }

        MapLocation[] soups = rc.senseNearbySoup();
        //if soups in range
        if (soups.length > 0) {
            //if we can carry more soup, then go get more soup
            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                for (MapLocation soup : soups) {
                    if (!blockSoups.contains(soup))
                        blockSoups.add(soup);
                    if (rc.getLocation().isAdjacentTo(soup)) {
                        tryMine(rc.getLocation().directionTo(soup));
                        return;
                    }
                }
                pathTo(soups[0]);
                return;
            }
            //otherwise either find a refinery or build a refinery
            int distance = 500000;
            destination = HQ;
            for (MapLocation r : refineries) {
                if (rc.getLocation().distanceSquaredTo(r) < distance) {
                    destination = r;
                    distance = rc.getLocation().distanceSquaredTo(r);
                }
            }
            if (rc.getLocation().isAdjacentTo(destination))
                tryRefine(rc.getLocation().directionTo(destination));
            else if (distance < 50)
                pathTo(destination);
            else if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
                for (Direction dir : directions)
                    if (tryBuild(RobotType.REFINERY, dir))
                        break;
            }
            else
                pathTo(destination);
        }
        //if no soups in range
        else {
            ArrayList<MapLocation> toRemove = new ArrayList<>();
                for (MapLocation soup : blockSoups) {
                    if (rc.canSenseLocation(soup))
                        toRemove.add(soup);
            }
            blockSoups.removeAll(toRemove);
            //if we're carrying enough soup worth refining
            if (rc.getSoupCarrying() > soup_threshold) {
                //find a refinery and go refine it
                int distance = 50000;
                MapLocation refine = HQ;
                for (MapLocation r : refineries){
                    if (rc.getLocation().distanceSquaredTo(r) < distance){
                        distance = rc.getLocation().distanceSquaredTo(r);
                        refine = r;
                    }
                }
                if (rc.getLocation().isAdjacentTo(refine))
                    tryRefine(rc.getLocation().directionTo(refine));
                else
                    pathTo(refine);
            }
            //go explore for soup
            else {
                int distance = 50000;
                for (MapLocation soup : blockSoups) {
                    if (rc.getLocation().distanceSquaredTo(soup) < distance) {
                        distance = rc.getLocation().distanceSquaredTo(soup);
                        destination = soup;
                    }
                }
                if (destination == null || rc.canSenseLocation(destination))
                    destination = randomUnexploredLocation();
                pathTo(destination);
            }
        }
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

    static int parseBlockchain(int round) throws GameActionException {
        int res = 0;
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID) {
                if (t.getMessage()[1] == DESIGN_SCHOOL_BUILT) {
                    design_school = true;
                    res = 1;
                } else if (t.getMessage()[1] == FULFILLMENT_CENTER_BUILT) {
                    fulfillment_center = true;
                    res = 2;
                } else if (t.getMessage()[1] == HQ_LOCATION) {
                    HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    hqID = t.getMessage()[4];
                    hqElevation = t.getMessage()[5];
                    res = 3;
                } else if (t.getMessage()[1] == SOUPS_FOUND) {
                    for (int j = 2; j < 7 && t.getMessage()[j] != 0; j++)
                        blockSoups.add(new MapLocation(t.getMessage()[j] / 100, t.getMessage()[j] % 100));
                    res = 4;
                } else if (t.getMessage()[0] % 10000 == TEAM_ID) {
                    updateMap(t.getMessage());
                    res = 5;
                }
            }
        }
        return res;
    }
}
