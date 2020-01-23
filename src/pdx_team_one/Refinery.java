package pdx_team_one;
import battlecode.common.*;

public class Refinery extends Robot{
    Refinery(RobotController r) {
        super(r);
    }
    public void takeTurn() throws GameActionException{
        System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()) + "\nSoup total: " + rc.getTeamSoup());
    }
}
