package Nodes;

import ScriptClasses.PublicStaticFinalConstants;
import org.osbot.rs07.api.Inventory;
import org.osbot.rs07.api.Menu;
import org.osbot.rs07.api.Mouse;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.input.mouse.InventorySlotDestination;
import org.osbot.rs07.script.MethodProvider;

public class PrepNode implements ExecutableNode, Comparable<ExecutableNode> {
    private final int BASE_STARTING_KEY = 1;
    private int currentKey = BASE_STARTING_KEY;
    private static PrepNode prepNodeSingleton;

    private PrepNode(){

    }

    public static PrepNode getPrepNodeInstance(){
        if(prepNodeSingleton == null){
            prepNodeSingleton = new PrepNode();
        }
        return prepNodeSingleton;
    }

    @Override
    public int executeNodeAction() throws InterruptedException {
        drinkAbsorptions();
        setPlayerHealthTo1();
        return 1000;
    }

    private void drinkAbsorptions() throws InterruptedException {
        PublicStaticFinalConstants.hostScriptReference.log("entering drinkAbsorptions");
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        int absorptionLvl = getAbsorptionLvl();
        while(absorptionLvl < 100 && doesPlayerHaveAbsorptionsLeft()){
            PublicStaticFinalConstants.hostScriptReference.log("absorptionLvl: " + absorptionLvl);
            inv.interact(PublicStaticFinalConstants.DRINK, PublicStaticFinalConstants.ABSORPTION_POTION_1_ID, PublicStaticFinalConstants.ABSORPTION_POTION_2_ID, PublicStaticFinalConstants.ABSORPTION_POTION_3_ID, PublicStaticFinalConstants.ABSORPTION_POTION_4_ID);
            absorptionLvl = getAbsorptionLvl();
            MethodProvider.sleep(PublicStaticFinalConstants.randomNormalDist(PublicStaticFinalConstants.RS_GAME_TICK_MS*3, 180));
        }
        PublicStaticFinalConstants.hostScriptReference.log("exiting drinkAbsorptions");
    }



    private void setPlayerHealthTo1() throws InterruptedException {
        PublicStaticFinalConstants.hostScriptReference.log("entering setPlayerHealthTo1");
        int currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
        int estimatedHealthAfterOverload = currentHealth - 49; //49 incase health regenerates 1pt in overload dmg process
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        if(currentHealth >= 50 && doesPlayerHaveOverloadsLeft()){
            inv.interact(PublicStaticFinalConstants.DRINK, PublicStaticFinalConstants.OVERLOAD_POTION_1_ID, PublicStaticFinalConstants.OVERLOAD_POTION_2_ID,
                    PublicStaticFinalConstants.OVERLOAD_POTION_3_ID, PublicStaticFinalConstants.OVERLOAD_POTION_4_ID);
            while(currentHealth > estimatedHealthAfterOverload){ //wait out overload dmg, DO NOT GUZZLE while taking overload dmg, may result in overload dmg player killing player.
                MethodProvider.sleep(500);
                currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
                PublicStaticFinalConstants.hostScriptReference.log("waiting out overload dmg... current hp: " + currentHealth);
            }
        }
        while(currentHealth > 1){
            guzzleRockCake();
            currentHealth = PublicStaticFinalConstants.hostScriptReference.getSkills().getDynamic(Skill.HITPOINTS);
            PublicStaticFinalConstants.hostScriptReference.log("guzzling rockcake... hp: " + currentHealth);
            MethodProvider.sleep(PublicStaticFinalConstants.randomNormalDist(PublicStaticFinalConstants.RS_GAME_TICK_MS, 60.0));
        }
        PublicStaticFinalConstants.hostScriptReference.log("exiting setPlayerHealthTo1");
    }

    private void guzzleRockCake(){
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        Mouse mouse = PublicStaticFinalConstants.hostScriptReference.getMouse();
        Menu rockCakeMenu = PublicStaticFinalConstants.hostScriptReference.getMenuAPI();
        if(inv.contains(PublicStaticFinalConstants.DWARVEN_ROCK_CAKE_ID)){
            int rockCakeInvSlot = inv.getSlot(PublicStaticFinalConstants.DWARVEN_ROCK_CAKE_ID);
            InventorySlotDestination rockCakeDest = new InventorySlotDestination(PublicStaticFinalConstants.hostScriptReference.getBot(), rockCakeInvSlot);
            mouse.click(rockCakeDest, true);
            if(rockCakeMenu.isOpen()){
                rockCakeMenu.selectAction(PublicStaticFinalConstants.GUZZLE);
            }
        }
        
    }

    private boolean doesPlayerHaveOverloadsLeft(){
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        return inv.contains(PublicStaticFinalConstants.OVERLOAD_POTION_1_ID) || inv.contains(PublicStaticFinalConstants.OVERLOAD_POTION_2_ID)
                || inv.contains(PublicStaticFinalConstants.OVERLOAD_POTION_3_ID) || inv.contains(PublicStaticFinalConstants.OVERLOAD_POTION_4_ID);
    }

    private boolean doesPlayerHaveAbsorptionsLeft(){
        Inventory inv = PublicStaticFinalConstants.hostScriptReference.getInventory();
        return inv.contains(PublicStaticFinalConstants.ABSORPTION_POTION_1_ID) || inv.contains(PublicStaticFinalConstants.ABSORPTION_POTION_2_ID)
                || inv.contains(PublicStaticFinalConstants.ABSORPTION_POTION_3_ID) || inv.contains(PublicStaticFinalConstants.ABSORPTION_POTION_4_ID);
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
    public int compareTo(ExecutableNode o) {
        return this.getKey() - o.getKey();
    }
}
