package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;

public class Miner extends Robot{

    private static int soup_threshold =  RobotType.MINER.soupLimit/2;
    private static boolean scout = false;
    private static boolean enemy_design_school = false;
    private static ArrayList<MapLocation> blockSoups = new ArrayList<>();
    private static ArrayList<MapLocation> refineries = new ArrayList<>();

    Miner(RobotController r) throws GameActionException{
        super(r);
        if (rc.getRoundNum() == 2)
            scout = true;
        map = new int[rc.getMapWidth()][rc.getMapHeight()];
        for (int i = 1; i < rc.getRoundNum(); i++)
            parseBlockchain(i);
        refineries.add(HQ);
    }
    public void takeTurn() throws GameActionException {
        parseBlockchain(rc.getRoundNum() - 1);

        if (scout)
            doScoutThings();
        else
            doMinerThings();
    }

    void doScoutThings() throws GameActionException{
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

        if (destination == null || rc.canSenseLocation(destination))
            destination = randomUnexploredLocation();
        //if randomUnexploredLocation() returns null, no more land to explore, scout now needs to mine
        if (destination == null) {
            scout = false;
            doMinerThings();
            return;
        }
        if (rc.getCooldownTurns() <= 1)
            pathTo(destination);

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

    void doMinerThings() throws GameActionException{
        for (RobotInfo r : rc.senseNearbyRobots()){
            if (r.type == RobotType.DESIGN_SCHOOL)
                design_school = true;
            if (r.type == RobotType.REFINERY && !refineries.contains(r.location))
                refineries.add(r.location);
        }
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

        MapLocation soups[] = rc.senseNearbySoup();
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
            int distance = 100;
            for (MapLocation r : refineries) {
                if (rc.getLocation().distanceSquaredTo(r) < distance) {
                    destination = r;
                    distance = rc.getLocation().distanceSquaredTo(r);
                }
            }

            if (distance < 100) {
                pathTo(destination);
                return;
            }
            if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
                for (Direction dir : directions)
                    if (tryBuild(RobotType.REFINERY, dir))
                        return;
            }

            distance = 50000;
            for (MapLocation r : refineries) {
                if (rc.getLocation().distanceSquaredTo(r) < distance) {
                    distance = rc.getLocation().distanceSquaredTo(r);
                    destination = r;
                }
            }
            pathTo(destination);
            return;
        }
        //if no soups in range
        else {
            ArrayList<MapLocation> toRemove = new ArrayList<>();
            if (!blockSoups.isEmpty()) {
                for (MapLocation soup : blockSoups) {
                    if (rc.canSenseLocation(soup))
                        toRemove.add(soup);
                       // blockSoups.remove(soup);
                }
            }
            blockSoups.removeAll(toRemove);
            //if we're carrying enough soup worth refining
            if (rc.getSoupCarrying() > soup_threshold) {
                //find a refinery and go refine it
                for (RobotInfo bot : rc.senseNearbyRobots()) {
                    if (bot.type == RobotType.REFINERY || bot.type == RobotType.HQ) {
                        if (!refineries.contains(bot.location))
                            refineries.add(bot.location);
                        if (rc.getLocation().isAdjacentTo(bot.location))
                            tryRefine(rc.getLocation().directionTo(bot.location));
                        else
                            pathTo(bot.location);
                        return;
                    }
                }
                //find the nearest refinery
                int distance = 50000;
                MapLocation refine = HQ;
                for (MapLocation r : refineries){
                    if (rc.getLocation().distanceSquaredTo(r) < distance){
                        distance = rc.getLocation().distanceSquaredTo(r);
                        refine = r;
                    }
                }
                pathTo(refine);
                return;
            }
            //go explore for soup
            else {
                int distance = 50000;
                if (!blockSoups.isEmpty()) {
                    for (MapLocation soup : blockSoups) {
                        if (rc.getLocation().distanceSquaredTo(soup) < distance) {
                            distance = rc.getLocation().distanceSquaredTo(soup);
                            destination = soup;
                        }
                    }
                }
                else if (destination == null || rc.canSenseLocation(destination))
                    destination = randomUnexploredLocation();
                pathTo(destination);
            }
        }
    }

    private static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    private static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    static void parseBlockchain(int round) throws GameActionException {
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID) {
                if (t.getMessage()[1] == DESIGN_SCHOOL_BUILT)
                    design_school = true;
                else if (t.getMessage()[1] == HQ_LOCATION) {
                    HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    hqID = t.getMessage()[4];
                    hqElevation = t.getMessage()[5];
                } else if (t.getMessage()[1] == SOUPS_FOUND) {
                    for (int j = 2; j < 7 && t.getMessage()[j] != 0; j++)
                        blockSoups.add(new MapLocation(t.getMessage()[j] / 100, t.getMessage()[j] % 100));
                } else if (t.getMessage()[0] % 10000 == TEAM_ID) {
                    updateMap(t.getMessage());
                }
            }
        }
    }
}
