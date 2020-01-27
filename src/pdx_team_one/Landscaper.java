package pdx_team_one;
import battlecode.common.*;

public class Landscaper extends Robot{
    private MapLocation hqLocation;
    private int hqID = 0;

    Landscaper(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        // find HQ on map
        if (hqLocation == null)
            getHQLoc();

        //if we're near HQ, terraform around it
        if (rc.canSenseRobot(hqID))
            terraform();
        else
            pathTo(hqLocation);
    }

    private void getHQLoc() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                hqLocation = robot.location;
                return;
            }
        }
        // if still null, search the blockchain
        for (int i = 1; i < rc.getRoundNum(); i++){
            for (Transaction t : rc.getBlock(i)){
                if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_LOCATION){
                    t.getMessage()[2] = hqLocation.x;
                    t.getMessage()[3] = hqLocation.y;
                    t.getMessage()[4] = hqID;
                    return;
                }
            }
        }
    }

    private void terraform()throws GameActionException{
        MapLocation highest = rc.getLocation();
        MapLocation lowest = rc.getLocation();
        MapLocation current = rc.getLocation();

        int high = rc.senseElevation(highest);
        int low = rc.senseElevation(highest);

        //find the highest and lowest elevations
        for (int i = -4; i <= 4; i++){
            for (int j = -4; j <= 4; j++){
                MapLocation check = new MapLocation(current.x + i, current.y + j);
                if (rc.canSenseLocation(check)){
                    int elevation = rc.senseElevation(check);
                    if (elevation > high){
                        high = elevation;
                        highest = check;
                    }
                    if (elevation < low){
                        low = elevation;
                        lowest = check;
                    }
                }
            }
        }

        if (current.isAdjacentTo(lowest) && rc.getDirtCarrying() > 0){
                if (tryDeposit(current.directionTo(lowest)))
                    return;
        }

        if (current.isAdjacentTo(highest) && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit){
                if (tryDig(current.directionTo(highest)))
                    return;
        }


        if (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit){
            if (rc.getLocation() == lowest){
                for (Direction dir : directions){
                    pathTo(rc.adjacentLocation(dir));
                }
            }
            else if (current.isAdjacentTo(lowest)){
                tryDeposit(current.directionTo(lowest));
            }
            else
                pathTo(lowest);
        }
        else{
            if (rc.getLocation() == highest){
                for (Direction dir : directions){
                    pathTo(rc.adjacentLocation(dir));
                }
            }
            else if (current.isAdjacentTo(highest)){
                tryDig(current.directionTo(highest));
            }
            else
                pathTo(highest);

        }
    }

    private boolean tryDeposit(Direction dir) throws GameActionException{
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

    private boolean tryDig(Direction dir) throws GameActionException{
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }
        return false;
    }
}
