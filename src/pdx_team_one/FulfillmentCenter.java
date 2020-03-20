package pdx_team_one;
import battlecode.common.*;

//the Fulfillment Center builds Drones
public class FulfillmentCenter extends Building {

    private int num = 0;
    private int maxDrones = 1;

    FulfillmentCenter(RobotController r) throws GameActionException {
        super(r);
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
    }

    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        //determines whether or not to build drones
        if (rc.getTeamSoup() >= RobotType.REFINERY.cost + 5 && num < maxDrones)
            buildDrones();
        else if (num < 4 && rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5)
            buildDrones();
    }

    //build the drone
    private void buildDrones() throws GameActionException {
        for (Direction dir : corners) {
            if (tryBuild(RobotType.DELIVERY_DRONE, dir))
                num++;
        }
        for (Direction dir : Direction.cardinalDirections()) {
            if (tryBuild(RobotType.DELIVERY_DRONE, dir))
                num++;
        }
    }

    //get the latest hot goss
    private void parseBlockchain(int round) throws GameActionException {
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == START_PHASE_2)
                maxDrones = 6;
        }
    }
}
