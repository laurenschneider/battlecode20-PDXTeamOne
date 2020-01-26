package pdx_team_one;
import battlecode.common.*;

public class HQ extends Robot{

    private static int numMiners = 0;

    HQ(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        buildMiners();
        checkElevation();
        defense();
    }

    private void buildMiners() throws GameActionException {
        //todo: this will create a miner for every available direction, up to 8. Is this intended?
        if(numMiners < 5) {
            for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) {
                    numMiners++;
                }
            }
        }
    }

    private void checkElevation() throws GameActionException {
        if((rc.senseElevation(rc.getLocation()) - GameConstants.MIN_WATER_ELEVATION) < 50 ) {
            // HQ is in danger, take action to terraform around HQ
            int [] message = new int[7];
            message[0] = 11111111;      // 8 ones means it's us
            message[1] = 0;             // 0 means HQ is in danger of flooding
            sendMessage(message,50);
        }
    }

    private void defense() throws GameActionException {
        // HQ has built in net gun
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, opponent);
        for (RobotInfo e : enemiesInRange) {
            if (e.type == RobotType.DELIVERY_DRONE) {
                if (rc.canShootUnit(e.ID)){
                    rc.shootUnit(e.ID);
                    break;
                }
            }
        }
    }
}
