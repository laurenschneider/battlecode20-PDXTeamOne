package pdx_team_one;
import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

public class FulfillmentCenter extends Robot{

    private Set<Integer> landscapers = new HashSet<>();
    int num = 0;
    int maxDrones = 1;
    FulfillmentCenter(RobotController r)throws GameActionException {
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
    }

    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (rc.getTeamSoup() >= RobotType.REFINERY.cost + 5 && num < maxDrones)
            num = buildDrones();
        else if (num < 4 && rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5)
            num = buildDrones();
    }

    int buildDrones() throws GameActionException{
        for (Direction dir : corners) {
            if (tryBuild(RobotType.DELIVERY_DRONE, dir))
                return ++num;
        }
        for (Direction dir : Direction.cardinalDirections()){
            if (tryBuild(RobotType.DELIVERY_DRONE, dir))
                return ++num;
        }
        return num;
    }

    public int parseBlockchain(int round) throws GameActionException {
        int res = 0;
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID) {
                if (t.getMessage()[1] == DEFENSE)
                    maxDrones = 1;
                else if (t.getMessage()[1] == START_PHASE_2)
                    maxDrones = 6;
            }
        }
        return res;
    }
}
