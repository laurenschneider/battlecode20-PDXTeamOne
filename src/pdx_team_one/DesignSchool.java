package pdx_team_one;
import battlecode.common.*;

public class DesignSchool extends Robot{
    DesignSchool(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException{
        for (Direction dir : directions) {
            tryBuild(RobotType.LANDSCAPER, dir);
        }
    }
}
