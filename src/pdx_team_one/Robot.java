package pdx_team_one;
import battlecode.common.*;

//Parent class for all other robots

public abstract class Robot {
    static RobotController rc;

    static final int TEAM_ID = 11111;
    static final int HQ_LOCATION = 0;
    static final int HQ_FLOOD_DANGER = 1;
    static final int DESIGN_SCHOOL_BUILT = 2;
    static final int UNEXPLORED = -1000;

    static int[][] map;
    static int hqElevation;
    static MapLocation destination = null;
    static boolean design_school = false;

    public Robot(RobotController r)
    {
        rc = r;
    }

    public abstract void takeTurn() throws GameActionException;

    static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
            rc.move(dir);
            updateLocalMap();
            return true;
        } else return false;
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static void sendMessage(int[] message,int cost) throws GameActionException {
        if (rc.canSubmitTransaction(message,  cost))
            rc.submitTransaction(message, cost);
    }

    //todo: make this better. Perhaps graphs?
    static void pathTo(MapLocation ml)throws GameActionException{
        Direction toward = rc.getLocation().directionTo(ml);
        while (!tryMove(toward)){
            toward = toward.rotateLeft();
        }
    }

    static void parseBlockchain() throws GameActionException{
        for (int i = 1; i < rc.getRoundNum(); i++){
            for (Transaction t : rc.getBlock(i)){
                if (t.getMessage()[0] == TEAM_ID){
                    if (t.getMessage()[1] == DESIGN_SCHOOL_BUILT)
                        design_school = true;
                    else if (t.getMessage()[1] == HQ_LOCATION)
                        hqElevation = t.getMessage()[6];
                }
                else if (t.getMessage()[0]/100000 == TEAM_ID){
                    updateMap(t.getMessage());
                }
            }
        }
    }

    static void parseBlockchain(int round) throws GameActionException {
        for (Transaction t : rc.getBlock(round)) {
            if (t.getMessage()[0] == TEAM_ID) {
                if (t.getMessage()[1] == DESIGN_SCHOOL_BUILT)
                    design_school = true;
            } else if (t.getMessage()[0] / 100000 == TEAM_ID) {
                updateMap(t.getMessage());
            }
        }

    }

    static void updateMap(int[] updates){

        int[] elevations = new int[11];
        int[] xs = new int[11];
        int[] ys = new int[11];
        int rcx,rcy;

        elevations[0] = updates[0]%1000 - 500;
        updates[0] /= 1000;
        ys[0] = updates[0]%10;
        updates[0] /= 10;
        xs[0] = updates[0]%10;

        rcy = updates[1]%100;
        updates[1] /= 100;
        rcx = updates[1]%100;
        updates[1] /= 100;
        elevations[1] = updates[1]%1000 - 500;
        updates[1] /= 1000;
        ys[1] = updates[1]%10;
        updates[1] /= 100;
        xs[1] = updates[1]%10;

        elevations[2] = updates[2]%1000 - 500;
        updates[2] /= 1000;
        ys[4] = updates[2]%10;
        updates[2] /= 10;
        xs[4] = updates[2]%10;
        updates[2] /= 10;
        ys[3] = updates[2]%10;
        updates[2] /= 10;
        xs[3] = updates[2]%10;
        updates[2] /= 10;
        ys[2] = updates[2]%10;
        updates[2] /= 10;
        xs[2] = updates[2]%10;

        elevations[3] = updates[3]%1000 - 500;
        updates[3] /= 1000;
        ys[7] = updates[3]%10;
        updates[3] /= 10;
        xs[7] = updates[3]%10;
        updates[3] /= 10;
        ys[6] = updates[3]%10;
        updates[3] /= 10;
        xs[6] = updates[3]%10;
        updates[3] /= 10;
        ys[5] = updates[3]%10;
        updates[3] /= 10;
        xs[5] = updates[3]%10;

        elevations[4] = updates[4]%1000 - 500;
        updates[4] /= 1000;
        ys[10] = updates[4]%10;
        updates[4] /= 10;
        xs[10] = updates[4]%10;
        updates[4] /= 10;
        ys[9] = updates[4]%10;
        updates[4] /= 10;
        xs[9] = updates[4]%10;
        updates[4] /= 10;
        ys[8] = updates[4]%10;
        updates[4] /= 10;
        xs[8] = updates[4]%10;

        elevations[7] = updates[5]%1000 - 500;
        updates[5] /= 1000;
        elevations[6] = updates[5]%1000 - 500;
        updates[5] /= 1000;
        elevations[5] = updates[5]%1000 - 500;

        elevations[10] = updates[6]%1000 - 500;
        updates[6] /= 1000;
        elevations[9] = updates[6]%1000 - 500;
        updates[6] /= 1000;
        elevations[8] = updates[6]%1000 - 500;

        for (int i = 0; i < 11; i++){
            if (elevations[i] == 0)
                continue;
            if (xs[i] < 5)
                xs[i] -= 5;
            else
                xs[i] -= 4;
            if (ys[i] < 5)
                ys[i] -= 5;
            else
                ys[i] -= 4;
            map[rcx + xs[i]][rcy + ys[i]] = elevations[i];
        }
    }

    static void updateLocalMap() throws GameActionException{
        String[] xs = new String[11];
        String[] ys = new String[11];
        String[] elevations = new String[11];
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        int count = 0;
        for (int i = -5; i<= 5 && count < 11; i++){
            for (int j = -5; j <= 5 && count < 11; j++){
                MapLocation check = new MapLocation(x + i, x + j);
                if (rc.canSenseLocation(check)){
                    if (map[x+i][y+j] == UNEXPLORED){
                        if (i < 0)
                            xs[count] = Integer.toString(i + 5);
                        else
                            xs[count] = Integer.toString(i + 4);
                        if (y < 0)
                            ys[count] = Integer.toString(i + 5);
                        else
                            ys[count] = Integer.toString(i + 4);
                        if (rc.senseFlooding(check)) {
                            elevations[count] = "1";
                            map[x+i][y+j] = 1;
                        }
                        else {
                            int elevation = rc.senseElevation(check) - hqElevation + 500;
                            if (elevation > 999)
                                elevation = 999;
                            if (elevation < 1)
                                elevation = 1;
                            if (elevation < 10)
                                elevations[count] = "00" + elevation;
                            else if (elevation < 100)
                                elevations[count] = "0" + elevation;
                            else
                                elevations[count] = Integer.toString(elevation);
                            map[x + i][y + j] = elevation - 500;
                        }
                        count++;
                    }
                }
            }
        }


        if (count > 0){
            for (int i = 10; i > count; i--){
                xs[i] = "0";
                ys[i] = "0";
                elevations[i] = "000";
            }
            int[] msg = new int[7];
            String xstring, ystring;
            if (x < 10)
                xstring = "0" + x;
            else
                xstring = Integer.toString(x);

            if (y < 10)
                ystring = "0" + y;
            else
                ystring = Integer.toString(y);

            msg[0] = Integer.parseInt("11111" + xs[0] + ys[0] + elevations[0]);
            msg[1] = Integer.parseInt(xs[1] + ys[1] + elevations[1] + xstring + ystring);
            msg[0] = Integer.parseInt(xs[2] + ys[2] + xs[3] + ys[3] + xs[4] + ys[4] + elevations[2]);
            msg[0] = Integer.parseInt(xs[5] + ys[5] + xs[6] + ys[6] + xs[7] + ys[7] + elevations[3]);
            msg[0] = Integer.parseInt(xs[8] + ys[8] + xs[9] + ys[9] + xs[10] + ys[10] + elevations[4]);
            msg[0] = Integer.parseInt(elevations[5] + elevations[6] + elevations[7]);
            msg[0] = Integer.parseInt(elevations[8] + elevations[9] + elevations[10]);
            sendMessage(msg,1);
        }

    }

    static MapLocation randomLocation(){
        while (true){
            int x = (int)(Math.random() * rc.getMapWidth());
            int y = (int)(Math.random() * rc.getMapHeight());
            if (map[x][y] == UNEXPLORED){
                destination = new MapLocation(x,y);
                return destination;
            }
        }
    }

}
