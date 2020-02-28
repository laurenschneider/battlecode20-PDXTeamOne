package pdx_team_one;

import battlecode.common.*;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Map;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.mock;

public class LandscaperTest {
    private RobotController rcMock = mock(RobotController.class);
    Landscaper testLandscaper = new Landscaper(rcMock);


    public LandscaperTest() throws GameActionException {
    }

    @Test
    public void takeTurnTestDefend() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        testLandscaper.HQ = new MapLocation(3,4);
        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Landscaper landspy= Mockito.spy(testLandscaper);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doNothing().when(landspy).parseBlockchain(1);
        Mockito.doNothing().when(landspy).defend();
        landspy.takeTurn();
    }

    @Test
    public void takeTurnTestAttack() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        testLandscaper.HQ = new MapLocation(300,400);
        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Landscaper landspy= Mockito.spy(testLandscaper);
        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doNothing().when(landspy).parseBlockchain(1);
        Mockito.doNothing().when(landspy).attack();
        landspy.takeTurn();
    }

    @Test
    public void testParseBlockChainHQ() throws GameActionException {
        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222;
        message[1] = 0; message[2] = 0; message[3] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;

        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Mockito.doReturn(t).when(rcMock).getBlock(1);

        int res = testLandscaper.parseBlockchain();
        assertEquals(res, 1);
    }

    @Test
    public void testParseBlockChainEnemy() throws GameActionException {
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

        int res = testLandscaper.parseBlockchain();
        assertEquals(res, 2);
    }
/*
    @Test
    public void testParseBlockChainTarget() throws GameActionException {
        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222;
        message[1] = 5; message[2] = 0; message[3] = 0;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;

        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Mockito.doReturn(t).when(rcMock).getBlock(1);

        int res = testLandscaper.parseBlockchain();
        assertEquals(res, 3);
    }
    */

    @Test
    public void testAttackWithDirt() throws GameActionException {
        testLandscaper.enemyHQ = new MapLocation(0,1);
        MapLocation loc = new MapLocation(1,2);

        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doReturn(2).when(rcMock).getDirtCarrying();

        testLandscaper.attack();
    }

    @Test
    public void testAttackAdjacentEnemy() throws GameActionException {
        testLandscaper.enemyHQ = new MapLocation(1,1);
        MapLocation loc = new MapLocation(1,1);

        Mockito.doReturn(loc).when(rcMock).getLocation();

        testLandscaper.attack();
    }

    @Test
    public void testAttackElse() throws GameActionException {
        testLandscaper.enemyHQ = new MapLocation(0,1);
        MapLocation loc = new MapLocation(1,2);

        Mockito.doReturn(loc).when(rcMock).getLocation();

        testLandscaper.attack();
    }

    @Test
    public void tryDepositTrue() throws GameActionException {
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(true).when(rcMock).canDepositDirt(Direction.EAST);
        boolean res = testLandscaper.tryDeposit(Direction.EAST);
        assertTrue(res);
    }

    @Test
    public void tryDepositFalse() throws GameActionException {

        boolean res = testLandscaper.tryDeposit(Direction.EAST);
        assertFalse(res);
    }

    @Test
    public void tryDigTrue() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);

        Mockito.doReturn(loc).when(rcMock).getLocation();
        Mockito.doReturn(true).when(rcMock).isReady();
        Mockito.doReturn(true).when(rcMock).canDigDirt(Direction.EAST);

        boolean res = testLandscaper.tryDig(Direction.EAST);
        assertTrue(res);
    }

    @Test
    public void tryDigFalse() throws GameActionException {
        MapLocation loc = new MapLocation(1,2);
        Mockito.doReturn(loc).when(rcMock).getLocation();

        boolean res = testLandscaper.tryDig(Direction.EAST);
        assertFalse(res);
    }
}
