package pdx_team_one;

import battlecode.common.*;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.ArrayList;

import static junit.framework.TestCase.*;

public class MinerTest {
    RobotController rcMock = Mockito.mock(RobotController.class);
    Miner testMiner = new Miner(rcMock) {};

    public MinerTest() throws GameActionException {}

    @Test
    public void scoutTakeTurn() throws GameActionException{
        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Mockito.doReturn(new Transaction[0]).when(rcMock).getBlock(1);
        testMiner = new Miner(rcMock);
        testMiner.scout = true;
        testMiner.enemyHQ = new MapLocation(0,0);
        testMiner.enemy_design_school = false;
        Mockito.doReturn(new MapLocation(2,2)).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.DESIGN_SCHOOL,Direction.SOUTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        testMiner.takeTurn();
    }

    @Test
    public void minerTakeTurn() throws GameActionException{
        Mockito.doReturn(3).when(rcMock).getRoundNum();
        Mockito.doReturn(new Transaction[0]).when(rcMock).getBlock(1);
        Mockito.doReturn(new Transaction[0]).when(rcMock).getBlock(2);
        Mockito.doReturn(new Transaction[0]).when(rcMock).getBlock(0);
        testMiner = new Miner(rcMock);
        testMiner.refineries = new ArrayList<>();
        testMiner.builder = false;
        testMiner.scout = false;
        Mockito.doReturn(new RobotInfo[0]).when(rcMock).senseNearbyRobots();
        Mockito.doReturn(new MapLocation[0]).when(rcMock).senseNearbySoup();
        Mockito.doReturn(0).when(rcMock).getSoupCarrying();
        Mockito.doReturn(100.0f).when(rcMock).getCooldownTurns();
        Mockito.doReturn(new MapLocation(4, 4)).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.REFINERY, Direction.SOUTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(500).when(rcMock).getTeamSoup();
        testMiner.takeTurn();
    }

    @Test
    public void scoutBuildDesign() throws GameActionException {
        testMiner.scout = true;
        testMiner.enemyHQ = new MapLocation(0,0);
        testMiner.enemy_design_school = false;
        Mockito.doReturn(new MapLocation(2,2)).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.DESIGN_SCHOOL,Direction.SOUTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        assertEquals(1,testMiner.doScoutThings());
    }

    @Test
    public void scoutFindEnemySuccess() throws GameActionException {
        testMiner.scout = true;
        testMiner.enemyHQ = null;
        Mockito.doReturn(new MapLocation(0,0)).when(rcMock).getLocation();
        RobotInfo[] r = new RobotInfo[1];
        r[0] = new RobotInfo(0,Team.A, RobotType.HQ, 0, false, 100, 0,0, new MapLocation(2,2));
        Mockito.doReturn(r).when(rcMock).senseNearbyRobots();
        Mockito.doReturn(Team.B).when(rcMock).getTeam();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.DESIGN_SCHOOL,Direction.NORTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        assertEquals(2,testMiner.doScoutThings());
    }

    @Test
    public void scoutOutOfTime() throws GameActionException {
        testMiner.scout = true;
        testMiner.enemyHQ = null;
        testMiner.HQ = new MapLocation(25,25);
        testMiner.refineries = new ArrayList<>();
        Mockito.doReturn(new MapLocation(4,4)).when(rcMock).getLocation();
        Mockito.doReturn(new RobotInfo[0]).when(rcMock).senseNearbyRobots();
        MapLocation[] s = new MapLocation[1];
        s[0] = new MapLocation(0,0);
        Mockito.doReturn(s).when(rcMock).senseNearbySoup();
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(true).when(rcMock).canMove(Direction.EAST);
        Mockito.doReturn(false).when(rcMock).senseFlooding(new MapLocation(5,4));
        assertEquals(3,testMiner.doScoutThings());
    }


    /*
    @Test
    public void scoutEndEarly() throws GameActionException {
        testMiner.scout = true;
        testMiner.enemyHQ = null;
        testMiner.HQ = new MapLocation(25,25);
        Mockito.doReturn(new MapLocation(4,4)).when(rcMock).getLocation();
        Mockito.doReturn(new RobotInfo[0]).when(rcMock).senseNearbyRobots();
        Mockito.doReturn(new MapLocation[0]).when(rcMock).senseNearbySoup();
        Mockito.doReturn(5.0f).when(rcMock).getCooldownTurns();
        //Mockito.doReturn(100).when(Mockito.mock(Clock.class)).getBytecodesLeft();
        assertEquals(4,testMiner.doScoutThings());
    }*/



    @Test
    public void builderDesignSchool() throws GameActionException{
        testMiner.vaporators = 1;
        testMiner.builder = true;
        testMiner.design_school = false;
        testMiner.HQ = new MapLocation(50,50);
        testMiner.vaporLocations = new ArrayDeque<>();
        Mockito.doReturn(new MapLocation(1, 1)).when(rcMock).getLocation();
        Mockito.doReturn(new RobotInfo[0]).when(rcMock).senseNearbyRobots();
        Mockito.doReturn(new MapLocation[0]).when(rcMock).senseNearbySoup();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.DESIGN_SCHOOL,Direction.SOUTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(new MapLocation(4,4)).when(rcMock).adjacentLocation(Direction.SOUTHWEST);
        assertEquals(3,testMiner.doBuilderThings());
    }

    @Test
    public void builderFulfillmentCenter() throws GameActionException{
        testMiner.vaporators = 1;
        testMiner.builder = true;
        testMiner.design_school = true;
        testMiner.fulfillment_center = false;
        testMiner.vaporLocations = new ArrayDeque<>();
        RobotInfo[] r = new RobotInfo[2];
        testMiner.HQ = new MapLocation(50,50);
        Mockito.doReturn(new MapLocation(4, 4)).when(rcMock).getLocation();
        r[0] = new RobotInfo(0,Team.A, RobotType.DESIGN_SCHOOL, 0, false, 100, 0,0, new MapLocation(0,0));
        r[1] = new RobotInfo(0,Team.A, RobotType.REFINERY, 0, false, 100, 0,0, new MapLocation(1,1));
        Mockito.doReturn(r).when(rcMock).senseNearbyRobots();
        Mockito.doReturn(new MapLocation[0]).when(rcMock).senseNearbySoup();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.FULFILLMENT_CENTER,Direction.SOUTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(new MapLocation(4,4)).when(rcMock).adjacentLocation(Direction.SOUTHWEST);
        assertEquals(4,testMiner.doBuilderThings());
    }

    @Test
    public void minerSoupsUnderLimit() throws GameActionException{
        Mockito.doReturn(new RobotInfo[0]).when(rcMock).senseNearbyRobots();
        MapLocation[] s = new MapLocation[1];
        s[0] = new MapLocation(0,0);
        Mockito.doReturn(s).when(rcMock).senseNearbySoup();
        Mockito.doReturn(0).when(rcMock).getSoupCarrying();
        Mockito.doReturn(new MapLocation(4, 4)).when(rcMock).getLocation();
        assertEquals(3, testMiner.doMinerThings());
    }

    @Test
    public void minerSoupsOverLimit() throws GameActionException{
        testMiner.refineries = new ArrayList<>();
        Mockito.doReturn(new RobotInfo[0]).when(rcMock).senseNearbyRobots();
        MapLocation[] s = new MapLocation[1];
        s[0] = new MapLocation(0,0);
        Mockito.doReturn(s).when(rcMock).senseNearbySoup();
        Mockito.doReturn(500).when(rcMock).getSoupCarrying();
        Mockito.doReturn(new MapLocation(4, 4)).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.REFINERY,Direction.SOUTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(500).when(rcMock).getTeamSoup();
        assertEquals(3, testMiner.doMinerThings());
    }

    @Test
    public void minerNoSoupsOverLimit() throws GameActionException{
        testMiner.refineries = new ArrayList<>();
        Mockito.doReturn(new RobotInfo[0]).when(rcMock).senseNearbyRobots();
        Mockito.doReturn(new MapLocation[0]).when(rcMock).senseNearbySoup();
        Mockito.doReturn(100).when(rcMock).getSoupCarrying();
        Mockito.doReturn(100.0f).when(rcMock).getCooldownTurns();
        Mockito.doReturn(new MapLocation(4, 4)).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.REFINERY,Direction.SOUTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(500).when(rcMock).getTeamSoup();
        assertEquals(4, testMiner.doMinerThings());

    }

    @Test
    public void minerNoSoupsUnderLimit() throws GameActionException {
        testMiner.refineries = new ArrayList<>();
        Mockito.doReturn(new RobotInfo[0]).when(rcMock).senseNearbyRobots();
        Mockito.doReturn(new MapLocation[0]).when(rcMock).senseNearbySoup();
        Mockito.doReturn(0).when(rcMock).getSoupCarrying();
        Mockito.doReturn(100.0f).when(rcMock).getCooldownTurns();
        Mockito.doReturn(new MapLocation(4, 4)).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canBuildRobot(RobotType.REFINERY, Direction.SOUTHWEST);
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(500).when(rcMock).getTeamSoup();
        assertEquals(4, testMiner.doMinerThings());
    }

    @Test
    public void tryMineSuccess() throws GameActionException {

        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(true).when(rcMock).canMineSoup(Direction.EAST);
        Mockito.doNothing().when(rcMock).mineSoup(Direction.EAST);

        boolean res = testMiner.tryMine(Direction.EAST);
        assertEquals(true, res);
    }

    @Test
    public void tryMineFail() throws GameActionException {

        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(false).when(rcMock).canMineSoup(Direction.EAST);

        boolean res = testMiner.tryMine(Direction.EAST);
        assertFalse(res);
    }

    @Test
    public void sendMessageSuccess() throws GameActionException {
        int [] message = new int[1];
        int cost = 10;

        Mockito.doReturn(100).when(rcMock).getTeamSoup();
        Mockito.doReturn(true).when(rcMock).canSubmitTransaction(message, cost);
        Mockito.doNothing().when(rcMock).submitTransaction(message, cost);
        boolean res = testMiner.sendMessage(message,cost);
        assertTrue(res);
    }

    @Test
    public void sendMessageFail() throws GameActionException {
        int [] message = new int[1];
        int cost = 10;

        Mockito.doReturn(100).when(rcMock).getTeamSoup();
        Mockito.doReturn(false).when(rcMock).canSubmitTransaction(message, cost);
        boolean res = testMiner.sendMessage(message,cost);
        assertFalse(res);
    }

    @Test
    public void parseBlockchainWrongTeam() throws GameActionException {
        int cost = 1;
        int [] message = new int [4];
        message[0] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;
        Mockito.doReturn(t).when(rcMock).getBlock(0);

        int res = testMiner.parseBlockchain(0);
        assertEquals(0, res);
    }


    @Test
    public void parseBlockchainHQLoc() throws GameActionException {
        int cost = 1;
        int [] message = new int [6];
        message[0] = 2222; message[1] = 0; message[2] = 0;message[3] = 0;message[4] = 0;message[5] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;
        Mockito.doReturn(t).when(rcMock).getBlock(0);
        int res = testMiner.parseBlockchain(0);
        assertEquals(3, res);
    }

    @Test
    public void parseBlockchainSoupFound() throws GameActionException {
        int cost = 1;
        int [] message = new int [7];
        message[0] = 2222; message[1] = 4; message[2] = 0;message[3] = 0;
        message[4] = 0;message[5] = 0;message[6] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;
        Mockito.doReturn(t).when(rcMock).getBlock(0);
        int res = testMiner.parseBlockchain(0);
        assertEquals(5, res);
    }

    @Test
    public void tryRefineNotReady() throws GameActionException {
        Mockito.doReturn(false).when(rcMock).isReady();
        boolean res = testMiner.tryRefine(Direction.EAST);
        assertFalse(res);
    }

    @Test
    public void tryRefineIsReady() throws GameActionException {
        Direction dir = Direction.EAST;
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(true).when(rcMock).canDepositSoup(dir);
        Mockito.doNothing().when(rcMock).depositSoup(dir,1);
        boolean res = testMiner.tryRefine(dir);
        assertTrue(res);
    }
}
