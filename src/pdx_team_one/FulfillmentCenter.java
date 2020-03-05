package pdx_team_one;
import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

public class FulfillmentCenter extends Robot{

    private Set<Integer> landscapers = new HashSet<>();
    int num = 0;
    int maxDrones;
    FulfillmentCenter(RobotController r)throws GameActionException {
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
    }

    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (rc.getTeamSoup() >= RobotType.REFINERY.cost && num < maxDrones)
            num = buildDrones();
        else if (num == maxDrones)
            rc.disintegrate();
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
                if (t.getMessage()[1] == ATTACK) {
                    maxDrones = 100000;
                } else if (t.getMessage()[1] == DEFENSE) {
                    maxDrones = 3;
                }
            }
        }
        return res;
    }
}
