package pdx_team_one;
import battlecode.common.*;

public class Landscaper extends Robot{
    MapLocation hqLocation;

    Landscaper(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException{
        // find HQ on map
        getHQLoc();

        // check if HQ is in trouble of flooding

        // find a place to build

        // need a method to dig
    }

    private void getHQLoc() throws GameActionException {
        if (hqLocation == null) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLocation = robot.location;
                }
            }

            if (hqLocation == null) {
                // if still null, search the blockchain
            }
        }
    }
}
