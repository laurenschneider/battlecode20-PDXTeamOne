package pdx_team_one;
import battlecode.common.*;

public class HQ extends Robot{

    private static int numMiners = 0;

    HQ(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        //todo: this will create a miner for every available direction, up to 8. Is this intended?
        if(numMiners < 5) {
            for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) {
                    numMiners++;
                }
            }
        }

        if((rc.senseElevation(rc.getLocation()) - GameConstants.MIN_WATER_ELEVATION) < 50 ) {
            // HQ is in danger, take action to terraform around HQ
        }

        // todo: some kind of defense with the net gun
    }
}
