package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;

public class Miner extends Robot{

    private static int soup_threshold =  RobotType.MINER.soupLimit/2;
    public static boolean scout = false;
    public static boolean builder = false;
    public static boolean enemy_design_school = false;
    private static ArrayList<MapLocation> blockSoups = new ArrayList<>();
    public static ArrayList<MapLocation> refineries = new ArrayList<>();
    private static Direction scoutPath = Direction.NORTH;
    public int vaporators = 0;

    Miner(RobotController r) throws GameActionException{
        super(r);
        if (rc.getRoundNum() == 2)
            scout = true;
        if (rc.getRoundNum() == 3)
            builder = true;
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

    public int doScoutThings() throws GameActionException{
        //if it knows where enemyHQ use, build a design school to bury it
        if (enemyHQ != null && !enemy_design_school){
            if (buildDesignSchool(rc.getLocation().directionTo(enemyHQ))) {
                enemy_design_school = true;
                return 1;
            }
        }
        //look for enemy HQ
        else if (enemyHQ == null && findEnemyHQ()) {
            return 2;
        }

        //let everyone know where soup is
        broadcastSoup();
        //walk in a direction until he hits a wall, then tries a different direction
        scout();

        if (Clock.getBytecodesLeft() < 500)
            return 3;
        //look for enemy HQ again after moving
        if (enemyHQ == null) {
            findEnemyHQ();
        }
        return 4;
    }

    public int doMinerThings() throws GameActionException{
        for (RobotInfo r : rc.senseNearbyRobots()){
            if (r.type == RobotType.DESIGN_SCHOOL)
                design_school = true;
            if (r.type == RobotType.REFINERY && !refineries.contains(r.location))
                refineries.add(r.location);
        }
        MapLocation[] soups = rc.senseNearbySoup();

        if (builder && (vaporators == 0 || (design_school && fulfillment_center))){
            Direction dir = rc.getLocation().directionTo(HQ).opposite();
            if(buildVaporator(dir))
                return 5;
        }
        //try to build a design school
        if (vaporators > 0 && builder && soups.length == 0 && !design_school && rc.getLocation().distanceSquaredTo(HQ) >= 4) {
            Direction dir = rc.getLocation().directionTo(HQ).opposite();
            if (buildDesignSchool(dir)){
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = DESIGN_SCHOOL_BUILT;
                msg[2] = rc.adjacentLocation(dir).x;
                msg[3] = rc.adjacentLocation(dir).y;
                sendMessage(msg, DEFCON3);
                return 1;
            }
        }

        if (vaporators > 0 && builder && !fulfillment_center && design_school && rc.getLocation().distanceSquaredTo(HQ) >= 6) {
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
                    return 2;
                }
                dir = dir.rotateRight();
            }
        }

        //if soups in range
        if (soups.length > 0) {
            //if we can carry more soup, then go get more soup
            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit)
                mineSoup(soups);
            //otherwise either find a refinery or build a refinery
            else
                findRefinery();
            return 3;
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
            if (rc.getSoupCarrying() > soup_threshold)
                findRefinery();
            //go explore for soup
            else
                findSoup();
            return 4;
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

    public int parseBlockchain(int round) throws GameActionException {
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
                } else if (t.getMessage()[1] == REFINERY_BUILT){
                    MapLocation r = new MapLocation(t.getMessage()[2],t.getMessage()[3]);
                    if (!refineries.contains(r))
                        refineries.add(r);
                    res = 4;
                } else if (t.getMessage()[1] == SOUPS_FOUND) {
                    for (int j = 2; j < 7 && t.getMessage()[j] != 0; j++)
                        blockSoups.add(new MapLocation(t.getMessage()[j] / 100, t.getMessage()[j] % 100));
                    res = 5;
                }
            } //else if (t.getMessage()[0] % 10000 == TEAM_ID) {
              //  updateMap(t.getMessage());
            //}
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
        MapLocation s = null;
        for (MapLocation soup : blockSoups) {
            if (s == null || rc.getLocation().distanceSquaredTo(soup) < rc.getLocation().distanceSquaredTo(s))
                s = soup;
        }
        if (s == null)
            scout();
        else
            pathTo(s);
    }

    private void scout() throws GameActionException{
        if (rc.getCooldownTurns() < 1) {
            while(!tryMove(scoutPath))
                scoutPath = randomDirection();
        }
    }

    private boolean buildDesignSchool(Direction toward)throws GameActionException{
        for (int i = 0; i < 8; i++){
            if (tryBuild(RobotType.DESIGN_SCHOOL,toward)){
                if (scout)
                    enemy_design_school = true;
                else
                    design_school = true;
                return true;
            }
            toward = toward.rotateLeft();
        }
        return false;
    }

    private boolean buildVaporator(Direction toward)throws GameActionException{
        for (int i = 0; i < 8; i++){
            if (tryBuild(RobotType.VAPORATOR,toward)) {
                vaporators++;
                return true;
            }
            toward = toward.rotateLeft();
        }
        return false;
    }

    private boolean findEnemyHQ()throws GameActionException{
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
                return buildDesignSchool(rc.getLocation().directionTo(enemyHQ));
            }
        }
        return false;
    }

    //broadcast undiscovered soup locations
    private boolean broadcastSoup() throws GameActionException{
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
                System.out.println("Soups sent");
                msg[0] = TEAM_ID;
                msg[1] = SOUPS_FOUND;
                sendMessage(msg,DEFCON4);
            }
            boolean build = true;
            for (MapLocation r : refineries){
                if (rc.getLocation().distanceSquaredTo(r) < 100)
                    build = false;
            }
            if (build)
                buildRefinery();
            return true;
        }
        return false;
    }

    private void mineSoup(MapLocation[] soups) throws GameActionException{
        for (MapLocation soup : soups) {
            if (!blockSoups.contains(soup))
                blockSoups.add(soup);
            if (rc.getLocation().isAdjacentTo(soup)) {
                tryMine(rc.getLocation().directionTo(soup));
                return;
            }
        }
        pathTo(soups[0]);
    }

    private void findRefinery()throws GameActionException{
        int distance = 500000;
        MapLocation destination = null;
        for (MapLocation r : refineries) {
            if (rc.getLocation().distanceSquaredTo(r) < distance) {
                destination = r;
                distance = rc.getLocation().distanceSquaredTo(r);
            }
        }
        if (distance > 100 && rc.senseNearbySoup().length > 0 && rc.getTeamSoup() >= RobotType.REFINERY.cost)
            buildRefinery();
        else if (destination != null && rc.getLocation().isAdjacentTo(destination))
            tryRefine(rc.getLocation().directionTo(destination));
        else if (distance < 50)
            pathTo(destination);
        else
            scout();
    }
}
