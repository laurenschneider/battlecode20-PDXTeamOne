package pdx_team_one;
import battlecode.common.*;

import java.util.HashSet;

public class HQ extends Robot{

    int numMiners = 0;
    int maxMiners = 6;
    private boolean attack;
    private boolean strategy;
    private boolean locationSent;
    int phase = 1;
    HashSet<MapLocation> buildings = new HashSet<>();

    HQ(RobotController r) throws GameActionException{
        super(r);
        HQ = rc.getLocation();
        initBuildings();
    }

    public void takeTurn() throws GameActionException {
        if (!locationSent) {
            locationSent = sendLocation();
            strategy = determineStrategy();
            MapLocation[] soups = rc.senseNearbySoup();
            broadcastSoup(soups);
        }
        defense();
        if (numMiners < maxMiners)
            numMiners = buildMiners();
        if (!attack)
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

    public boolean determineStrategy()throws GameActionException {
        int soup = 0;
        for (MapLocation m : rc.senseNearbySoup())
            soup += rc.senseSoup(m);
        maxMiners = soup / 300;
        if (maxMiners < 3)
            maxMiners = 3;
        if (maxMiners > 6)
            maxMiners = 6;
        /*if (soup < 2000){
            System.out.println("Not enough soup");
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = ATTACK;
            attack = true;
            return sendMessage(msg,DEFCON5);
        }*/

        MapLocation fc = null,ds = null;
        for (MapLocation m : buildings){
            if (evaluate(m)) {
                    ds = m;
                    break;
            }
        }

        if (ds == null) {
            System.out.println("No suitable locations");
            rc.resign();
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = ATTACK;
            attack = true;
            return sendMessage(msg, DEFCON5);
        }
        else{
            System.out.println(fc + ", " + ds);
            int[] msg = new int[7];
            msg[0] = TEAM_ID;
            msg[1] = DEFENSE;
            msg[2] = 0;//fc.x;
            msg[3] = 0;//fc.y;
            msg[4] = ds.x;
            msg[5] = ds.y;
            return sendMessage(msg,DEFCON5);
        }
    }

    public boolean evaluate(MapLocation m)throws GameActionException{
        if (!rc.canSenseLocation(m))
            return false;
        if (rc.senseElevation(m) < 3)
            return false;
        if (rc.senseElevation(m) - rc.senseElevation(HQ) >= 3 || rc.senseElevation(m) - rc.senseElevation(HQ) <= -3)
            return false;
        int spots = 0;
        for (Direction dir : directions){
            if (rc.canSenseLocation(m.add(dir)) && rc.senseElevation(m) -  rc.senseElevation(m.add(dir)) <= 3 && rc.senseElevation(m.add(dir)) - rc.senseElevation(m) <= 3)
                spots++;
        }
        return (spots >= 4);
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
    public void initBuildings() {
        for (int i = -2; i <= 2; i++) {
            System.out.println(HQ);
            if (rc.onTheMap(HQ.translate(i, 5)))
                buildings.add(HQ.translate(i, 5));
            if (rc.onTheMap(HQ.translate(i, -5)))
                buildings.add(HQ.translate(i, -5));
            if (rc.onTheMap(HQ.translate(5, i)))
                buildings.add(HQ.translate(5, i));
            if (rc.onTheMap(HQ.translate(-5, i)))
                buildings.add(HQ.translate(-5, i));
        }
        for (int i = -3; i <= 3; i++) {
            if (rc.onTheMap(HQ.translate(i, 4)))
                buildings.add(HQ.translate(i, 4));
            if (rc.onTheMap(HQ.translate(i, -4)))
                buildings.add(HQ.translate(i, -4));
            if (rc.onTheMap(HQ.translate(4, i)))
                buildings.add(HQ.translate(4, i));
            if (rc.onTheMap(HQ.translate(-4, i)))
                buildings.add(HQ.translate(-4, i));
        }
    }
}
