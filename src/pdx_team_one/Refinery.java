package pdx_team_one;
import battlecode.common.*;

//the refinery refines soup automatically, not much to do here
public class Refinery extends Building{
    private int usage = 0;
    Refinery(RobotController r) {
        super(r);
    }

    //if no one has come near the refinery in a while and there's no soup nearby, then just disintegrate
    public void takeTurn() {
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
