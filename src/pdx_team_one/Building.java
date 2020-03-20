package pdx_team_one;
import battlecode.common.*;

//This class is for buildings. The only method is defense() which tells the net guns/HQ which drone to shoot
public abstract class Building extends Robot{
    public Building(RobotController rc){
        super(rc);
    }

    protected void defense() throws GameActionException {
        RobotInfo target = null;
        //get all enemy drones in shooting radius
        for (RobotInfo e : rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, rc.getTeam().opponent())) {
            //target is prioritized by whether or not they're holding a unit followed by the closest drone
            if (rc.canShootUnit(e.ID)) {
                if (target == null)
                    target = e;
                else if (target.isCurrentlyHoldingUnit() == e.isCurrentlyHoldingUnit()) {
                    if (e.location.equals(closestLocation(new MapLocation[]{e.location, target.location})))
                        target = e;
                }
                else if (e.isCurrentlyHoldingUnit())
                    target = e;
            }
        }
        if (target!=null)
            rc.shootUnit(target.ID);
    }
}
