package pdx_team_one;

import battlecode.common.*;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;

public class DesignSchoolTest {
    private RobotController rcMock = mock(RobotController.class);
    DesignSchool testSchool = new DesignSchool(rcMock);

    @Test
    public void takeTurnReturnWhenGreaterThanEight() throws GameActionException {
        testSchool.numLS = 8;
        testSchool.takeTurn();
    }

    @Test
    public void takeTurnIncrementLS() throws GameActionException {
        DesignSchool schoolSpy = Mockito.spy(testSchool);

        Mockito.doReturn(true).when(schoolSpy).tryBuild(RobotType.LANDSCAPER, Direction.EAST);
        schoolSpy.takeTurn();

//        assertEquals(1,testSchool.numLS);
    }


}
