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

    public void setTank(){
        // Clear existing enemy tanks
        gp2.enemyTanks.clear();

        // Create array for 7 enemy tanks
        gp2.enemyTank = new Enemy_Tank[7];

        // Tank positions (x, y coordinates in tile units)
        int[][] tankPositions = {
                {30, 32}, // Original tank position
                {38, 68}, // Tank 2 - Left side, middle area
                {51, 47}, // Tank 3 - Right side, upper area
                {65, 19}, // Tank 4 - Center, lower area
                {73, 37}, // Tank 5 - Right side, middle area
                {89, 21}, // Tank 6 - Left side, upper area
                {97, 16}  // Tank 7 - Center-right, lower area
        };

        // Create and position all enemy tanks
        for (int i = 0; i < tankPositions.length; i++) {
            int worldX = tankPositions[i][0] * gp2.tileSize;
            int worldY = tankPositions[i][1] * gp2.tileSize;

            Enemy_Tank enemy = new Enemy_Tank(worldX, worldY, 128, 128, 200.0, null, gp2);

            // Add to both array and ArrayList
            gp2.enemyTank[i] = enemy;
            gp2.enemyTanks.add(enemy);

            System.out.println("Enemy tank " + (i + 1) + " created at position: " + worldX + ", " + worldY);
        }

        System.out.println("Total enemy tanks created: " + gp2.enemyTanks.size());
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

        gp.scout[0] = new Scout(43 * gp.tileSize, 12 * gp.tileSize, 40, 80, 2, gp);

    }

    public void setExplosion() {
        gp.eventHandler.addMine(42 * gp.tileSize, 5 * gp.tileSize, 32, 32, 3);
        gp.eventHandler.addMine(24 * gp.tileSize, 16 * gp.tileSize, 32, 32, 2);
    }

    public void setEnemy() {


        // ✅ Corrected to match Game.java's enemies[]
        gp.enemies[0] = new Enemy(43 * gp.tileSize, 2 * gp.tileSize, 50, 80, 0.8, gp);
//        gp.enemies[1] = new Enemy(48 * gp.tileSize, 7 * gp.tileSize, 50, 40, 1, gp);
//        gp.enemies[2] = new Enemy(55 * gp.tileSize, 53 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.enemies[3] = new Enemy(81* gp.tileSize, 54 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.enemies[4] = new Enemy(82 * gp.tileSize, 54 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.enemies[5] = new Enemy(89* gp.tileSize, 48 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.enemies[6] = new Enemy(87 * gp.tileSize, 40 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.enemies[7] = new Enemy(88 * gp.tileSize, 12 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.enemies[8] = new Enemy(95 * gp.tileSize, 6 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.enemies[9] = new Enemy(59 * gp.tileSize, 32 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.enemies[10] = new Enemy(94 * gp.tileSize, 26 * gp.tileSize, 50, 40, 1.2, gp);



    }

    public void setSoldiers() {

        gp.soldiers[0] = new Soldier(46 * gp.tileSize, 19 * gp.tileSize, 50, 50, 1.2, gp);
//        gp.soldiers[1] = new Soldier(21 * gp.tileSize, 52 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[2] = new Soldier(32 * gp.tileSize, 45 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[3] = new Soldier(12 * gp.tileSize, 62 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[4] = new Soldier(23 * gp.tileSize, 64 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[5] = new Soldier(16 * gp.tileSize, 73 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[6] = new Soldier(42 * gp.tileSize, 68 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[7] = new Soldier(60 * gp.tileSize, 71 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[8] = new Soldier(62 * gp.tileSize, 59 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[9] = new Soldier(38 * gp.tileSize, 58 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[10] = new Soldier(24 * gp.tileSize, 68 * gp.tileSize, 50, 40, 1.2, gp);
//        gp.soldiers[11] = new Soldier(10 * gp.tileSize, 36 * gp.tileSize, 50, 40, 1.2, gp);

    }
}