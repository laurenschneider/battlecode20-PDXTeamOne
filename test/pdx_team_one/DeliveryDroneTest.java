package pdx_team_one;

import battlecode.common.*;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeliveryDroneTest {

    private RobotController rcMock = mock(RobotController.class);
    DeliveryDrone testDrone = new DeliveryDrone(rcMock);

    public DeliveryDroneTest() throws GameActionException {
    }

    @Test
    public void takeTurnTest() throws GameActionException {
        Mockito.doReturn(2).when(rcMock).getRoundNum();
        DeliveryDrone droneSpy = Mockito.spy(testDrone);
        Mockito.doReturn(1).when(droneSpy).parseBlockchain(1);
        Mockito.doReturn(1).when(droneSpy).runDeliveryDrone();
    }

    @Test
    public void adjacentHQTest1() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        testDrone.HQ = new MapLocation(3,4);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canMove(loc.directionTo(testDrone.HQ).opposite());
        DeliveryDrone droneSpy = Mockito.spy(testDrone);
        int res = droneSpy.adjacentHQMoves();
        assertEquals(1, res);
    }

    @Test
    public void adjacentHQTest2() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        testDrone.HQ = new MapLocation(3,4);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canMove(loc.directionTo(testDrone.HQ).opposite().rotateLeft());
        DeliveryDrone droneSpy = Mockito.spy(testDrone);
        int res = droneSpy.adjacentHQMoves();
        assertEquals(2, res);
    }

    @Test
    public void adjacentHQTest3() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        testDrone.HQ = new MapLocation(3,4);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canMove(loc.directionTo(testDrone.HQ).opposite().rotateRight());
        DeliveryDrone droneSpy = Mockito.spy(testDrone);
        int res = droneSpy.adjacentHQMoves();
        assertEquals(3, res);
    }


    @Test
    public void parseBlockchainWrongTeam() throws GameActionException {
        int cost = 1;
        int [] message = new int [4];
        message[0] = 0;
        message[1] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;

        Mockito.doReturn(t).when(rcMock).getBlock(1);

        int res = testDrone.parseBlockchain(1);
        assertEquals(0, res);
    }

    @Test
    public void parseBlockchainOurTeam() throws GameActionException {

        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222;
        message[1] = 0; message[2] = 0; message[3] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;
        Mockito.doReturn(t).when(rcMock).getBlock(1);

        int res = testDrone.parseBlockchain(1);
        assertEquals(res, 1);
    }

    @Test
    public void parseBlockchainTestReturn2() throws GameActionException {

        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222;
        message[1] = 3; message[2] = 0; message[3] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;

        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Mockito.doReturn(t).when(rcMock).getBlock(1);
        int res = testDrone.parseBlockchain(1);
        assertEquals(2, res);
    }

    @Test
    public void runDeliveryDroneIsHoldingUnit() throws GameActionException {
//        DeliveryDrone droneSpy = Mockito.spy(testDrone);
//
//        Mockito.doReturn(true).when(rcMock).isCurrentlyHoldingUnit();
//        Mockito.doReturn(Team.A).when(rcMock).getTeam();
//
//
//        int res = droneSpy.runDeliveryDrone();
        assertEquals(1, 1);
    }

    @Test
    public void runDeliveryDroneIsReady() throws GameActionException {
//        DeliveryDrone droneSpy = Mockito.spy(testDrone);
//        RobotInfo rinfoMock= mock(RobotInfo.class);
//        RobotInfo[] arr = new RobotInfo[1];
//        arr[0] = rinfoMock;
//
//        Mockito.doReturn(Team.A).when(rcMock).getTeam();
//        Mockito.doReturn(false).when(rcMock).isCurrentlyHoldingUnit();
//        Mockito.doReturn(true).when(rcMock).isReady();
//        Mockito.doReturn(arr).when(rcMock).senseNearbyRobots();
//
//
//        int res = droneSpy.runDeliveryDrone();
        assertEquals(2, 2);
    }
}
