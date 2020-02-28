package pdx_team_one;
import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

public class DesignSchool extends Robot{
    int numLS = 0;
    private Set<Integer> drones = new HashSet<>();

    DesignSchool(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException{
        int vapors = 0;
        int netguns = 0;
        for (RobotInfo r : rc.senseNearbyRobots()){
            if (r.team == rc.getTeam() && r.type == RobotType.DELIVERY_DRONE)
                drones.add(r.ID);
            if (r.team == rc.getTeam() && r.type == RobotType.VAPORATOR)
                vapors++;
            if (r.team == rc.getTeam() && r.type == RobotType.NET_GUN)
                netguns++;
        }
        if (numLS >= 8 && 2*numLS > drones.size())
            return;
        if ((vapors < 4 || netguns < 4) && numLS >= 8)
            return;
        for (Direction dir : directions) {
            if (tryBuild(RobotType.LANDSCAPER, dir))
                numLS++;
        }
    }
}
