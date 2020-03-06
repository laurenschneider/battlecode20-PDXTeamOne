package pdx_team_one;
import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class Landscaper extends Robot{

    private MapLocation target;
    private ArrayList<MapLocation> dumpSpots = new ArrayList<>();
    private ArrayList<MapLocation> landSpots = new ArrayList<>();
    private ArrayList<MapLocation> innerSpots = new ArrayList<>();
    private ArrayList<MapLocation> digSpots = new ArrayList<>();
    private ArrayList<MapLocation> dsdumpSpots = new ArrayList<>();
    private ArrayList<MapLocation> firstDump = new ArrayList<>();
    public boolean attackStrat;
    public boolean startDump;
    public boolean ds_secure;
    private MapLocation fc = null, ds = null;
    public int dsElevation;
    int outerWallHeight = 100;

    Landscaper(RobotController r) throws GameActionException{
        super(r);
        for (RobotInfo ri : rc.senseNearbyRobots(-1,rc.getTeam())){
            if (ri.type == RobotType.DESIGN_SCHOOL) {
                dsElevation = rc.senseElevation(ri.location);
                ds = ri.location;
            }
        }
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
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
                innerSpots.add(HQ.add(dir));
                landSpots.add(HQ.add(dir));
                digSpots.add(HQ.add(dir));
                firstDump.add(HQ.add(dir.rotateRight()));
            }
            for (Direction dir : directions){
                if(rc.onTheMap(ds.add(dir)))
                    dsdumpSpots.add(ds.add(dir));
                if(rc.onTheMap(ds.add(dir).add(dir)))
                    digSpots.add(ds.add(dir).add(dir));
                if(rc.onTheMap(ds.add(dir).add(dir.rotateRight())))
                    digSpots.add(ds.add(dir).add(dir.rotateRight()));
                if (rc.onTheMap(HQ.add(dir).add(dir))) {
                    dumpSpots.add(HQ.add(dir).add(dir));
                    firstDump.add(HQ.add(dir).add(dir));
                }
                if (rc.onTheMap(HQ.add(dir).add(dir.rotateRight())))
                    firstDump.add(HQ.add(dir).add(dir.rotateRight()));
            }
            for (Direction dir : directions){
                if (rc.onTheMap(HQ.add(dir).add(dir.rotateRight())))
                    dumpSpots.add(HQ.add(dir).add(dir.rotateRight()));
            }

            landSpots.addAll(dumpSpots);
            for (Direction dir : directions) {
                MapLocation m = HQ.add(dir).add(dir).add(dir);
                if (rc.onTheMap(m)) {
                    dumpSpots.add(m);
                    landSpots.add(m);
                }
                m = HQ.add(dir).add(dir).add(dir.rotateRight());
                if (rc.onTheMap(m)) {
                    dumpSpots.add(m);
                    landSpots.add(m);
                }
                m = HQ.add(dir).add(dir).add(dir.rotateLeft());
                if (rc.onTheMap(m)) {
                    dumpSpots.add(m);
                    landSpots.add(m);
                }
            }
        }
        for (int i = -4; i < 4; i++) {
            if (rc.onTheMap(HQ.translate(i, 4)))
                digSpots.add(HQ.translate(i, 4));
            if (rc.onTheMap(HQ.translate(i, -4)))
                digSpots.add(HQ.translate(i, -4));
            if (rc.onTheMap(HQ.translate(4, i)))
                digSpots.add(HQ.translate(4, i));
            if (rc.onTheMap(HQ.translate(-4, i)))
                digSpots.add(HQ.translate(-4, i));
        }
        digSpots.removeAll(dumpSpots);
        digSpots.removeAll(dsdumpSpots);
        digSpots.remove(ds);
        target = landSpots.get(0);
    }


    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        System.out.println("DS: " + ds);
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
        if (!ds_secure)
            secureDS();
        if (!ds_secure)
            return;
        MapLocation current = rc.getLocation();
        if (!landSpots.contains(current)) {
            if (attackStrat) {
                target = closestLocation(landSpots.toArray(new MapLocation[0]));
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
            } else if (!startDump && !innerSpots.isEmpty()) {
                ArrayList<MapLocation> toRemove = new ArrayList<>();
                for (MapLocation m : innerSpots) {
                    if (rc.canSenseLocation(m)) {
                        RobotInfo r = rc.senseRobotAtLocation(m);
                        if (r != null && r.type == RobotType.LANDSCAPER)
                            toRemove.add(m);
                    }
                }
                innerSpots.removeAll(toRemove);
                if (!innerSpots.isEmpty())
                    pathTo(closestLocation(innerSpots.toArray(new MapLocation[0])));

            }
            for (MapLocation d : digSpots) {
                if (rc.canSenseLocation(d) && rc.getLocation().isAdjacentTo(d) && tryDig(rc.getLocation().directionTo(d)))
                    return;
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
            else if (!startDump && !innerSpots.contains(rc.getLocation())){
                ArrayList<MapLocation> toRemove = new ArrayList<>();
                for (MapLocation m : innerSpots){
                    if (rc.canSenseLocation(m)){
                        RobotInfo r = rc.senseRobotAtLocation(m);
                        if (r != null && r.type == RobotType.LANDSCAPER)
                            toRemove.add(m);
                    }
                }
                innerSpots.removeAll(toRemove);
                if (!innerSpots.isEmpty()) {
                    pathTo(closestLocation(innerSpots.toArray(new MapLocation[0])));
                    return;
                }
            }
            else if (!startDump && rc.getDirtCarrying() > 0){
                MapLocation dump = null;
                ArrayList<MapLocation> toRemove = new ArrayList<>();
                for (MapLocation d : firstDump) {
                    if (!rc.canSenseLocation(d))
                        toRemove.add(d);
                    else if (!d.isAdjacentTo(current))
                        toRemove.add(d);
                    else if (rc.senseRobotAtLocation(d) != null && (rc.senseRobotAtLocation(d).type == RobotType.VAPORATOR  ||rc.senseRobotAtLocation(d).type == RobotType.NET_GUN))
                        toRemove.add(d);
                    else if (rc.senseElevation(d) - rc.senseElevation(HQ) >= 3)
                        toRemove.add(d);
                    else if (dump == null)
                        dump = d;
                    else if (rc.senseElevation(d) < rc.senseElevation(dump))
                        dump = d;
                }
                firstDump.removeAll(toRemove);
                if (dump != null && tryDeposit(current.directionTo(dump)))
                    return;
            }
            for (MapLocation m : digSpots) {
                if (rc.canSenseLocation(m) && rc.getLocation().isAdjacentTo(m) && tryDig(rc.getLocation().directionTo(m)))
                    return;
            }
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
        /*RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(dir));
        if (r != null && r.type == RobotType.MINER)
            return false;*/
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

    private void secureDS()throws GameActionException{
        if (rc.senseElevation(ds) >= 6) {
            ds_secure = true;
            return;
        }
        if (!rc.isReady())
            return;
        ArrayList<MapLocation> toRemove = new ArrayList<>();
         if (rc.getDirtCarrying() > 0) {
             System.out.println("Gonna try and dump");
             for (MapLocation m : dsdumpSpots) {
                 System.out.println("Checking dsdumpspot " + m);
                 if (rc.canSenseLocation(m) && rc.senseElevation(m) - dsElevation < 3) {
                     if (rc.getLocation().isAdjacentTo(m) && tryDeposit(rc.getLocation().directionTo(m)))
                         return;
                 }
                 else
                     toRemove.add(m);
             }
         }
        else{
            System.out.println("it's digging time");
            for (MapLocation m : digSpots){
                System.out.println("Checking dsdigspot " + m);
                if (rc.canSenseLocation(m) && rc.getLocation().isAdjacentTo(m) && tryDig(rc.getLocation().directionTo(m)))
                    return;
            }
        }
        dsdumpSpots.removeAll(toRemove);
        if(!dsdumpSpots.isEmpty()) {
            if (rc.getDirtCarrying() > 0)
                pathTo(closestLocation(dsdumpSpots.toArray(new MapLocation[0])));
            else
                pathTo(closestLocation(digSpots.toArray(new MapLocation[0])));
        }
        else
            ds_secure = true;
        //ds_secure = true;
    }
}
