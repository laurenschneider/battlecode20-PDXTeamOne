package pdx_team_one;
import battlecode.common.*;

public class HQ extends Robot{

    int numMiners = 0;
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
        defense();
    }

    public boolean sendLocation() throws GameActionException {
        int [] message = new int[7];
        message[0] = TEAM_ID;      // 8 ones means it's us
        message[1] = HQ_LOCATION;
        message[2] = rc.getLocation().x;
        message[3] = rc.getLocation().y;
        message[4] = rc.getID();
        message[5] = rc.senseElevation(rc.getLocation());

        return sendMessage(message,DEFCON4);
    }

    public int buildMiners() throws GameActionException {
        if(numMiners < 5) {
            for (Direction dir : directions) {
                boolean res = tryBuild(RobotType.MINER, dir);
                if (res) {
                    numMiners++;
                }
            }
        }
        return numMiners;
    }

    public boolean defense() throws GameActionException {
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
        return false;
    }
}
