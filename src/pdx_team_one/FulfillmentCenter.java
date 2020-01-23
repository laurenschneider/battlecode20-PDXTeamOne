package pdx_team_one;
import battlecode.common.*;

public class FulfillmentCenter extends Robot{

    FulfillmentCenter(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }
}
