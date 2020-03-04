package pdx_team_one;
import battlecode.common.*;

public class DesignSchool extends Robot{
    int numLS = 0;
    int maxLS = 4;
    private boolean secure;
    DesignSchool(RobotController r) throws GameActionException{
        super(r);
        for (int i = 1; i < rc.getRoundNum(); i++)
            parseBlockchain(i);
    }

    public void takeTurn() throws GameActionException{
        parseBlockchain(rc.getRoundNum()-1);
        if (!secure)
            secure = checkSecure();
        if (numLS < maxLS && rc.getTeamSoup() >= RobotType.REFINERY.cost)
            buildLS();
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
                if (t.getMessage()[1] == ATTACK)
                    maxLS = 40;
                else if (t.getMessage()[1] == DEFENSE)
                    maxLS = 4;
                else if (t.getMessage()[1] == START_PHASE_2)
                    maxLS = 1000000;
            }
        }
        return res;
    }

    public boolean checkSecure() throws GameActionException{
        for (Direction dir : directions){
            if (rc.canSenseLocation(rc.getLocation().add(dir)) &&rc.senseElevation(rc.getLocation().add(dir)) - rc.senseElevation(rc.getLocation()) < 3)
                return false;
        }
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = DS_SECURE;
        return sendMessage(msg,DEFCON5);
    }
}
