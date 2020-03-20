package pdx_team_one;
import battlecode.common.*;
import java.util.HashSet;

//the Design School is a building that builds landscapers
public class DesignSchool extends Building{
    private int numLS = 0;
    private int maxLS = 1;
    private boolean droneHomeLocationSent;

    DesignSchool(RobotController r) throws GameActionException{
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
    }

    public void takeTurn() throws GameActionException{
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        //the following logic determines whether or not to build a landscaper
        if (numLS < maxLS && rc.getTeamSoup() >= RobotType.REFINERY.cost + 5)
            buildLS();
        else if (numLS < 4 && rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5)
            buildLS();
        else if (rc.getRoundNum() > 1000)
            buildLS();
        if (!droneHomeLocationSent)
            droneHomeLocationSent = sendDroneHomeLocation();
    }

    //build a landscaper
    private void buildLS() throws GameActionException{
        for (Direction dir : corners) {
            if (tryBuild(RobotType.LANDSCAPER, dir)) {
                ++numLS;
                return;
            }
        }
        for (Direction dir : Direction.cardinalDirections()){
            if (tryBuild(RobotType.LANDSCAPER, dir)) {
                ++numLS;
                return;
            }
        }
    }

    //get the latest hot goss
    private void parseBlockchain(int round) throws GameActionException {
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID) {
                switch (t.getMessage()[1]) {
                    case START_PHASE_2:
                        maxLS = 10000;
                        break;
                    case VAPORATOR_BUILT:
                        maxLS = t.getMessage()[2];
                        break;
                    case HQ_LOCATION:
                        HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        break;
                }
            }
        }
    }

    //finds the best spot for drones to call home and sends it out. It picks the spot based on the lowest pollution
    //followed by furthest away from HQ to prevent collisions with landscapers
    private boolean sendDroneHomeLocation() throws GameActionException{
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
        if (!homes.isEmpty()) {
            MapLocation ret = null;
            for (MapLocation m : homes) {
                if (ret == null)
                    ret = m;
                else if (HQ.distanceSquaredTo(m) > HQ.distanceSquaredTo(ret))
                    ret = m;
            }
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = DRONE_HOME;
            msg[2] = ret.x;
            msg[3] = ret.y;
            return sendMessage(msg, DEFCON5);
        }
        return false;
    }
}
