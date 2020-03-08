package pdx_team_one;
import battlecode.common.*;

import java.util.HashSet;

public class DesignSchool extends Robot{
    int numLS = 0;
    int maxLS = 1;
    private boolean droneHomeLocationSent;
    DesignSchool(RobotController r) throws GameActionException{
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
    }

    public void takeTurn() throws GameActionException{
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        if (numLS < maxLS && rc.getTeamSoup() >= RobotType.REFINERY.cost + 5)
            buildLS();
        else if (numLS < 4 && rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5)
            buildLS();
        else if (rc.getRoundNum() > 1000)
            buildLS();
        if (!droneHomeLocationSent)
            droneHomeLocationSent = sendDroneHomeLocation();
    }

    int buildLS() throws GameActionException{
        for (Direction dir : corners) {
            if (tryBuild(RobotType.LANDSCAPER, dir))
                return ++numLS;
        }
        for (Direction dir : Direction.cardinalDirections()){
            if (tryBuild(RobotType.LANDSCAPER, dir))
                return ++numLS;
        }
        return numLS;
    }

    public int parseBlockchain(int round) throws GameActionException {
        int res = 0;
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID) {
                if (t.getMessage()[1] == DEFENSE)
                    maxLS = 1;
                else if (t.getMessage()[1] == START_PHASE_2)
                    maxLS = 10000;
                else if (t.getMessage()[1] == VAPORATOR_BUILT)
                    maxLS = t.getMessage()[2];
            }
        }
        return res;
    }

    public boolean sendDroneHomeLocation() throws GameActionException{
        HashSet<MapLocation> homes = new HashSet<>();
        int pollution = 100000;
        for (Direction dir : directions) {
            MapLocation m = rc.getLocation().add(dir).add(dir);
            if (rc.onTheMap(m)){
                if (rc.sensePollution(m) < pollution) {
                    pollution = rc.sensePollution(m);
                    homes.clear();
                    homes.add(m);
                }
                else if (rc.sensePollution(m) == pollution)
                    homes.add(m);
            }
            m = rc.getLocation().add(dir).add(dir);
            if (rc.onTheMap(m)){
                if (rc.sensePollution(m) < pollution) {
                    pollution = rc.sensePollution(m);
                    homes.clear();
                    homes.add(m);
                }
                else if (rc.sensePollution(m) == pollution)
                    homes.add(m);
            }
        }
        MapLocation ret = null;
        for (MapLocation m : homes){
            if (ret == null)
                ret = m;
            else if (HQ.distanceSquaredTo(m) > HQ.distanceSquaredTo(ret))
                ret = m;
        }
        int [] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = DRONE_HOME;
        msg[2] = ret.x;
        msg[3] = ret.y;
        return sendMessage(msg,DEFCON5);
    }
}
