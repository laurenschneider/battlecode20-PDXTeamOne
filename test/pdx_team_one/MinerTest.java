package pdx_team_one;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import org.junit.Test;
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



}
