package pdx_team_one;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.*;

public class HQTest {

    @Mock
    RobotController rcMock;
    HQ hqtest = new HQ(rcMock);

    @Before
    public void setUp() throws GameActionException {
        when(hqtest.tryBuild(RobotType.MINER, Direction.EAST)).thenReturn(true);
    }

    @Test
    public void buildMinersShouldBuild5() throws GameActionException {
        int actual = hqtest.buildMiners();
        assertEquals(1, actual);
    }

    @Test
    public void sendLocationShouldCallSendMessage() throws GameActionException {
        // TODO
    }

}
