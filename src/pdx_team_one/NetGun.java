package pdx_team_one;
import battlecode.common.*;

//the net gun just shoots down enemy drones
public class NetGun extends Building{
    NetGun(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException{
        defense();
    }
}
