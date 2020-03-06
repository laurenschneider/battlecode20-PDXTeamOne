package pdx_team_one;
import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.HashSet;

public class HQ extends Robot{

    int numMiners = 0;
    int maxMiners = 6;
    private boolean attack;
    private boolean strategy;
    private boolean locationSent;
    private boolean[][] visited = new boolean[rc.getMapWidth()][rc.getMapHeight()];
    private HashSet<MapLocation> dsSpots = new HashSet<>();
    private ArrayDeque<Node> checkSpots = new ArrayDeque<>();
    int phase = 1;

    class Node{
        int moves;
        MapLocation loc;
        Node(int m, MapLocation l){
            moves = m;
            loc = l;
        }
    }

    HQ(RobotController r) throws GameActionException{
        super(r);
        HQ = rc.getLocation();
        visited[HQ.x][HQ.y] = true;
        checkSpots.add(new Node(0,HQ));
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
            MapLocation target = null;
            for (MapLocation m : dsSpots){
                if (target == null)
                    target = m;
                else if (rc.senseElevation(m) > rc.senseElevation(target))
                    target = m;
            }
            if (target != null) {
                int[] msg = new int[7];
                msg[0] = TEAM_ID;
                msg[1] = DEFENSE;
                msg[2] = 0;//fc.x;
                msg[3] = 0;//fc.y;
                msg[4] = target.x;
                msg[5] = target.y;
                sendMessage(msg, DEFCON5);
                System.out.println("DS: " + target);
            }
            else{
                System.out.println("No suitable Locations");
                rc.resign();
            }
            strategy = true;
        }
        else if (!attack)
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
            if (evaluate(m.loc))
                dsSpots.add(m.loc);
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
    }
}
