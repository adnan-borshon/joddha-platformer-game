package platformgame;

import platformgame.Entity.Enemy;
import platformgame.Entity.Npc;
import platformgame.Entity.Scout;
import platformgame.Entity.Soldier;
import platformgame.Objects.*;
import platformgame.Tanks.Enemy_Tank;

public class AssetSetter {
    Game gp;
    Game_2 gp2;

    public AssetSetter(Game gp) {
        this.gp = gp;
    }

    public AssetSetter(Game_2 gp2) {
        this.gp2 = gp2;
    }

    // FIXED: Properly setting up enemy tanks
    public void setTank(){
        // Clear existing enemy tanks
        gp2.enemyTanks.clear();

        // Create one enemy tank at a fixed position
        Enemy_Tank enemy = new Enemy_Tank(28 * gp2.tileSize, 29 * gp2.tileSize, 128, 128, 200.0, null, gp2);

        // Add to both the array (for backward compatibility) and ArrayList (for proper collision)
        gp2.enemyTank = new Enemy_Tank[1];
        gp2.enemyTank[0] = enemy;
        gp2.enemyTanks.add(enemy);

        System.out.println("Enemy tank created at position: " + (28 * gp2.tileSize) + ", " + (29 * gp2.tileSize));
    }

    public void setObject() {
        gp.object[5] = new Obj_Boots();
        gp.object[5].worldX = 36 * gp.tileSize;
        gp.object[5].worldY = 5 * gp.tileSize;

        gp.object[7] = new Obj_ammo();
        gp.object[7].worldX = 38 * gp.tileSize;
        gp.object[7].worldY = 10 * gp.tileSize;

        gp.object[8] = new Obj_ammo();
        gp.object[8].worldX = 60 * gp.tileSize;
        gp.object[8].worldY = 48 * gp.tileSize;

        gp.object[9] = new Obj_booth();
        gp.object[9].worldX = 54 * gp.tileSize;
        gp.object[9].worldY = 22 * gp.tileSize;

        gp.object[0] = new Obj_Life();
        gp.object[0].name = "obj";
        gp.object[0].worldX = 58 * gp.tileSize;
        gp.object[0].worldY = 36 * gp.tileSize;

        gp.object[1] = new Obj_Life();
        gp.object[1].name = "obj";
        gp.object[1].worldX = 24 * gp.tileSize;
        gp.object[1].worldY = 23 * gp.tileSize;

        gp.object[2] = new Obj_Life();
        gp.object[2].name = "obj";
        gp.object[2].worldX = 27 * gp.tileSize;
        gp.object[2].worldY = 53 * gp.tileSize;

        gp.object[3] = new Obj_Life();
        gp.object[3].name = "obj";
        gp.object[3].worldX = 54 * gp.tileSize;
        gp.object[3].worldY = 63 * gp.tileSize;
    }

    public void setNpc() {
        // First NPC with default dialogue (this will use the setDialogue() method)
        gp.npc[0] = new Npc(30 * gp.tileSize, 10 * gp.tileSize, 40, 40, 1, gp);

        // Second NPC with custom dialogue
        String[] npc2Dialogue = {
                "Welcome to the safe zone, soldier!",
                "You can rest here and resupply.",
                "Check your ammo before moving forward.",
                "Good luck on your mission!"
        };
        gp.npc[1] = new Npc(38 * gp.tileSize, 20 * gp.tileSize, 40, 40, 1, gp, npc2Dialogue);
    }

    public void setScout() {
        gp.scout[0] = new Scout(43 * gp.tileSize, 12 * gp.tileSize, 40, 40, 2, gp);
    }

    public void setExplosion() {
        gp.eventHandler.addMine(42 * gp.tileSize, 5 * gp.tileSize, 32, 32, 3);
        gp.eventHandler.addMine(24 * gp.tileSize, 16 * gp.tileSize, 32, 32, 2);
    }

    public void setEnemy() {
        // This is now redundant as we are setting only one enemy tank in setTank()
        gp.enemies[0]=new Enemy(32* gp.tileSize,2* gp.tileSize, 50,50, 1,gp);
    }

    public void setSoldiers() {
        gp.soldiers[0] = new Soldier(46 * gp.tileSize, 19 * gp.tileSize, 50, 40, 1.2, gp);
    }
}