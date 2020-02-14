package pdx_team_one;
import battlecode.common.*;

public class NetGun extends Robot{
    NetGun(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException{
        for (RobotInfo r : rc.senseNearbyRobots()){
            if (r.getTeam() != rc.getTeam() && rc.canShootUnit(r.ID))
                rc.shootUnit(r.ID);
        }
    }
}
