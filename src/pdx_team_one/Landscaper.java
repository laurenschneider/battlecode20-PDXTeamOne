package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.Map;

public class Landscaper extends Robot{

    private boolean [] nearHQ = new boolean[8];
    private MapLocation target = null;
    private ArrayList<MapLocation> dumpSpots = new ArrayList<>();

    Landscaper(RobotController r) throws GameActionException{
        super(r);
        HQ = new MapLocation(0,0);
        parseBlockchain();
//        map = new int[rc.getMapWidth()][rc.getMapHeight()];
//        updateLocalMap(false);
        for (Direction dir: directions)
            dumpSpots.add(HQ.add(dir));
    }


    public void takeTurn() throws GameActionException {
        parseBlockchain(rc.getRoundNum()-1);
        if (rc.getLocation().distanceSquaredTo(HQ) < 100)
            defend();
        else
            attack();

    }

    public int parseBlockchain() throws GameActionException {
        int res = 0;
        for (int i = 1; i < rc.getRoundNum(); i++){
            for (Transaction t : rc.getBlock(i)){
                if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_LOCATION){
                    HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    t.getMessage()[4] = hqID;
                    res = 1;
                }
                else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == ENEMY_HQ_FOUND){
                    enemyHQ = new MapLocation(t.getMessage()[2],t.getMessage()[3]);
                    t.getMessage()[4] = enemyHQID;
                    res = 2;
                }
                else if (t.getMessage()[0] == TEAM_ID && t.getMessage()[1] == HQ_TARGET_ACQUIRED){
                    nearHQ[t.getMessage()[2]] = true;
                    res = 3;
                }
            }
        }
        return res;
    }

    public void parseBlockchain(int num) throws GameActionException {
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


    public void attack()throws GameActionException {
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

    public void defend()throws GameActionException{
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
            msg[3] = rc.getID();
            System.out.println("Target acquired! " + dir);
            if (!sendMessage(msg, DEFCON3)) {
                pathTo(HQ);
                target = null;
                System.out.println("jk message didn't send");
                return;
            }
        }


        MapLocation current = rc.getLocation();

        if (current.equals(target) && rc.getDirtCarrying() > 0){
            //System.out.println("Dumping dirt");
                MapLocation dump = current.add(current.directionTo(HQ).rotateLeft());
                for (MapLocation d : dumpSpots) {
                    if (rc.senseElevation(d) < rc.senseElevation(dump) && current.isAdjacentTo(d)) {
                        dump = d;
                        }
                    }
            tryDeposit(current.directionTo(dump));
        }
        else if (rc.getLocation().equals(target)) {
            //System.out.println("Digging dirt");
            if (tryDig(current.directionTo(HQ).opposite()))
                return;
            if (tryDig(current.directionTo(HQ).opposite().rotateRight()))
                return;
            tryDig(current.directionTo(HQ).opposite().rotateLeft());
        }
        else {
            //System.out.println("Trying to get to " + target);
            pathTo(target);
        }

    }

    public boolean tryDeposit(Direction dir) throws GameActionException{
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

    public boolean tryDig(Direction dir) throws GameActionException{
        if (rc.isLocationOccupied(rc.getLocation().add(dir)))
            return false;
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }
        return false;
    }
}
