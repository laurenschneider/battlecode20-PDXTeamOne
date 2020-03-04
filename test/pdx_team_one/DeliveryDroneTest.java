package pdx_team_one;

import battlecode.common.*;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static junit.framework.TestCase.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeliveryDroneTest {

    private RobotController rcMock = mock(RobotController.class);
    //DeliveryDrone testDrone = new DeliveryDrone(rcMock);
/*
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

    /*
    @Test
    public void testHoldingFriendReturn1() throws GameActionException {
        testDrone.HQ = new MapLocation(3,4);
        MapLocation loc = new MapLocation(1,2);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        RobotInfo ri = new RobotInfo(1, Team.A,RobotType.DELIVERY_DRONE,0,false,1,1,2,loc);
        testDrone.setHolding(ri);
        testDrone.addLandscaper(1,loc);
        int res = testDrone.holdingFriend();
        assertEquals(1, res);
    }*/
/*
    @Test
    public void testHoldingFriendReturn2() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        MapLocation locLand = new MapLocation(44,44);
        Mockito.doReturn(loc).when(rcMock).getLocation();

        RobotInfo ri = new RobotInfo(1, Team.A,RobotType.DELIVERY_DRONE,0,false,1,1,2,locLand);
        testDrone.setHolding(ri);
        testDrone.addLandscaper(1,locLand);

        DeliveryDrone dronespy= Mockito.spy(testDrone);
        Mockito.doNothing().when(dronespy).pathTo(locLand);

        int res = testDrone.holdingFriend();
        assertEquals(2, res);
    }

    @Test
    public void testNearbyEnemyTrue() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        MapLocation locLand = new MapLocation(1,2);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).canPickUpUnit(1);
        RobotInfo ri = new RobotInfo(1, Team.A,RobotType.DELIVERY_DRONE,0,false,1,1,2,locLand);

        DeliveryDrone dronespy= Mockito.spy(testDrone);
        Mockito.doNothing().when(dronespy).pathTo(locLand);

        testDrone.HQ = new MapLocation(3,4);
        Mockito.doNothing().when(rcMock).dropUnit(loc.directionTo(testDrone.HQ).opposite());

        boolean res = testDrone.nearbyEnemy(ri);
        assertTrue(res);
    }

    @Test
    public void testNearbyEnemyFalse() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        MapLocation locLand = new MapLocation(44,44);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        RobotInfo ri = new RobotInfo(1, Team.A,RobotType.DELIVERY_DRONE,0,false,1,1,2,locLand);
        DeliveryDrone dronespy= Mockito.spy(testDrone);
        Mockito.doNothing().when(dronespy).pathTo(locLand);
        boolean res = testDrone.nearbyEnemy(ri);
        assertFalse(res);
    }

    @Test
    public void testNearbyLandscapers1() throws GameActionException {
        MapLocation locLand = new MapLocation(44,44);
        Mockito.doReturn(locLand).when(rcMock).getLocation();
        RobotInfo ri = new RobotInfo(1, Team.A,RobotType.DELIVERY_DRONE,0,false,1,1,2,locLand);
        Mockito.doReturn(true).when(rcMock).canPickUpUnit(1);
        Mockito.doNothing().when(rcMock).pickUpUnit(1);
        int res = testDrone.nearbyLandscapers(ri);
        assertEquals(1, res);
    }

    @Test
    public void testNearbyLandscapers2() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        MapLocation locLand = new MapLocation(44,44);
        RobotInfo ri = new RobotInfo(1, Team.A,RobotType.DELIVERY_DRONE,0,false,1,1,2,locLand);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        DeliveryDrone dronespy= Mockito.spy(testDrone);
        Mockito.doNothing().when(dronespy).pathTo(locLand);
        int res = testDrone.nearbyLandscapers(ri);
        assertEquals(2, res);
    }
    */
}
