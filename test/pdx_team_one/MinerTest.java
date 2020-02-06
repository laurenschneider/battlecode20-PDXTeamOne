package pdx_team_one;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertEquals;

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
        assertEquals(false, res);
    }


}
