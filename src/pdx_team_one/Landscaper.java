package pdx_team_one;
import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class Landscaper extends Robot{

    private MapLocation target = null;
    private ArrayList<MapLocation> dumpSpots = new ArrayList<>();
    private ArrayList<MapLocation> landSpots = new ArrayList<>();
    private ArrayList<MapLocation> digSpots = new ArrayList<>();
    private ArrayList<MapLocation> dsdumpSpots = new ArrayList<>();
    boolean[][] landed;
    public boolean attackStrat;
    public boolean startDump;
    public boolean ds_secure;
    private MapLocation fc = null, ds = null;
    public int dsElevation;
    int outerWallHeight = 100;

    Landscaper(RobotController r) throws GameActionException{
        super(r);
        landed = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        for (int i = 1; i < rc.getRoundNum(); i++)
            parseBlockchain(i);
        if (attackStrat) {
            for (Direction dir : directions)
                dumpSpots.add(HQ.add(dir).add(dir).add(dir));
            for (Direction dir : directions)
                dumpSpots.add(HQ.add(dir).add(dir).add(dir.rotateRight()));
            for (Direction dir : directions)
                dumpSpots.add(HQ.add(dir).add(dir).add(dir.rotateLeft()));
            landSpots.addAll(dumpSpots);
            digSpots.add(HQ.add(Direction.EAST));
            digSpots.add(HQ.add(Direction.WEST));
        }
        else{
            for (Direction dir : corners) {
                landSpots.add(HQ.add(dir));
                digSpots.add(HQ.add(dir));
            }
            for (Direction dir : directions){
                dsdumpSpots.add(ds.add(dir));
                digSpots.add(ds.add(dir).add(dir));
                digSpots.add(ds.add(dir).add(dir.rotateRight()));
                dumpSpots.add(HQ.add(dir).add(dir));
            }
            for (Direction dir : directions){
                dumpSpots.add(HQ.add(dir).add(dir.rotateRight()));
            }

            landSpots.addAll(dumpSpots);
            for (Direction dir : directions) {
                dumpSpots.add(HQ.add(dir).add(dir).add(dir));
                landSpots.add(HQ.add(dir).add(dir).add(dir));
            }
            for (Direction dir : directions) {
                dumpSpots.add(HQ.add(dir).add(dir).add(dir.rotateRight()));
                landSpots.add(HQ.add(dir).add(dir).add(dir.rotateRight()));
            }
            for (Direction dir : directions) {
                dumpSpots.add(HQ.add(dir).add(dir).add(dir.rotateLeft()));
                landSpots.add(HQ.add(dir).add(dir).add(dir.rotateLeft()));
            }
        }
        for (int i = -4; i < 4; i++){
            digSpots.add(HQ.translate(i,4));
            digSpots.add(HQ.translate(i,-4));
            digSpots.add(HQ.translate(4,i));
            digSpots.add(HQ.translate(-4,i));
        }
        digSpots.removeAll(dumpSpots);
        digSpots.removeAll(dsdumpSpots);
        digSpots.remove(ds);
        for (RobotInfo ri : rc.senseNearbyRobots(5,rc.getTeam())){
            if (ri.type == RobotType.DESIGN_SCHOOL)
                dsElevation = rc.senseElevation(ri.location);
        }
        target = landSpots.get(0);
    }


    public void takeTurn() throws GameActionException {
        parseBlockchain(rc.getRoundNum()-1);
        if (rc.getLocation().distanceSquaredTo(HQ) < 100)
            defend();
        else
            attack();

    }

    public void parseBlockchain(int num) throws GameActionException {
        for (Transaction t : rc.getBlock(num)) {
            if (t.getMessage()[0] == TEAM_ID) {
                if (t.getMessage()[1] == HQ_LOCATION) {
                    HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    t.getMessage()[4] = hqID;
                } else if (t.getMessage()[1] == ENEMY_HQ_FOUND) {
                    enemyHQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    t.getMessage()[4] = enemyHQID;
                    //} else if (t.getMessage()[1] == HQ_TARGET_ACQUIRED) {
                    //landed[t.getMessage()[2]][t.getMessage()[3]] = true;
                } else if (t.getMessage()[1] == ATTACK) {
                    //fc_secure = true;
                    ds_secure = true;
                    attackStrat = true;
                } else if (t.getMessage()[1] == DEFENSE) {
                    fc = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    ds = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
                    attackStrat = false;
                } //else if (t.getMessage()[1] == FC_SECURE){
                //fc_secure = true;
                else if (t.getMessage()[1] == DS_SECURE) {
                    ds_secure = true;
                } else if (t.getMessage()[1] == START_PHASE_2) {
                    startDump = true;
                }
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

    public void defend()throws GameActionException {
    /*
        if (target == null) {
            for (MapLocation d : landSpots) {
                if (!landed[d.x][d.y]) {
                    target = d;
                    break;
                }
            }
            if (target != null) {
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = HQ_TARGET_ACQUIRED;
                msg[2] = target.x;
                msg[3] = target.y;
                msg[4] = rc.getID();
                msg[5] = rc.getLocation().x;
                msg[6] = rc.getLocation().y;
                System.out.println("Target acquired! " + target);
                if (!sendMessage(msg, DEFCON3)) {
                    pathTo(target);
                    target = null;
                    System.out.println("jk message didn't send");
                    return;
                }
                landed[target.x][target.y] = true;
            }
        }*/


        if (!ds_secure)
            secureDS();
        if (!ds_secure)
            return;
        MapLocation current = rc.getLocation();
        if (!landSpots.contains(current)) {
            if (attackStrat)
                target = closestLocation(landSpots.toArray(new MapLocation[0]));
            else {
                for (MapLocation d : digSpots) {
                    if (rc.canSenseLocation(d) && rc.getLocation().isAdjacentTo(d) && tryDig(rc.getLocation().directionTo(d)))
                        return;
                }
                return;
            }
            if (rc.senseFlooding(target)) {
                if (current.isAdjacentTo(target)) {
                    if (rc.getDirtCarrying() > 0)
                        tryDeposit(current.directionTo(target));
                    else {
                        MapLocation dig = null;
                        for (MapLocation m : digSpots) {
                            if (current.isAdjacentTo(m) && tryDig(current.directionTo(m)))
                                return;
                            else if (dig == null)
                                dig = m;
                            else
                                dig = closestLocation(new MapLocation[]{dig, m});
                        }
                        pathTo(dig);
                    }
                }
            }
            return;
        }
        if (attackStrat) {
            ArrayList<MapLocation> toRemove = new ArrayList<>();
            MapLocation dump = null;
            if (rc.getDirtCarrying() > 0) {
                for (MapLocation m : dumpSpots) {
                    if (!rc.canSenseLocation(m))
                        toRemove.add(m);
                    else if (!current.isAdjacentTo(m))
                        toRemove.add(m);
                    else if (dump == null || rc.senseElevation(m) < rc.senseElevation(dump))
                        dump = m;
                }
                dumpSpots.removeAll(toRemove);
                if (tryDeposit(current.directionTo(dump)))
                    return;
            }
            for (MapLocation m : digSpots) {
                if (rc.canSenseLocation(m) && current.isAdjacentTo(m) & tryDig(current.directionTo(m)))
                    return;
            }
        } else {
            if (startDump && rc.getDirtCarrying() > 0) {
                MapLocation dump = null;
                ArrayList<MapLocation> toRemove = new ArrayList<>();
                for (MapLocation d : dumpSpots) {
                    if (!rc.canSenseLocation(d))
                        toRemove.add(d);
                    else if (d.distanceSquaredTo(HQ) >= 9 && (rc.senseElevation(d) > outerWallHeight))
                        toRemove.add(d);
                    else if (!d.isAdjacentTo(current))
                        toRemove.add(d);
                    else if (dump == null)
                        dump = d;
                    else if (rc.senseElevation(d) < rc.senseElevation(dump))
                        dump = d;
                }
                dumpSpots.removeAll(toRemove);
                if (tryDeposit(current.directionTo(dump)))
                    return;
            }
            for (MapLocation m : digSpots) {
                if (rc.canSenseLocation(m) && rc.getLocation().isAdjacentTo(m) && tryDig(rc.getLocation().directionTo(m)))
                    return;
            }
        }
    }

    public boolean tryDeposit(Direction dir) throws GameActionException{
        RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(dir));
        if (r != null && r.type == RobotType.MINER) {
            return false;
        }
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

    public boolean tryDig(Direction dir) throws GameActionException{
        RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(dir));
        if (r != null && r.type == RobotType.MINER)
            return false;
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

    private void secureDS()throws GameActionException{
        if (!rc.isReady())
            return;
         if (rc.getDirtCarrying() > 0) {
             for (MapLocation m : dsdumpSpots) {
                 if (rc.canSenseLocation(m) && rc.senseElevation(m) - dsElevation < 3) {
                     if (rc.getLocation().isAdjacentTo(m) && tryDeposit(rc.getLocation().directionTo(m)))
                         return;
                 }
             }
         }
        else{
            for (MapLocation m : digSpots){
                if (rc.canSenseLocation(m) && rc.getLocation().isAdjacentTo(m) && tryDig(rc.getLocation().directionTo(m)))
                    return;
            }
        }
        ds_secure = true;
    }
}
