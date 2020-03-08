package pdx_team_one;
import battlecode.common.*;

public class Refinery extends Robot{
    int usage = 0;
    Refinery(RobotController r) {
        super(r);
    }
    public void takeTurn() throws GameActionException {
        if (usage >= 100)
            rc.disintegrate();
        MapLocation[] soups = rc.senseNearbySoup();
        if (soups.length > 0)
            usage = 0;
        for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (r.type == RobotType.MINER) {
                usage = 0;
                return;
            }
        }
        usage++;
    }
}
