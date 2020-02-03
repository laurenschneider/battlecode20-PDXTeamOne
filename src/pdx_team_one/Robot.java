package pdx_team_one;
import battlecode.common.*;
import java.util.ArrayDeque;
import java.util.ArrayList;

//Parent class for all other robots

public abstract class Robot {
    static RobotController rc;

    static int TEAM_ID = 1111; // must be 4 digits for map transmission to work
    static final int HQ_LOCATION = 0;
    static final int HQ_FLOOD_DANGER = 1;
    static final int DESIGN_SCHOOL_BUILT = 2;
    static final int ENEMY_HQ_FOUND = 3;
    static final int SOUPS_FOUND = 4;
    static final int HQ_TARGET_ACQUIRED = 5;

    static final int UNEXPLORED = 0;
    static final int FLOODED = -1000;
    static final int x = 0;
    static final int y = 1;

    //message importance
    //map locations
    static final int DEFCON5 = 1;
    //HQ location
    static final int DEFCON4 = 5;
    //soup locations, design school built
    static final int DEFCON3 = 10;
    //
    static final int DEFCON2 = 25;
    //HQ in trouble/enemy HQ found
    static final int DEFCON1 = 50;

    static int[][] map;
    static int hqElevation;
    static MapLocation HQ = null;
    static int hqID = 0;
    static MapLocation destination = null;
    static MapLocation prevDestination = null;
    static ArrayList<MapLocation> visited = new ArrayList<>();
    static boolean design_school = false;
    static MapLocation enemyHQ = null;
    static int enemyHQID = 0;


    //TEAM_ID here is changed here for debugging because I can't figure out how
    //to scrimmage against a different team locally
    public Robot(RobotController r)
    {
        rc = r;
        if (rc.getTeam() == Team.A)
            TEAM_ID = 1111;
        else
            TEAM_ID = 2222;
    }

    public abstract void takeTurn() throws GameActionException;

    //strictly for debugging and measuring bytecode performance
    static int start,end, startturn, endturn;
    static void start(){
        start = Clock.getBytecodesLeft();
        startturn = rc.getRoundNum();
    }
    static void end(String s){
        end = Clock.getBytecodesLeft();
        endturn = rc.getRoundNum();
        System.out.println("It took me " + (endturn-startturn) + " turns and " + (start-end) + " bytecodes to " + s);
    }


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
        if (cost > rc.getTeamSoup())
            cost = rc.getTeamSoup();
        if (rc.canSubmitTransaction(message,  cost))
            rc.submitTransaction(message, cost);
    }

    //todo: make this better. Perhaps graphs?
    static void pathTo(MapLocation ml)throws GameActionException {
        if (rc.getCooldownTurns() >= 1)
            return;
        if (prevDestination == null || !(prevDestination.equals(ml))) {
            visited.clear();
            prevDestination = ml;
        }
        Direction dir = rc.getLocation().directionTo(ml);
        visited.add(rc.getLocation());
        for (int i = 0; i < 8; i++) {
            if (!(visited.contains(rc.getLocation().add(dir)))) {
                if (tryMove(dir))
                    return;
            }
            dir = dir.rotateLeft();
        }
        visited.clear();
        dir = rc.getLocation().directionTo(ml);
        visited.add(rc.getLocation());
        for (int i = 0; i < 8; i++) {
            if (!(visited.contains(rc.getLocation().add(dir)))) {
                if (tryMove(dir))
                    return;
            }
            dir = dir.rotateLeft();
        }

    }

    static boolean canTraverse(MapLocation a, MapLocation b) throws GameActionException{
        if (!a.isAdjacentTo(b))
            return false;
        if (!rc.onTheMap(a) || !rc.onTheMap(b)) {
            System.out.println("Either " + a.x + ", " + a.y + " or " + b.x + ", " + b.y + " is not on the map");
            return false;
        }
        if (rc.canSenseLocation(a) && rc.isLocationOccupied(a) && !(rc.getLocation().equals(a))) {
            System.out.println(a.x + ", " + a.y + " is occupied");
            return false;
        }
        if (rc.canSenseLocation(b) && rc.isLocationOccupied(b) && !(rc.getLocation().equals(b))) {
            System.out.println(b.x + ", " + b.y + " is occupied");
            return false;
        }
        if (map[a.x][a.y] == UNEXPLORED || map[b.x][b.y] == UNEXPLORED)
            return true;
        int diff = map[a.x][a.y] - map[b.x][b.y];
        System.out.println("You should only see this if returning true on this map");
        return (diff >= -3 && diff <= 3);
    }

    //idk how to optimize this any more than it is
    static void updateMap(int[] updates) {
        int[] polar = new int[11];
        int [] elevations = new int[11];

        int rcx,rcy;

        elevations[0] = (updates[0]/10000)%1000;
        polar[0] = updates[0]/10000000;

        rcy = updates[1]%100;
        rcx = (updates[1]/100)%100;
        elevations[1] = (updates[1]/10000)%1000;
        polar[1] = updates[1]/10000000;


        polar[8] = updates[2]%100;
        polar[7] = (updates[2]/100)%100;
        elevations[2] = (updates[2]/10000)%1000;
        polar[2] = updates[2]/10000000;

        if (updates[3] < 0){
            updates[3] *= -1;
            polar[7] += 100;
        }

        polar[10] = updates[3]%100;
        polar[9] = (updates[3]/100)%100;
        elevations[3] = (updates[3]/10000)%1000;
        polar[3] = updates[3]/10000000;

        if (updates[4] < 0){
            updates[4] *= -1;
            polar[8] += 100;
        }

        elevations[10] = updates[4]%10;
        elevations[7] = (updates[4]/10)%1000;
        elevations[4] = (updates[4]/10000)%1000;
        polar[4] = updates[4]/10000000;

        if (updates[5] < 0){
            updates[5] *= -1;
            polar[9] += 100;
        }

        elevations[10] += 10*(updates[5]%10);
        elevations[8] = (updates[5]/10)%1000;
        elevations[5] = (updates[5]/10000)%1000;
        polar[5] = updates[5]/10000000;

        if (updates[6] < 0){
            updates[6] *= -1;
            polar[10] += 100;
        }

        elevations[10] += 100*(updates[6]%10);
        elevations[9] = (updates[6]/10)%1000;
        elevations[6] = (updates[6]/10000)%1000;
        polar[6] = updates[6]/10000000;


        for (int i = 0; i < 11; i++){
            if (polar[i] != 0)
                map[rcx + coordinates[polar[i]][x]][rcy + coordinates[polar[i]][y]] = elevations[i];
            else {
                break;
            }
        }
    }

    //todo: optimize this
    static void updateLocalMap() throws GameActionException{
        int[] polar = new int[11];
        int[] elevations = new int[11];
        int rcx = rc.getLocation().x;
        int rcy = rc.getLocation().y;
        int count = 0;

        for (int i = -5; i<= 5 && count < 11 && rcx + i >= 0 && rcx + i < rc.getMapWidth(); i++){
            for (int j = -5; j <= 5 && count < 11 && rcy + j >= 0 && rcy + j < rc.getMapHeight(); j++){
                if (map[rcx+i][rcy+j] == UNEXPLORED) {
                    MapLocation check = rc.getLocation().translate(i, j);
                    if (rc.canSenseLocation(check)) {
                        if (rc.senseFlooding(check)) {
                            map[rcx + i][rcy + j] = FLOODED;
                            elevations[count] = 2;
                        } else {
                            int elevation = rc.senseElevation(check) - hqElevation + 500;
                            if (elevation > 999)
                                elevation = 999;
                            if (elevation < 2)
                                elevation = 2;
                            elevations[count] = elevation;
                            map[rcx + i][rcy + j] = elevation;
                        }
                        polar[count] = 85+i-13*j;
                        count++;
                    }
                }
            }
        }

        if (count > 7){
            int[] msg = new int[7];
            int over7 = 1;
            int over8 = 1;
            int over9 = 1;
            int over10 = 1;
            if (polar[7] >= 100) {
                over7 = -1;
                polar[7] -= 100;
            }
            if (polar[8] >= 100) {
                over8 = -1;
                polar[8] -= 100;
            }
            if (polar[9] >= 100) {
                over9 = -1;
                polar[9] -= 100;
            }
            if (polar[10] >= 100) {
                over10 = -1;
                polar[10] -= 100;
            }

            msg[0] = 10000000*polar[0] + 10000*elevations[0] + TEAM_ID;
            msg[1] = 10000000*polar[1] + 10000*elevations[1] + 100*rcx + rcy;
            msg[2] = 10000000*polar[2] + 10000*elevations[2] + 100*polar[7] + polar[8];
            msg[3] = over7*(10000000*polar[3] + 10000*elevations[3] + 100*polar[9] + polar[10]);
            msg[4] = over8*(10000000*polar[4] + 10000*elevations[4] + 10*elevations[7] + elevations[10]%10);
            msg[5] = over9*(10000000*polar[5] + 10000*elevations[5] + 10*elevations[8] + (elevations[10]/10)%10);
            msg[6] = over10*(10000000*polar[6] + 10000*elevations[6] + 10*elevations[9] + (elevations[10]/100));

            sendMessage(msg,DEFCON5);
        }
    }

    //todo: check if location is reachable. If no locations are reachable, return null
    //todo: also implement some better logic instead of a completely random location
    static MapLocation randomUnexploredLocation(){
        while (true){
            int x = (int)(Math.random() * rc.getMapWidth());
            int y = (int)(Math.random() * rc.getMapHeight());
            if (map[x][y] == UNEXPLORED){
                return new MapLocation(x,y);
            }
        }
    }

    static final int[][] coordinates = {
            {-100,-100},
            {-6, 6},
            {-5, 6},
            {-4, 6},
            {-3, 6},
            {-2, 6},
            {-1, 6},
            {0, 6},
            {1, 6},
            {2, 6},
            {3, 6},
            {4, 6},
            {5, 6},
            {6, 6},
            {-6, 5},
            {-5, 5},
            {-4, 5},
            {-3, 5},
            {-2, 5},
            {-1, 5},
            {0, 5},
            {1, 5},
            {2, 5},
            {3, 5},
            {4, 5},
            {5, 5},
            {6, 5},
            {-6, 4},
            {-5, 4},
            {-4, 4},
            {-3, 4},
            {-2, 4},
            {-1, 4},
            {0, 4},
            {1, 4},
            {2, 4},
            {3, 4},
            {4, 4},
            {5, 4},
            {6, 4},
            {-6, 3},
            {-5, 3},
            {-4, 3},
            {-3, 3},
            {-2, 3},
            {-1, 3},
            {0, 3},
            {1, 3},
            {2, 3},
            {3, 3},
            {4, 3},
            {5, 3},
            {6, 3},
            {-6, 2},
            {-5, 2},
            {-4, 2},
            {-3, 2},
            {-2, 2},
            {-1, 2},
            {0, 2},
            {1, 2},
            {2, 2},
            {3, 2},
            {4, 2},
            {5, 2},
            {6, 2},
            {-6, 1},
            {-5, 1},
            {-4, 1},
            {-3, 1},
            {-2, 1},
            {-1, 1},
            {0, 1},
            {1, 1},
            {2, 1},
            {3, 1},
            {4, 1},
            {5, 1},
            {6, 1},
            {-6, 0},
            {-5, 0},
            {-4, 0},
            {-3, 0},
            {-2, 0},
            {-1, 0},
            {0, 0},
            {1, 0},
            {2, 0},
            {3, 0},
            {4, 0},
            {5, 0},
            {6, 0},
            {-6, -1},
            {-5, -1},
            {-4, -1},
            {-3, -1},
            {-2, -1},
            {-1, -1},
            {0, -1},
            {1, -1},
            {2, -1},
            {3, -1},
            {4, -1},
            {5, -1},
            {6, -1},
            {-6, -2},
            {-5, -2},
            {-4, -2},
            {-3, -2},
            {-2, -2},
            {-1, -2},
            {0, -2},
            {1, -2},
            {2, -2},
            {3, -2},
            {4, -2},
            {5, -2},
            {6, -2},
            {-6, -3},
            {-5, -3},
            {-4, -3},
            {-3, -3},
            {-2, -3},
            {-1, -3},
            {0, -3},
            {1, -3},
            {2, -3},
            {3, -3},
            {4, -3},
            {5, -3},
            {6, -3},
            {-6, -4},
            {-5, -4},
            {-4, -4},
            {-3, -4},
            {-2, -4},
            {-1, -4},
            {0, -4},
            {1, -4},
            {2, -4},
            {3, -4},
            {4, -4},
            {5, -4},
            {6, -4},
            {-6, -5},
            {-5, -5},
            {-4, -5},
            {-3, -5},
            {-2, -5},
            {-1, -5},
            {0, -5},
            {1, -5},
            {2, -5},
            {3, -5},
            {4, -5},
            {5, -5},
            {6, -5},
            {-6, -6},
            {-5, -6},
            {-4, -6},
            {-3, -6},
            {-2, -6},
            {-1, -6},
            {0, -6},
            {1, -6},
            {2, -6},
            {3, -6},
            {4, -6},
            {5, -6},
            {6, -6}
    };
}
