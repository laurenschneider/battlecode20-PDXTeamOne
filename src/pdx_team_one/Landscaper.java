package pdx_team_one;
import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

public class Landscaper extends Robot {

    private HashSet<MapLocation> dumpSpots = new HashSet<>();
    private HashSet<MapLocation> landSpots = new HashSet<>();
    private HashSet<MapLocation> innerSpots;
    private HashSet<MapLocation> digSpots = new HashSet<>();
    private HashSet<MapLocation> dsdumpSpots = new HashSet<>();
    private HashSet<MapLocation> firstDump = new HashSet<>();
    public boolean startDump;
    public boolean ds_secure;
    private MapLocation fc = null, ds = null;
    public int dsElevation;
    int outerWallHeight = 100;
    int[] waterLevel = new int[]{0,256,464,677,930,1210,50000};

    Landscaper(RobotController r) throws GameActionException {
        super(r);
        for (RobotInfo ri : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (ri.type == RobotType.DESIGN_SCHOOL) {
                dsElevation = rc.senseElevation(ri.location);
                ds = ri.location;
            }
            if (ri.type == RobotType.FULFILLMENT_CENTER)
                fc = ri.location;
        }
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        for (Direction dir : corners) {
            digSpots.add(HQ.add(dir));
            firstDump.add(HQ.add(dir.rotateRight()));
        }
        for (Direction dir : directions) {
            if (rc.onTheMap(ds.add(dir)) && !fc.equals(ds.add(dir))) {
                dsdumpSpots.add(ds.add(dir));
            }
            if (fc.isAdjacentTo(ds)) {
                if (rc.onTheMap(fc.add(dir)) && !ds.equals(fc.add(dir)))
                    dsdumpSpots.add(fc.add(dir));
            }
            if (rc.onTheMap(ds.add(dir).add(dir)))
                digSpots.add(ds.add(dir).add(dir));
            if (rc.onTheMap(ds.add(dir).add(dir.rotateRight())))
                digSpots.add(ds.add(dir).add(dir.rotateRight()));
            if (rc.onTheMap(HQ.add(dir).add(dir))) {
                firstDump.add(HQ.add(dir).add(dir));
            }
            if (rc.onTheMap(HQ.add(dir).add(dir.rotateRight())))
                firstDump.add(HQ.add(dir).add(dir.rotateRight()));
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
        for (Direction dir : directions) {
            if (rc.onTheMap(fc.add(dir)) && !fc.add(dir).equals(ds)) {
                digSpots.remove(fc.add(dir));
            }
        }

        innerSpots = initInnerSpots();
        landSpots.addAll(innerSpots);
        dumpSpots.addAll(initWallSpots());
        dumpSpots.addAll(initOuterSpots());
        landSpots.addAll(dumpSpots);


        digSpots.remove(fc);
        digSpots.remove(ds);
        digSpots.removeAll(dumpSpots);
        digSpots.removeAll(dsdumpSpots);

        dumpSpots.remove(ds);
        dumpSpots.remove(fc);
        landSpots.remove(ds);
        landSpots.remove(fc);
    }


    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
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
                } else if (t.getMessage()[1] == DEFENSE) {
                    if (fc == null)
                        fc = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                    if (ds == null)
                        ds = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
                } else if (t.getMessage()[1] == DS_SECURE) {
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
            //System.out.println("I'm not in position yet");
            if (!startDump && !innerSpots.isEmpty()) {
                ArrayList<MapLocation> toRemove = new ArrayList<>();
                MapLocation target = null;
                for (MapLocation m : innerSpots) {
                    if (rc.canSenseLocation(m)) {
                        RobotInfo r = rc.senseRobotAtLocation(m);
                        if (r != null && r.type == RobotType.LANDSCAPER)
                            toRemove.add(m);
                        else if (target == null)
                            target = m;
                        else if (rc.senseElevation(m) < rc.senseElevation(target))
                            target = m;
                    }
                }
                innerSpots.removeAll(toRemove);

                if(target != null)
                    pathTo(target);
                else if (!innerSpots.isEmpty())
                    pathTo(closestLocation(innerSpots.toArray(new MapLocation[0])));

            }/*
            if (rc.isReady() && Clock.getBytecodesLeft() > 500) {
                for (MapLocation d : digSpots) {
                    if (rc.canSenseLocation(d) && rc.getLocation().isAdjacentTo(d) && tryDig(rc.getLocation().directionTo(d)))
                        return;
                }
            }
            */
        } else if (startDump && rc.getDirtCarrying() > 0) {
          //  System.out.println("Let's start dumping");
            MapLocation dump = null;
            ArrayList<MapLocation> toRemove = new ArrayList<>();
            for (MapLocation d : dumpSpots) {
                if (rc.canSenseLocation(d) && current.isAdjacentTo(d)) {
                    if (d.distanceSquaredTo(HQ) >= 9 && (rc.senseElevation(d) > outerWallHeight))
                        toRemove.add(d);
                    else if (rc.senseElevation(d) < -1000)
                        toRemove.add(d);
                    else if (dump == null)
                        dump = d;
                    else if (rc.senseElevation(d) < rc.senseElevation(dump))
                        dump = d;
                }
            }
            dumpSpots.removeAll(toRemove);
            if (dump != null && tryDeposit(current.directionTo(dump)))
                return;
        } else if (!startDump && !innerSpots.contains(rc.getLocation())) {
           // System.out.println("Not in an inner spot, gotta get to one");
            ArrayList<MapLocation> toRemove = new ArrayList<>();
            for (MapLocation m : innerSpots) {
                if (rc.canSenseLocation(m)) {
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
        } else if (!startDump && rc.getDirtCarrying() > 0) {
            //MapLocation[] soups = rc.senseNearbySoup();
          //  System.out.println("Let's start the initial wall");
            MapLocation dump = null;
            ArrayList<MapLocation> toRemove = new ArrayList<>();
            for (MapLocation d : firstDump) {
                if (!rc.canSenseLocation(d))
                    toRemove.add(d);
                else if (!d.isAdjacentTo(current))
                    toRemove.add(d);
                else if (rc.senseRobotAtLocation(d) != null && (rc.senseRobotAtLocation(d).type == RobotType.VAPORATOR || rc.senseRobotAtLocation(d).type == RobotType.NET_GUN))
                    toRemove.add(d);
                /*
                else if (soups.length > 0 && (rc.senseElevation(d) - rc.senseElevation(HQ) >= 3))
                    continue;*/
                else if (d.isAdjacentTo(HQ) && rc.senseElevation(d) < rc.senseElevation (HQ.add(HQ.directionTo(d)).add(HQ.directionTo(d)))){
                    Direction dir = HQ.directionTo(current);
                    if (rc.senseElevation(current.add(dir)) < 6)
                        continue;
                    else if (rc.senseElevation(current.add(dir.rotateRight())) < 6)
                        continue;
                    else if (rc.senseElevation(current.add(dir.rotateRight().rotateRight())) < 6)
                        continue;
                    else if (rc.senseElevation(current.add(dir.rotateLeft())) < 6)
                        continue;
                    else if (rc.senseElevation(current.add(dir.rotateLeft().rotateLeft())) < 6)
                        continue;
                    tryDeposit(current.directionTo(d));
                    return;
                }
                else if (dump == null)
                    dump = d;
                else if (rc.senseElevation(d) < rc.senseElevation(dump))
                    dump = d;
            }
            firstDump.removeAll(toRemove);
            if (dump != null && tryDeposit(current.directionTo(dump)))
                return;
        }
        if (rc.isReady()) {
            boolean floodDanger = false;
            int i = 0;
            while (waterLevel[i] < rc.getRoundNum())
                i++;
            if (rc.senseElevation(current) <= i) {
                for (Direction dir : directions) {
                    if (rc.senseFlooding(current.add(dir))) {
                        floodDanger = true;
                        break;
                    }
                }
            }
            if (floodDanger) {
                for (Direction dir : directions) {
                    if (rc.senseElevation(rc.getLocation().add(dir)) > i)
                        if (tryDig(dir))
                            return;
                }
            } else {
                for (MapLocation m : digSpots) {
                    if (rc.canSenseLocation(m) && rc.getLocation().isAdjacentTo(m) && tryDig(rc.getLocation().directionTo(m)))
                        return;
                }
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
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

    private void secureDS()throws GameActionException{
       // System.out.println("Securing DS");
        /*
        if (rc.senseElevation(ds) >= 6) {
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = DS_SECURE;
            sendMessage(msg, DEFCON5);
            ds_secure = true;
            return;
        }*/
        if (!rc.isReady())
            return;
        ArrayList<MapLocation> toRemove = new ArrayList<>();
         if (rc.getDirtCarrying() > 0) {
        //     System.out.println("Gonna try and dump");
             for (int i =0; waterLevel[i] < rc.senseElevation(rc.getLocation()); i++){
             }
             for (MapLocation m : dsdumpSpots) {
             //    System.out.println("Checking dsdumpspot " + m);
                 if (rc.canSenseLocation(m) && rc.senseElevation(m) - dsElevation < 3) {
                     if (rc.getLocation().isAdjacentTo(m) && tryDeposit(rc.getLocation().directionTo(m)))
                         return;
                 }
                 else if (rc.canSenseLocation(m))
                     toRemove.add(m);
             }
         }
        else{
         //   System.out.println("it's digging time");
            for (Direction dir : directions){
                if(digSpots.contains(rc.getLocation().add(dir)))
                    if (tryDig(dir))
                        return;
            }
        }
        dsdumpSpots.removeAll(toRemove);
        if(!dsdumpSpots.isEmpty()) {
            if (rc.getDirtCarrying() == 25) {
                MapLocation m = closestLocation(dsdumpSpots.toArray(new MapLocation[0]));
                Direction dir = rc.getLocation().directionTo(m);
                if (tryMove(dir))
                    return;
                else if (tryMove(dir.rotateLeft()))
                    return;
                else if (tryMove(dir.rotateRight()))
                    return;
                /*
                for (Direction dir : Direction.cardinalDirections()){
                    for (MapLocation m : dsdumpSpots){
                        if (ds.add(dir).isAdjacentTo(m))
                            if(tryMove(rc.getLocation().directionTo(ds.add(dir))))
                                return;
                    }
                }*/
            }
            for (Direction dir : directions) {
                if (digSpots.contains(rc.getLocation().add(dir)))
                    if (tryDig(dir))
                        return;
            }
            pathTo(closestLocation(digSpots.toArray(new MapLocation[0])));
        }
        else {
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = DS_SECURE;
            ds_secure = true;
            sendMessage(msg, DEFCON5);
        }
    }
}
