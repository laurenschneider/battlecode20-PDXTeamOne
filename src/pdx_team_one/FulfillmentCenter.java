package pdx_team_one;
import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

public class FulfillmentCenter extends Robot{

    private Set<Integer> landscapers = new HashSet<>();
    int num = 0;
    FulfillmentCenter(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        int vapors = 0;
        int netguns = 0;
        for (RobotInfo r : rc.senseNearbyRobots()){
            if (r.team == rc.getTeam() && r.type == RobotType.LANDSCAPER)
                landscapers.add(r.ID);
            if (r.team == rc.getTeam() && r.type == RobotType.VAPORATOR)
                vapors++;
            if (r.team == rc.getTeam() && r.type == RobotType.NET_GUN)
                netguns++;
        }
        if ((vapors < 4 || netguns < 4) && num > 3)
            return;
        if (num < 2*landscapers.size() || num <= 3) {
            for (Direction dir : directions)
                if (tryBuild(RobotType.DELIVERY_DRONE, dir))
                    num++;
        }
    }
}
