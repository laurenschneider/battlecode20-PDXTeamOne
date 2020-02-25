package pdx_team_one;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.Transaction;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static junit.framework.TestCase.*;

public class MinerTest {
    RobotController rcMock = Mockito.mock(RobotController.class);
    Miner testMiner = new Miner(rcMock) {
        void updateLocalMap(boolean scout) throws GameActionException {
            // do nothing
        }
    };

    public MinerTest() throws GameActionException {}

    @Test
    public void takeTurnTestScout() throws GameActionException {
        testMiner.scout = true;
        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Miner minerspy= Mockito.spy(testMiner);
        Mockito.doReturn(1).when(minerspy).parseBlockchain(1);
        Mockito.doNothing().when(minerspy).doScoutThings();
        minerspy.takeTurn();
    }

    @Test
    public void takeTurnTestMine() throws GameActionException {
        Mockito.doReturn(2).when(rcMock).getRoundNum();
        Miner minerspy= Mockito.spy(testMiner);
        Mockito.doReturn(1).when(minerspy).parseBlockchain(1);
        Mockito.doNothing().when(minerspy).doMinerThings();
        minerspy.takeTurn();
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
    public void parseBlockchainDesignSchoolBuilt() throws GameActionException {
        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222; message[1] = 2;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;
        Mockito.doReturn(t).when(rcMock).getBlock(0);
        int res = testMiner.parseBlockchain(0);
        assertEquals(1, res);
    }

    @Test
    public void parseBlockchainFCenterBuilt() throws GameActionException {
        int cost = 1;
        int [] message = new int [5];
        message[0] = 2222; message[1] = 6;
        int id = 3;
        Transaction t1 = new Transaction(cost,message,id);
        Transaction [] t = new Transaction[1];
        t[0] = t1;
        Mockito.doReturn(t).when(rcMock).getBlock(0);
        int res = testMiner.parseBlockchain(0);
        assertEquals(2, res);
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
