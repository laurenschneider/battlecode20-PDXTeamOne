package pdx_team_one;
import battlecode.common.*;

public class DesignSchool extends Robot{
    int numLS = 0;
    DesignSchool(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException{
        if (numLS >= 8)
            return;
        for (Direction dir : directions) {
            if (tryBuild(RobotType.LANDSCAPER, dir))
                numLS++;
        }
    }
}
