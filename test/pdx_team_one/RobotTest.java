package pdx_team_one;

import battlecode.common.RobotController;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

public class RobotTest {
    RobotController rcMock = mock(RobotController.class);

    @Test
    public void testStart() {
        Mockito.doReturn(1).when(rcMock).getRoundNum();
    }

    @Test
    public void testEnd() {
        Mockito.doReturn(1).when(rcMock).getRoundNum();
    }

}
