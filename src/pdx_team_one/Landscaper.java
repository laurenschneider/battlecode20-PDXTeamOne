package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;

public class Landscaper extends Robot{

    private boolean [] nearHQ = new boolean[8];
    private MapLocation target = null;
    private ArrayList<MapLocation> landingSpots = new ArrayList<>();

    Landscaper(RobotController r) throws GameActionException{
        super(r);
        parseBlockchain();
        map = new int[rc.getMapWidth()][rc.getMapHeight()];
        updateLocalMap(false);
    }


    public void takeTurn() throws GameActionException {
        parseBlockchain(rc.getRoundNum()-1);
        if (rc.getLocation().distanceSquaredTo(HQ) < 100)
            defend();
        else
            attack();

    }

    private void parseBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for (Transaction t : rc.getBlock(i)){
                if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_LOCATION){
                    HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    t.getMessage()[4] = hqID;
                }
                else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == ENEMY_HQ_FOUND){
                    enemyHQ = new MapLocation(t.getMessage()[2],t.getMessage()[3]);
                    t.getMessage()[4] = enemyHQID;
                }
                else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_TARGET_ACQUIRED){
                    nearHQ[t.getMessage()[2]] = true;
                }
            }
        }
    }

    private void parseBlockchain(int num) throws GameActionException {
        for (Transaction t : rc.getBlock(num)) {
            if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_LOCATION) {
                HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                t.getMessage()[4] = hqID;
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == ENEMY_HQ_FOUND) {
                enemyHQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                t.getMessage()[4] = enemyHQID;
            } else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_TARGET_ACQUIRED) {
                nearHQ[t.getMessage()[2]] = true;
            }
        }
    }


    private void attack()throws GameActionException {
        MapLocation current = rc.getLocation();

        if (current.isAdjacentTo(enemyHQ) && rc.getDirtCarrying() > 0)
            tryDeposit(current.directionTo(enemyHQ));
        else if (current.isAdjacentTo(enemyHQ)){
            for (Direction dir: directions)
                tryDig(dir);
        }
        else{
            pathTo(enemyHQ);
        }
    }

    private void defend()throws GameActionException{
        if (target == null) {
            int dir = 0;
            for (int i = 0; i < 8; i++) {
                if (!nearHQ[i]) {
                    target = HQ.add(directions[i]);
                    dir = i;
                    break;
                }
            }

            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = HQ_TARGET_ACQUIRED;
            msg[2] = dir;
            System.out.println("Target acquired! " + dir);
            if (!sendMessage(msg, DEFCON3)) {
                pathTo(HQ);
                target = null;
                System.out.println("jk message didn't send");
                return;
            }
            landingSpots.add(target.subtract(target.directionTo(HQ)));
            landingSpots.add(target.subtract(target.directionTo(HQ).rotateLeft()));
            landingSpots.add(target.subtract(target.directionTo(HQ).rotateRight()));

        }


        MapLocation current = rc.getLocation();

        if (!landingSpots.contains(current)){
            if (rc.canSenseLocation(landingSpots.get(0)) && !rc.isLocationOccupied(landingSpots.get(0)))
                pathTo(landingSpots.get(0));
            else if (rc.canSenseLocation(landingSpots.get(1)) && !rc.isLocationOccupied(landingSpots.get(1)))
                pathTo(landingSpots.get(1));
            else if (rc.canSenseLocation(landingSpots.get(2)) && !rc.isLocationOccupied(landingSpots.get(2)))
                pathTo(landingSpots.get(2));
            else
                pathTo(HQ);
            return;
        }

        if (current.isAdjacentTo(target) && rc.getDirtCarrying() > 0 && rc.senseNearbyRobots(target,0,rc.getTeam()).length == 0) {
            int low = rc.senseElevation(target);
            for (int i = 0; i < 8; i ++){
                MapLocation check = rc.getLocation().add(directions[i]);
                if (rc.senseElevation(check) < low && rc.getLocation().isAdjacentTo(check) && HQ.isAdjacentTo(check)){
                    if (rc.senseNearbyRobots(check,0,rc.getTeam()).length == 0) {
                        target = check;
                        low = rc.senseElevation(target);
                    }
                }
            }
            tryDeposit(current.directionTo(target));
        }
        else if (current.isAdjacentTo(target)) {
            if (tryDig(current.directionTo(target).opposite()))
                return;
            if (tryDig(current.directionTo(target).opposite().rotateRight()))
                return;
            tryDig(current.directionTo(target).opposite().rotateLeft());
        }

    }

    private boolean tryDeposit(Direction dir) throws GameActionException{
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

    private boolean tryDig(Direction dir) throws GameActionException{
        if (rc.isLocationOccupied(rc.getLocation().add(dir)))
            return false;
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }
        return false;
    }
}
