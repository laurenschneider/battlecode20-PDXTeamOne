package pdx_team_one;
import battlecode.common.*;
import java.util.HashSet;

//the Landscaper digs dirt and builds walls
public class Landscaper extends Unit {

    private HashSet<MapLocation> dumpSpots = new HashSet<>();
    private HashSet<MapLocation> landSpots = new HashSet<>();
    private HashSet<MapLocation> innerSpots;
    private HashSet<MapLocation> outerSpots;
    private HashSet<MapLocation> digSpots = new HashSet<>();
    private HashSet<MapLocation> dsDumpSpots = new HashSet<>();
    private HashSet<MapLocation> firstDump = new HashSet<>();
    private boolean startDump;
    private boolean ds_secure;
    private MapLocation fc = null, ds = null;
    private int dsElevation;
    private int[] waterLevel = new int[]{0,256,464,677,930,1210,50000};

    //initializes all the landing and dumping spots
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
        for (Direction dir : directions) {
            if (rc.onTheMap(ds.add(dir)) && !fc.equals(ds.add(dir))) {
                dsDumpSpots.add(ds.add(dir));
            }
            if (fc.isAdjacentTo(ds)) {
                if (rc.onTheMap(fc.add(dir)) && !ds.equals(fc.add(dir)))
                    dsDumpSpots.add(fc.add(dir));
            }
            if (rc.onTheMap(ds.add(dir).add(dir)))
                digSpots.add(ds.add(dir).add(dir));
            if (rc.onTheMap(ds.add(dir).add(dir.rotateRight())))
                digSpots.add(ds.add(dir).add(dir.rotateRight()));
        }

        innerSpots = initInnerSpots();
        landSpots.addAll(innerSpots);
        digSpots.addAll(innerSpots);
        dumpSpots.addAll(initWallSpots());

        outerSpots = initOuterSpots();
        for (MapLocation m : outerSpots){
            digSpots.add(m.add(HQ.directionTo(m)));
            dumpSpots.add(m);
        }

        landSpots.addAll(dumpSpots);

        digSpots.remove(fc);
        digSpots.remove(ds);
        digSpots.removeAll(dumpSpots);
        digSpots.removeAll(dsDumpSpots);

        dumpSpots.remove(ds);
        dumpSpots.remove(fc);
        landSpots.remove(ds);
        landSpots.remove(fc);

        if(constriction)
            return;
        boolean left,right,upper,lower;
        left = (HQ.x < 4);
        right = (rc.getMapWidth() - HQ.x <= 4);
        upper = (rc.getMapHeight() - HQ.y <= 4);
        lower = (HQ.y < 4);

        firstDump.addAll(initWallSpots());
        for (Direction dir : Direction.cardinalDirections()){
            firstDump.add(HQ.add(dir));
        }

        if (upper) {
            firstDump.remove(HQ.add(Direction.NORTH));
            if (!right)
                firstDump.add(HQ.translate(1,2));
            if (!left)
                firstDump.add(HQ.translate(-1,2));
        }
        if (right) {
            firstDump.remove(HQ.add(Direction.EAST));
            if (!upper)
                firstDump.add(HQ.translate(2, 1));
            if (!lower)
                firstDump.add(HQ.translate(2, -1));
        }
        if (lower) {
            firstDump.remove(HQ.add(Direction.SOUTH));
            if (!right)
                firstDump.add(HQ.translate(1,-2));
            if (!left)
                firstDump.add(HQ.translate(-1,-2));
        }
        if (left) {
            firstDump.remove(HQ.add(Direction.WEST));
            if (!upper)
                firstDump.add(HQ.translate(-2,1));
            if (!lower)
                firstDump.add(HQ.translate(-2,-1));
        }
    }


    public void takeTurn() throws GameActionException {
        for (; lastBlockRead < rc.getRoundNum(); lastBlockRead++)
            parseBlockchain(lastBlockRead);
        defend();
    }

    //get the latest hot goss
    public void parseBlockchain(int num) throws GameActionException {
        for (Transaction t : rc.getBlock(num)) {
            if (t.getMessage()[0] == TEAM_ID) {
                switch (t.getMessage()[1]) {
                    case HQ_LOCATION:
                        HQ = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        hqElevation = t.getMessage()[5];
                        break;
                    case DEFENSE:
                        if (fc == null)
                            fc = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
                        if (ds == null)
                            ds = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
                        break;
                    case DS_SECURE:
                        ds_secure = true;
                        break;
                    case START_PHASE_2:
                        startDump = true;
                        break;
                }
            }
        }
    }

    //the main driver function
    public void defend() throws GameActionException {
        MapLocation current = rc.getLocation();

        //if we're holding dirt and happen to be next to an enemy netgun, then dump on it
        if (rc.getDirtCarrying() > 0){
            for (RobotInfo r : rc.senseNearbyRobots(-1,rc.getTeam().opponent())){
                if (r.type == RobotType.NET_GUN && current.isAdjacentTo(r.location)){
                    if (tryDeposit(current.directionTo(r.location)))
                        return;
                }
            }
        }

        //secure the design school first and foremost
        if (!ds_secure)
            secureDS();
        if (!ds_secure)
            return;

        //we wait to build the wall until we get the signal, but after turn 1000 we don't have time to wait anymore
        if(rc.getRoundNum()> 1000)
            startDump = true;

        //if we're not in position, we need to either get in position or dig in place until a drone picks us up
        if (!landSpots.contains(current)) {
            if (!startDump && !innerSpots.isEmpty()) {
                HashSet<MapLocation> toRemove = new HashSet<>();
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

            }
        //if we have dirt and can start dumping, then pick the lowest adjacent dump spot and dump
        } else if (startDump && rc.getDirtCarrying() > 0) {
            MapLocation dump = null;
            HashSet<MapLocation> toRemove = new HashSet<>();
            for (MapLocation d : dumpSpots) {
                if (rc.canSenseLocation(d) && current.isAdjacentTo(d)) {
                    if (outerSpots.contains(d) && (rc.senseElevation(d) > 100))
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
        //if we haven't received the signal yet, then move to the middle
        } else if (!startDump && !innerSpots.contains(rc.getLocation())) {
            HashSet<MapLocation> toRemove = new HashSet<>();
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
        //if we haven't received the signal but we're already in the middle, then start the initial wall
        } else if (!startDump && rc.getDirtCarrying() > 0) {
            MapLocation dump = null;
            HashSet<MapLocation> toRemove = new HashSet<>();
            for (MapLocation d : firstDump) {
                //remove spots we're not adjacent to and/or spots with buildings on them
                if (!d.isAdjacentTo(current))
                    toRemove.add(d);
                else if (rc.senseRobotAtLocation(d) != null && (rc.senseRobotAtLocation(d).type == RobotType.VAPORATOR || rc.senseRobotAtLocation(d).type == RobotType.NET_GUN))
                    toRemove.add(d);
                //we need to make sure the wall is at least 6 before worrying about making sure the future net gun
                //and vaporator spots are level
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
                //finally pick the spot with the lowest elevation
                else if (dump == null)
                    dump = d;
                else if (rc.senseElevation(d) < rc.senseElevation(dump))
                    dump = d;
            }
            firstDump.removeAll(toRemove);
            if (dump != null && tryDeposit(current.directionTo(dump)))
                return;
        }
        //if we've made it this far and still haven't done anything, then just dig
        if (rc.isReady()) {
            //gotta make sure we're digging in smart locations so we don't flood ourselves
            //waterLevel[] is an array of turn counts for the first few water levels
            boolean floodDanger = false;
            int i = 0;
            //determine flood levels and check adjacent squares for flood danger
            while (waterLevel[i] < rc.getRoundNum())
                i++;
            if (rc.senseElevation(current) <= i) {
                for (Direction dir : directions) {
                    if (rc.onTheMap(current.add(dir)) && rc.senseFlooding(current.add(dir))) {
                        floodDanger = true;
                        break;
                    }
                }
            }
            //if we're in danger of flooding, dig somewhere safe, even if it's in a wall spot
            if (floodDanger) {
                for (Direction dir : directions) {
                    if (rc.onTheMap(current.add(dir)) && rc.senseElevation(current.add(dir)) > i)
                        if (tryDig(dir))
                            return;
                }
            //otherwise just dig wherever
            } else {
                for (MapLocation m : digSpots) {
                    if (rc.canSenseLocation(m) && rc.getLocation().isAdjacentTo(m) && tryDig(rc.getLocation().directionTo(m)))
                        return;
                }
            }
        }
    }

    //dump dirt
    public boolean tryDeposit(Direction dir) throws GameActionException{
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

    //dig dirt
    public boolean tryDig(Direction dir) throws GameActionException{
        RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(dir));
        if (r!= null && r.type == RobotType.MINER)
            return false;
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

    //secure the design school by building a small wall around it
    private void secureDS()throws GameActionException{
        if (!rc.isReady())
            return;
        HashSet<MapLocation> toRemove = new HashSet<>();
         if (rc.getDirtCarrying() > 0) {
             for (MapLocation m : dsDumpSpots) {
                 if (rc.canSenseLocation(m) && rc.senseElevation(m) - dsElevation < 3) {
                     if (rc.getLocation().isAdjacentTo(m) && tryDeposit(rc.getLocation().directionTo(m)))
                         return;
                 }
                 else if (rc.canSenseLocation(m))
                     toRemove.add(m);
             }
         }
        else{
            for (Direction dir : directions){
                if(digSpots.contains(rc.getLocation().add(dir)))
                    if (tryDig(dir))
                        return;
            }
        }
        dsDumpSpots.removeAll(toRemove);
        if(!dsDumpSpots.isEmpty()) {
            if (rc.getDirtCarrying() == 25) {
                MapLocation m = closestLocation(dsDumpSpots.toArray(new MapLocation[0]));
                Direction dir = rc.getLocation().directionTo(m);
                if (tryMove(dir))
                    return;
                else if (tryMove(dir.rotateLeft()))
                    return;
                else if (tryMove(dir.rotateRight()))
                    return;
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
