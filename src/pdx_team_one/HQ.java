package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayDeque;
import java.util.HashSet;

//HQ builds miners, shoots drones, and when HQ is destroyed, the game is lost
public class HQ extends Building{

    private int numMiners = 0;
    private int maxMiners;
    private boolean strategy;
    private boolean locationSent;
    private boolean[][] visited = new boolean[rc.getMapWidth()][rc.getMapHeight()];
    private HashSet<MapLocation> dsSpots = new HashSet<>();
    private HashSet<MapLocation> fcSpots = new HashSet<>();
    private HashSet<MapLocation> innerSpots;
    private HashSet<MapLocation> wallSpots;
    private HashSet<MapLocation> outerSpots;
    int DSelevation = -100000;
    private ArrayDeque<Node> checkSpots = new ArrayDeque<>();
    private boolean innerSpotsFilled;
    private boolean wallSpotsFilled;

    //this is used to determine optimal Design School location
    private static class Node{
        int moves;
        MapLocation loc;
        int elevation;
        Node(int m, MapLocation l) throws GameActionException{
            moves = m;
            loc = l;
            elevation = rc.senseElevation(l);
        }
    }

    //initializes map locations
    HQ(RobotController r) throws GameActionException{
        super(r);
        HQ = rc.getLocation();
        hqElevation = rc.senseElevation(rc.getLocation());
        visited[HQ.x][HQ.y] = true;
        checkSpots.add(new Node(0, HQ));
        innerSpots = initInnerSpots();
        wallSpots = initWallSpots();
        outerSpots = initOuterSpots();
        int soup = 0;

        for (MapLocation m : rc.senseNearbySoup())
            soup += rc.senseSoup(m);
        maxMiners = soup/500;
        if (maxMiners < 4)
            maxMiners = 4;
        else if (maxMiners > 6)
            maxMiners = 6;
    }

    public void takeTurn() throws GameActionException {
        //sends out where it's located to everyone
        if (!locationSent) {
            locationSent = sendLocation();
            MapLocation[] soups = rc.senseNearbySoup();
            broadcastSoup(soups);
        }
        //shoot down any net guns;
        defense();
        //build miners
        if (numMiners < maxMiners && rc.getTeamSoup() >= RobotType.MINER.cost)
            numMiners = buildMiners();
        //search for design school location
        if (!checkSpots.isEmpty())
            BFS(checkSpots.remove());
        //determine best design school location and send it out
        else if (!strategy && Clock.getBytecodesLeft() > 15000){
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
            if (ds == null || fc == null){
                ds = HQ.add(Direction.NORTH).add(Direction.NORTH).add(Direction.NORTH);
                fc = HQ.add(Direction.NORTH).add(Direction.NORTH).add(Direction.NORTHEAST);
                maxMiners = 6;
            }
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = DEFENSE;
            msg[2] = fc.x;
            msg[3] = fc.y;
            msg[4] = ds.x;
            msg[5] = ds.y;
            sendMessage(msg, DEFCON5);
            strategy = true;
        }
        //check to see what phase we're on
        if(!innerSpotsFilled)
            checkInnerSpots();
        if (!wallSpotsFilled)
            checkWallSpots();
    }

    //send location
    private boolean sendLocation() throws GameActionException {
        int [] message = new int[7];
        message[0] = TEAM_ID;
        message[1] = HQ_LOCATION;
        message[2] = rc.getLocation().x;
        message[3] = rc.getLocation().y;
        message[4] = rc.getID();
        message[5] = rc.senseElevation(rc.getLocation());
        return sendMessage(message,DEFCON5);
    }

    //build a miner
    private int buildMiners() throws GameActionException {
        Direction dir = randomDirection();
        while(!tryBuild(RobotType.MINER,dir)){
            dir = randomDirection();
        }
        return ++numMiners;
    }


    //checks to see if m is a possibility for the design school. It must not interfere with the wall and have at least
    //4 landing sports for landscapers
    private boolean evaluate(MapLocation m)throws GameActionException {
        if (!rc.canSenseLocation(m))
            return false;
        if(m.distanceSquaredTo(HQ) <= 13 || outerSpots.contains(m) || wallSpots.contains(m) || innerSpots.contains(m))
            return false;
        int spots = 0;
        for (Direction dir : directions) {
            if (rc.canSenseLocation(m.add(dir))) {
                if (rc.senseFlooding(m.add(dir)))
                    return false;
                if (elevationDiff(m,m.add(dir)) <= 3)
                    spots++;
            }
        }
        return (spots >= 4);
    }

    //a recursive breadth-first search to determine the optimal location for the design school
    private void BFS(Node m) throws GameActionException{
        //if the spot is close enough for miners to easily get to
        if (m.moves <= 6) {
            //if it passes the evaluation, then we only want to spots that are the highest elevations
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
            //add the surrounding spots to the BFS if we haven't already checked them
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
        //if we have enough time, keep chugging along
        if (!checkSpots.isEmpty() && Clock.getBytecodesLeft() > 2000)
            BFS(checkSpots.remove());
    }

    //see if landscapers have filled the inner spots and let everyone know
    private void checkInnerSpots() throws GameActionException {
        for (MapLocation m : innerSpots) {
            RobotInfo r = rc.senseRobotAtLocation(m);
            if (r == null || r.type != RobotType.LANDSCAPER)
                return;
        }
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = INNER_SPOTS_FILLED;
        if (sendMessage(msg, DEFCON5))
            innerSpotsFilled = true;
    }

    //see if landscapers have filled the wall spots and let everyone know
    private void checkWallSpots() throws GameActionException {
        for (MapLocation m : wallSpots) {
            RobotInfo r = rc.senseRobotAtLocation(m);
            if (r == null || r.type != RobotType.LANDSCAPER)
                return;
        }
        int[] msg = new int[7];
        msg[0] = TEAM_ID;
        msg[1] = WALL_SPOTS_FILLED;
        if (sendMessage(msg, DEFCON5))
            wallSpotsFilled = true;
    }
}
