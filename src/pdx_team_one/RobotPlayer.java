package pdx_team_one;
import battlecode.common.*;

//this is the scaffolding code that came with BattleCode
public strictfp class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings({"InfiniteLoopStatement", "unused"})
    public static void run(RobotController rc){

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.

        Robot bot = null;

        //System.out.println("I'm a " + rc.getType() + " and I just got created!");
        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
        try {
            //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
            switch (rc.getType()) {
                case HQ:
                    bot = new HQ(rc);
                    break;
                case MINER:
                    bot = new Miner(rc);
                    break;
                case REFINERY:
                    bot = new Refinery(rc);
                    break;
                case VAPORATOR:
                    bot = new Vaporator(rc);
                    break;
                case DESIGN_SCHOOL:
                    bot = new DesignSchool(rc);
                    break;
                case FULFILLMENT_CENTER:
                    bot = new FulfillmentCenter(rc);
                    break;
                case LANDSCAPER:
                    bot = new Landscaper(rc);
                    break;
                case DELIVERY_DRONE:
                    bot = new DeliveryDrone(rc);
                    break;
                case NET_GUN:
                    bot = new NetGun(rc);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                if (bot != null)
                    bot.takeTurn();
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
