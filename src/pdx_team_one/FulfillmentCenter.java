package pdx_team_one;
import battlecode.common.*;

public class FulfillmentCenter extends Robot{

    int num = 0;
    FulfillmentCenter(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        if (num < 10) {
            for (Direction dir : directions)
                if (tryBuild(RobotType.DELIVERY_DRONE, dir))
                    num++;
        }
    }
}
