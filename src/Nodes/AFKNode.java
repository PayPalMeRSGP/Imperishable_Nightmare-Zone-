package Nodes;

import ScriptClasses.PublicStaticFinalConstants;
import org.osbot.rs07.Bot;
import org.osbot.rs07.api.Inventory;
import org.osbot.rs07.api.Tabs;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.ui.Message;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.listener.MessageListener;
import org.osbot.rs07.script.MethodProvider;

import java.util.concurrent.ThreadLocalRandom;

public class AFKNode implements ExecutableNode, MessageListener, Comparable<ExecutableNode> {
    private final int BASE_STARTING_KEY = 2;
    private int currentKey = BASE_STARTING_KEY;
    private final static String DRINK = "Drink";
    private final static String GUZZLE = "Guzzle";

    private static AFKNode AFKNodeSingleton;

    private int hpRegenLimit; //determines when to guzzle back to 1
    private int potionMinBoost; //if using super ranging, determines when to re-pot

    private AFKNode(){}

    public static AFKNode getMainAFKNodeInstance(){
        if(AFKNodeSingleton == null){
            AFKNodeSingleton = new AFKNode();
            Bot bot = PublicStaticFinalConstants.hostScriptReference.getBot();
            bot.addMessageListener(AFKNodeSingleton);
            AFKNodeSingleton.hpRegenLimit = ThreadLocalRandom.current().nextInt(2, 6); //generate initial random hp limit, this variable is used by handlePotionsAndHP() to determine when to reguzzle back to 1 hp
            AFKNodeSingleton.potionMinBoost = ThreadLocalRandom.current().nextInt(3, 7); //generate potion min boost, used to determine next re-pot
        }
        return AFKNodeSingleton;
    }


    //AFK node does not toggle prayer
    //This Node seeks to emulate a human player leaving his account afk in nmz thereby letting his hp regen up to a random amount (hpRegenLimit)
    //then coming back and guzzling rockcakes back to 1 hp before afking again.
    @Override
    public int executeNodeAction() throws InterruptedException {
        if(isPlayerNotInNMZ()){
            PublicStaticFinalConstants.hostScriptReference.log("died in NMZ, stopping script");
            MethodProvider.sleep(10000);
            PublicStaticFinalConstants.hostScriptReference.stop();
        }
        handleAbsorptionLvl();
        handlePotionsAndHP();
        int sleepTime = (int) PublicStaticFinalConstants.randomNormalDist(15000, 3000);
        PublicStaticFinalConstants.hostScriptReference.log("The next onLoop() call executes in " + sleepTime/1000 + " seconds");
        return sleepTime;
    }

    private boolean isPlayerNotInNMZ(){
        Position currentPos = PublicStaticFinalConstants.hostScriptReference.myPosition();
        return PublicStaticFinalConstants.OUTSIDE_NMZ.contains(currentPos);
    }

    private void togglePrayer() throws InterruptedException {
        int currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
        if(currentHealth == 1){
            int currentPrayerPts = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.PRAYER);
            if(currentPrayerPts > 0){
                RS2Widget prayerWidget = PublicStaticFinalConstants.hostScriptReference.getWidgets().get(160,14);
                if(prayerWidget.interact(true, "Activate")){
                    MethodProvider.sleep(PublicStaticFinalConstants.randomNormalDist(2000, 200));
                    prayerWidget.interact(true, "Deactivate");
                }
            }
        }
    }

    private void handlePotionsAndHP() throws InterruptedException {
        openInventoryTab();
        int currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
        if(currentHealth >= hpRegenLimit){
            if(!drinkOverload()){
                PublicStaticFinalConstants.hostScriptReference.log("Did not drink Overload");
            }
            guzzleRockCakeTo1();
            PublicStaticFinalConstants.hostScriptReference.getMouse().moveOutsideScreen();
            hpRegenLimit = ThreadLocalRandom.current().nextInt(2, 5); //generate next random hp limit

        }

    }

    private boolean drinkSuperRangingPotion(){
        openInventoryTab();
        int currentRangeBoost = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.RANGED) - PublicStaticFinalConstants.hostScriptReference.getSkills().getStatic(Skill.RANGED);
        if(doesPlayerHaveSuperRangePotsLeft() && currentRangeBoost < potionMinBoost){
            Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
            return inv.interact(PublicStaticFinalConstants.DRINK, PublicStaticFinalConstants.SUPER_RANGING_4_ID, PublicStaticFinalConstants.SUPER_RANGING_3_ID,
                    PublicStaticFinalConstants.SUPER_RANGING_2_ID, PublicStaticFinalConstants.SUPER_RANGING_1_ID);
        }
        return false;

    }

    private boolean drinkOverload() throws InterruptedException {
        openInventoryTab();
        int currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
        if(currentHealth > 50 && doesPlayerHaveOverloadsLeft() && doesPlayerHaveAbsorptionsLeft()){
            PublicStaticFinalConstants.hostScriptReference.log("We can drink overloads");
            int startingHealth = currentHealth;
            Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
            boolean drankOverload = inv.interact(DRINK, PublicStaticFinalConstants.OVERLOAD_POTION_1_ID, PublicStaticFinalConstants.OVERLOAD_POTION_2_ID,
                    PublicStaticFinalConstants.OVERLOAD_POTION_3_ID, PublicStaticFinalConstants.OVERLOAD_POTION_4_ID);

            while(currentHealth > startingHealth - 48 && drankOverload){
                MethodProvider.sleep(100);
                currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
            }
            return drankOverload;
        }
        return false;
    }

    private void openInventoryTab(){
        Tabs tab = PublicStaticFinalConstants.hostScriptReference.getTabs();
        if(tab.getOpen() != Tab.INVENTORY){
            tab.open(Tab.INVENTORY);
        }
    }

    private void guzzleRockCakeTo1() throws InterruptedException {
        int currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
        while(currentHealth > 1){
            Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
            inv.interact(GUZZLE, PublicStaticFinalConstants.DWARVEN_ROCK_CAKE_ID);
            MethodProvider.sleep(PublicStaticFinalConstants.randomNormalDist(PublicStaticFinalConstants.RS_GAME_TICK_MS, 60.0));
            currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
        }
    }

    private boolean handleAbsorptionLvl() throws InterruptedException {
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        int absorptionLvl = getAbsorptionLvl();
        if(absorptionLvl < 150){
            while(absorptionLvl <= 150 && doesPlayerHaveAbsorptionsLeft()){
                PublicStaticFinalConstants.hostScriptReference.log("absorptionLvl: " + absorptionLvl);
                inv.interact(DRINK, PublicStaticFinalConstants.ABSORPTION_POTION_1_ID, PublicStaticFinalConstants.ABSORPTION_POTION_2_ID,
                        PublicStaticFinalConstants.ABSORPTION_POTION_3_ID, PublicStaticFinalConstants.ABSORPTION_POTION_4_ID);
                absorptionLvl = getAbsorptionLvl();
                MethodProvider.sleep(PublicStaticFinalConstants.randomNormalDist(PublicStaticFinalConstants.RS_GAME_TICK_MS, 60.0));
            }
            return true;
        }
        return false;
    }

    private boolean doesPlayerHaveAbsorptionsLeft(){
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        return inv.contains(PublicStaticFinalConstants.ABSORPTION_POTION_1_ID) || inv.contains(PublicStaticFinalConstants.ABSORPTION_POTION_2_ID)
                || inv.contains(PublicStaticFinalConstants.ABSORPTION_POTION_3_ID) || inv.contains(PublicStaticFinalConstants.ABSORPTION_POTION_4_ID);
    }

    private boolean doesPlayerHaveOverloadsLeft(){
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        return inv.contains(PublicStaticFinalConstants.OVERLOAD_POTION_1_ID) || inv.contains(PublicStaticFinalConstants.OVERLOAD_POTION_2_ID)
                || inv.contains(PublicStaticFinalConstants.OVERLOAD_POTION_3_ID) || inv.contains(PublicStaticFinalConstants.OVERLOAD_POTION_4_ID);
    }

    private boolean doesPlayerHaveSuperRangePotsLeft(){
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        return inv.contains(PublicStaticFinalConstants.SUPER_RANGING_1_ID) || inv.contains(PublicStaticFinalConstants.SUPER_RANGING_2_ID)
                || inv.contains(PublicStaticFinalConstants.SUPER_RANGING_3_ID) || inv.contains(PublicStaticFinalConstants.SUPER_RANGING_4_ID);
    }

    private int getAbsorptionLvl() {
        RS2Widget widget = PublicStaticFinalConstants.hostScriptReference.getWidgets().get(202, 1, 9);
        if(widget != null && widget.isVisible() && widget.getMessage() != null)
            return Integer.parseInt(widget.getMessage().replace(",", ""));
        return 0;
    }

    @Override
    public void resetKey() {
        currentKey = BASE_STARTING_KEY;
    }

    @Override
    public void setKey(int key) {
        currentKey = key;
    }

    @Override
    public int getKey() {
        return currentKey;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " currentKey: " + currentKey;
    }

    @Override
    public void onMessage(Message message) throws InterruptedException {
        PublicStaticFinalConstants.hostScriptReference.log(message.getMessage());
        if(message.getType() == Message.MessageType.GAME){
            if(message.getMessage().contains("overload")){
                PublicStaticFinalConstants.hostScriptReference.log("recieved overload worn off msg");
            }
        }
    }

    @Override
    public int compareTo(ExecutableNode o) {
        return this.getKey() - o.getKey();
    }
}

//I can always git revert if shit fucks up. Great Im looking at the
