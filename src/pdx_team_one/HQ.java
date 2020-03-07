package pdx_team_one;
import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.HashSet;

public class HQ extends Robot{

    int numMiners = 0;
    int maxMiners = 6;
    private boolean strategy;
    private boolean locationSent;
    private boolean[][] visited = new boolean[rc.getMapWidth()][rc.getMapHeight()];
    private HashSet<MapLocation> dsSpots = new HashSet<>();
    private HashSet<MapLocation> fcSpots = new HashSet<>();
    private HashSet<MapLocation> innerSpots = new HashSet<>();
    private HashSet<MapLocation> wallSpots = new HashSet<>();
    int DSelevation = -100000;
    private ArrayDeque<Node> checkSpots = new ArrayDeque<>();
    int phase = 1;

    class Node{
        int moves;
        MapLocation loc;
        int elevation;
        Node(int m, MapLocation l) throws GameActionException{
            moves = m;
            loc = l;
            elevation = rc.senseElevation(l);
        }
    }

    HQ(RobotController r) throws GameActionException{
        super(r);
        HQ = rc.getLocation();
        visited[HQ.x][HQ.y] = true;
        checkSpots.add(new Node(0,HQ));
        for (Direction dir : corners)
            innerSpots.add(HQ.add(dir));
        for (Direction dir : directions){
            wallSpots.add(HQ.add(dir).add(dir));
            wallSpots.add(HQ.add(dir).add(dir.rotateRight()));
        }
    }

    public void takeTurn() throws GameActionException {
        if (!locationSent) {
            locationSent = sendLocation();
            MapLocation[] soups = rc.senseNearbySoup();
            broadcastSoup(soups);
        }
        defense();
        if (numMiners < maxMiners)
            numMiners = buildMiners();
        if (!checkSpots.isEmpty())
            BFS(checkSpots.remove());
        else if (!strategy && Clock.getBytecodesLeft() > 15000){
            System.out.println("Starting with " + Clock.getBytecodesLeft());
            MapLocation[] corners = new MapLocation[4];
            corners[0] = new MapLocation(0,0);
            corners[1] = new MapLocation(0,rc.getMapHeight());
            corners[2] = new MapLocation(rc.getMapWidth(),0);
            corners[3] = new MapLocation(rc.getMapWidth(),rc.getMapHeight());

            MapLocation corner = closestLocation(corners);
            MapLocation ds = null, fc = null;
            for (MapLocation m : dsSpots){
                if (ds == null)
                    ds = m;
                else if (fc == null)
                    fc = m;
                else if (m.distanceSquaredTo(corner) < ds.distanceSquaredTo(corner)) {
                    fc = ds;
                    ds = m;
                }
                else if (m.distanceSquaredTo(corner) < fc.distanceSquaredTo(corner)){
                    fc = m;
                }
            }
            if (fc == null){
                for (MapLocation m : fcSpots) {
                    if (fc == null)
                        fc = m;
                    else if (m.distanceSquaredTo(corner) < fc.distanceSquaredTo(corner))
                        fc = m;
                }
            }
            if (fc != null) {
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = DEFENSE;
                msg[2] = fc.x;
                msg[3] = fc.y;
                msg[4] = ds.x;
                msg[5] = ds.y;
                sendMessage(msg, DEFCON5);
                System.out.println("DS: " + ds);
            }
            else{
                System.out.println("No suitable Locations");
                rc.resign();
            }
            strategy = true;
            System.out.println("Ending with " + Clock.getBytecodesLeft());
        }
        checkPhase();
    }

    public boolean sendLocation() throws GameActionException {
        int [] message = new int[7];
        message[0] = TEAM_ID;
        message[1] = HQ_LOCATION;
        message[2] = rc.getLocation().x;
        message[3] = rc.getLocation().y;
        message[4] = rc.getID();
        message[5] = rc.senseElevation(rc.getLocation());
        return sendMessage(message,DEFCON5);
    }

    public int buildMiners() throws GameActionException {
        for (Direction dir : corners)
            if (tryBuild(RobotType.MINER, dir))
                return ++numMiners;
        for (Direction dir : Direction.cardinalDirections())
            if (tryBuild(RobotType.MINER, dir)) {
                return ++numMiners;
            }
        return numMiners;
    }


    public boolean evaluate(MapLocation m)throws GameActionException {
        if (!rc.canSenseLocation(m))
            return false;
        if (HQ.distanceSquaredTo(m) <= 13 || HQ.distanceSquaredTo(m) == 18)
            return false;
        int spots = 0;
        for (Direction dir : directions) {
            if (rc.canSenseLocation(m.add(dir))) {
                if (rc.senseFlooding(m.add(dir)))
                    return false;
                if (rc.senseElevation(m) - rc.senseElevation(m.add(dir)) <= 3 && rc.senseElevation(m.add(dir)) - rc.senseElevation(m) <= 3)
                    spots++;
            }
        }
        return (spots >= 4);
    }

    public void BFS(Node m) throws GameActionException{
        if (m.moves <= 6) {
            if (evaluate(m.loc)) {
                if (m.elevation == DSelevation)
                    dsSpots.add(m.loc);
                else if (m.elevation > DSelevation){
                    fcSpots.clear();
                    fcSpots.addAll(dsSpots);
                    dsSpots.clear();
                    dsSpots.add(m.loc);
                    DSelevation = m.elevation;
                }
            }
            for (Direction dir : directions) {
                MapLocation check = m.loc.add(dir);
                if (rc.onTheMap(check) && rc.canSenseLocation(check) && !visited[check.x][check.y]) {
                    if (rc.senseElevation(m.loc) - rc.senseElevation(check) <= 3 && rc.senseElevation(check) - rc.senseElevation(m.loc) <= 3) {
                        visited[check.x][check.y] = true;
                        checkSpots.add(new Node(m.moves + 1, check));
                    }
                }
            }
        }
        if (!checkSpots.isEmpty() && Clock.getBytecodesLeft() > 2000)
            BFS(checkSpots.remove());
    }

    public void checkPhase() throws GameActionException{
        if (phase == 1){
            for (Direction dir: Direction.cardinalDirections()){
                RobotInfo r = rc.senseRobotAtLocation(HQ.add(dir));
                if (r == null || (r.type != RobotType.VAPORATOR && r.type != RobotType.NET_GUN))
                    return;
            }
            System.out.println("Starting phase 2");
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = START_PHASE_2;
            if(sendMessage(msg,DEFCON5))
                phase++;
        }
        if (phase == 2){
            for (MapLocation m : innerSpots){
                RobotInfo r = rc.senseRobotAtLocation(m);
                if (r== null || r.type!= RobotType.LANDSCAPER)
                    return;
            }
            System.out.println("Inner Spots filled");
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = INNER_SPOTS_FILLED;
            if(sendMessage(msg,DEFCON5))
                phase++;
        }
        else if (phase == 3){
            for (MapLocation m : wallSpots){
                RobotInfo r = rc.senseRobotAtLocation(m);
                if (r == null || r.type != RobotType.LANDSCAPER)
                    return;
            }
            System.out.println("Wall Spots filled");
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = WALL_SPOTS_FILLED;
            if(sendMessage(msg,DEFCON5))
                phase++;
        }
    }
}
