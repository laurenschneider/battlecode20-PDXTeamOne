package pdx_team_one;
import battlecode.common.*;

public class DeliveryDrone extends Robot{

    DeliveryDrone(RobotController r) {
        super(r);
    }

    private boolean [] nearHQ = new boolean[8];

    public void takeTurn() throws GameActionException {
        //super.takeTurn();
        runDeliveryDrone();
    }

    private void parseBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for (Transaction t : rc.getBlock(i)){
                if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_LOCATION){
                    HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    t.getMessage()[4] = hqID;
                }
                else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == ENEMY_HQ_FOUND){
                    enemyHQ = new MapLocation(t.getMessage()[2],t.getMessage()[3]);
                    t.getMessage()[4] = enemyHQID;
                }
                else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_TARGET_ACQUIRED){
                    nearHQ[t.getMessage()[2]] = true;
                }
            }
        }
    }

    public void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();

        if (rc.isCurrentlyHoldingUnit()) {
            tryMove(randomDirection());
        } else if (rc.isReady()) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            if (robots.length == 0) {
                parseBlockchain();
                rc.move(rc.getLocation().directionTo(HQ));
            }
            for (RobotInfo r : robots) {
                if (r.team == enemy) {
                    if (rc.getLocation().isAdjacentTo(r.getLocation())) {
                        if (rc.canPickUpUnit(r.getID())) {
                            rc.pickUpUnit(r.getID());
                            rc.move(randomDirection());
                            rc.dropUnit(rc.getLocation().directionTo(HQ).opposite());
                            break;
                        }
                    }
                }
            }
            //move to the enemy direction
            for (RobotInfo r : robots) {
                if (r.team == enemy) {
                    rc.move(rc.getLocation().directionTo(r.getLocation()));
                }
            }
        }
    }
}