package pdx_team_one;
import battlecode.common.*;

public class Miner extends Robot{

    private MapLocation spawn_point;
    private static int soup_threshold =  RobotType.MINER.soupLimit/2;

    Miner(RobotController r) {
        super(r);
        spawn_point = r.getLocation();
    }
    public void takeTurn() throws GameActionException {
        if (map == null){
            map = new int[rc.getMapWidth()][rc.getMapHeight()];
            for (int i = 0; i < rc.getMapWidth(); i++){
                for (int j = 0; j < rc.getMapHeight(); j++){
                    map[i][j] = UNEXPLORED;
                }
            }
            parseBlockchain();
        }
        else
            parseBlockchain(rc.getRoundNum()-1);

        MapLocation soups[] = rc.senseNearbySoup();
        //if no soups in range
        if (soups.length == 0){
            //if we're carrying enough soup worth refining
            if (rc.getSoupCarrying() > soup_threshold){
                Direction toward;
                RobotInfo[] bots = rc.senseNearbyRobots();
                //find a refinery and go refine it
                for (RobotInfo bot : bots) {
                    if (bot.type == RobotType.REFINERY || bot.type == RobotType.HQ) {
                        toward = rc.getLocation().directionTo(bot.location);
                        if (rc.getLocation().isAdjacentTo(bot.location))
                            tryRefine(toward);
                        else {
                            while(!tryMove(toward))
                                toward = toward.rotateLeft();
                        }
                        return;
                    }
                }
                //head back to HQ if no refinery in range
                toward = rc.getLocation().directionTo(spawn_point);
                while(!tryMove(toward))
                    toward = toward.rotateLeft();
                return;
            }
            //head away from HQ for soup
            else{
                //try to build a design school
                if (!design_school){
                    for (Direction dir : directions){
                        if (tryBuild(RobotType.DESIGN_SCHOOL,dir)){
                            design_school = true;
                            int[] msg = new int[7];
                            msg[0] = TEAM_ID;
                            msg[1] = DESIGN_SCHOOL_BUILT;
                            msg[2] = rc.adjacentLocation(dir).x;
                            msg[3] = rc.adjacentLocation(dir).y;
                            sendMessage(msg,10);
                            return;
                        }
                    }
                }
                if (destination == null || rc.canSenseLocation(destination)) {
                    destination = null;
                    pathTo(randomLocation());
                }
                else
                    pathTo(destination);
                return;
            }
        }
        //if soup in range
        else {
            //if we an carry more soup, then go get more soup
            if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                for (MapLocation soup : soups) {
                    if (rc.getLocation().isAdjacentTo(soup)) {
                        tryMine(rc.getLocation().directionTo(soup));
                        return;
                    }
                }
                Direction dir = rc.getLocation().directionTo(soups[0]);
                while (!tryMove(dir))
                    dir = dir.rotateLeft();
                return;

            //otherwise either find a refinery or build a refinery
            }
            RobotInfo[] bots = rc.senseNearbyRobots();
            for (RobotInfo bot : bots) {
                if (bot.type == RobotType.REFINERY || bot.type == RobotType.HQ) {
                    Direction toward = rc.getLocation().directionTo(bot.location);
                    if (rc.getLocation().isAdjacentTo(bot.location))
                        tryRefine(toward);
                    else
                        tryMove(toward);
                    return;
                }
            }
            for (Direction dir : directions)
                if (tryBuild(RobotType.REFINERY, dir))
                    return;
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
}
