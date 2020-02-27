package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.Deque;

public class Landscaper extends Robot{

    private boolean [] nearHQ = new boolean[8];
    private MapLocation target = null;
    private ArrayList<MapLocation> dumpSpots = new ArrayList<>();
    private ArrayList<MapLocation> digSpots = new ArrayList<>();
    private MapLocation[] outerWall = new MapLocation[24];
    private boolean outerWallLandscaper = false;
    private int outerWallHeight = -100;
    private MapLocation start = null;
    private int curr = -1;

    Landscaper(RobotController r) throws GameActionException{
        super(r);
        HQ = new MapLocation(0,0);
        parseBlockchain();

        int i = 0;
        for (Direction dir: directions) {
            dumpSpots.add(HQ.add(dir));
            outerWall[i] = HQ.add(dir).add(dir).add(dir.rotateLeft());
            outerWall[i+1] = HQ.add(dir).add(dir).add(dir);
            outerWall[i+2] = HQ.add(dir).add(dir).add(dir.rotateRight());
            i += 3;
        }
        for (Direction dir: Direction.cardinalDirections())
            digSpots.add(HQ.add(dir).add(dir));
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
        if (!outerWallLandscaper && target == null) {
            int dir = -1;
            for (int i = 0; i < 8; i++) {
                if (!nearHQ[i]) {
                    target = HQ.add(directions[i]);
                    dir = i;
                    break;
                }
            }
            if (target == null)
                outerWallLandscaper = true;
            else {
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
        }

        if (outerWallLandscaper) {
            if (start == null) {
                for (curr = 0; curr < 24; curr++) {
                    if (outerWall[curr].equals(rc.getLocation())) {
                        start = rc.getLocation();
                        outerWallHeight = rc.senseElevation(rc.getLocation())+3;
                        break;
                    }
                }
            }
            if (start == null) {
                pathTo(HQ);
                return;
            }
            int next = curr+1;
            if (next == 24)
                next = 0;
            if (outerWall[next].equals(start))
                outerWallHeight = rc.senseElevation(rc.getLocation())+3;
            if (rc.senseElevation(outerWall[next]) < outerWallHeight) {
                if (rc.getDirtCarrying() > 0 && tryDeposit(rc.getLocation().directionTo(outerWall[next])))
                    return;
            }
            else if (tryMove(rc.getLocation().directionTo(outerWall[next]))){
                curr++;
                if (curr == 24)
                    curr = 0;
                return;
            }
            else if (rc.senseElevation(outerWall[next]) - rc.senseElevation(rc.getLocation()) > 3){
                outerWallHeight = rc.senseElevation(outerWall[next]);
                if (rc.getDirtCarrying() > 0 && tryDeposit(Direction.CENTER))
                    return;
            }
            else if (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit){
                int prev = curr - 1;
                if (prev == -1)
                    prev = 23;
                if(tryDeposit(rc.getLocation().directionTo(outerWall[prev])))
                    return;
                else if (tryDeposit(Direction.CENTER))
                    return;
                else if (tryDeposit(rc.getLocation().directionTo(outerWall[next])))
                    return;
            }
            if (tryDig(rc.getLocation().directionTo(HQ).opposite()))
                return;
            else if (tryDig(rc.getLocation().directionTo(HQ).opposite().rotateLeft()))
                return;
            else if (tryDig(rc.getLocation().directionTo(HQ).opposite().rotateRight()))
                return;
            return;
        }


        MapLocation current = rc.getLocation();

        if (current.equals(target) && rc.getDirtCarrying() > 0) {
            //System.out.println("Dumping dirt");
            MapLocation dump = current.add(current.directionTo(HQ).rotateLeft());
            for (MapLocation d : dumpSpots) {
                if (rc.senseElevation(d) < rc.senseElevation(dump) && current.isAdjacentTo(d))
                    dump = d;
            }
            tryDeposit(current.directionTo(dump));
        }
        else if (current.equals(target)) {
            //System.out.println("Digging dirt");
            for (MapLocation dig : digSpots) {
                if (tryDig(current.directionTo(dig)))
                    return;
            }
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
