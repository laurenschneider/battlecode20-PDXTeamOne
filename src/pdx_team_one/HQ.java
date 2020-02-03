package pdx_team_one;
import battlecode.common.*;

public class HQ extends Robot{

    static int numMiners = 0;
    static boolean locationSent = false;

    HQ(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        if (!locationSent) {
           sendLocation();
           locationSent = true;
        }
        buildMiners();
        checkElevation();
        defense();
    }

    private void sendLocation() throws GameActionException {
        int [] message = new int[7];
        message[0] = TEAM_ID;      // 8 ones means it's us
        message[1] = HQ_LOCATION;
        message[2] = rc.getLocation().x;
        message[3] = rc.getLocation().y;
        message[4] = rc.getID();
        sendMessage(message,50);
    }

    public void buildMiners() throws GameActionException {
        if(numMiners < 5) {
            for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) {
                    numMiners++;
                }
            }
        }
    }

    public void checkElevation() throws GameActionException {
        if((rc.senseElevation(rc.getLocation()) - GameConstants.MIN_WATER_ELEVATION) < 50 ) {
            // HQ is in danger, take action to terraform around HQ
            int [] message = new int[7];
            message[0] = TEAM_ID;      // 8 ones means it's us
            message[1] = HQ_FLOOD_DANGER;             // 1 means HQ is in danger of flooding
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
