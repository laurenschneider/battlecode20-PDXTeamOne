package pdx_team_one;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FulfillmentCenterTest {

    private RobotController rcMock = mock(RobotController.class);
    FulfillmentCenter testCenter = new FulfillmentCenter(rcMock);
/*
    @Test
    public void takeTurnTest() throws GameActionException {
        FulfillmentCenter centerSpy = Mockito.spy(testCenter);

        Mockito.doReturn(true).when(centerSpy).tryBuild(RobotType.DELIVERY_DRONE, Direction.EAST);
        testCenter.takeTurn();

        // note this test is doing nothing
    }
    */
}
