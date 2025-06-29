package platformgame;

import javafx.scene.image.Image;
import platformgame.Entity.Enemy;
import platformgame.Entity.Npc;
import platformgame.Entity.Scout;
import platformgame.Entity.Soldier;
import platformgame.Objects.*;
import platformgame.Tanks.Enemy_Tank;
import platformgame.Tanks.Tank2;

public class AssetSetter {
    Game gp;
    Game_2 gp2;

    public AssetSetter(Game gp) {
        this.gp = gp;
    }

    public AssetSetter(Game_2 gp2) {
        this.gp2 = gp2;
    }

    public void setTank() {
        // Clear existing enemy tanks
        gp2.enemyTanks.clear();

        // Create array for 7 enemy tanks
        gp2.enemyTank = new Enemy_Tank[9];

        // Tank positions (x, y coordinates in tile units)
        int[][] tankPositions = {
                {10, 36},
                {26,29 },
                {42, 17},
                {60, 21},
                {38, 67},
                {68, 60},
                {74,36},
                {99,17},
                {99,27},
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
        gp.object[0] = new Obj_Boots();
        gp.object[0].worldX = 36 * gp.tileSize;
        gp.object[0].worldY = 5 * gp.tileSize;

        gp.object[1] = new Obj_booth();
        gp.object[1].worldX = 54 * gp.tileSize;
        gp.object[1].worldY = 22 * gp.tileSize;

//key
        gp.object[2] = new Obj_Key();
        gp.object[2].name = "key1";
        gp.object[2].worldX = 68 * gp.tileSize;
        gp.object[2].worldY = 7 * gp.tileSize;

        gp.object[3] = new Obj_Key();
        gp.object[3].name = "key2";
        gp.object[3].worldX = 4 * gp.tileSize;
        gp.object[3].worldY = 60 * gp.tileSize;
        gp.object[21] = new Obj_Key();
        gp.object[21].name = "key3";
        gp.object[21].worldX = 85 * gp.tileSize;
        gp.object[21].worldY = 55 * gp.tileSize;

        //key opener
        gp.object[4] = new Obj_key_opener();
        gp.object[4].name = "key1_opener";
        gp.object[4].worldX = 14 * gp.tileSize;
        gp.object[4].worldY = 61 * gp.tileSize;

        gp.object[5] = new Obj_key_opener();
        gp.object[5].name = "key2_opener";
        gp.object[5].worldX = 67 * gp.tileSize;
        gp.object[5].worldY = 49 * gp.tileSize;

        gp.object[6] = new Obj_key_opener();
        gp.object[6].name = "key3_opener";
        gp.object[6].worldX = 79 * gp.tileSize;
        gp.object[6].worldY = 33 * gp.tileSize;

        //granade

        gp.object[7] = new Obj_granade();
        gp.object[7].worldX = 44 * gp.tileSize;
        gp.object[7].worldY = 9 * gp.tileSize;

        gp.object[8] = new Obj_granade();
        gp.object[8].name = "bridge_granade";
        gp.object[8].worldX = 92 * gp.tileSize;
        gp.object[8].worldY = 9 * gp.tileSize;

        //granade launcher

        gp.object[9] = new Obj_granade_launcher();
        gp.object[9].worldX = 61 * gp.tileSize;
        gp.object[9].worldY = 24 * gp.tileSize;

        gp.object[10] = new Obj_granade_launcher();
        gp.object[10].name = "bridgeDestruction";
        gp.object[10].worldX = 89 * gp.tileSize;
        gp.object[10].worldY = 45 * gp.tileSize;

// ammonation

        gp.object[11] = new Obj_ammo();
        gp.object[11].worldX = 37 * gp.tileSize;
        gp.object[11].worldY = 12 * gp.tileSize;

        gp.object[12] = new Obj_ammo();
        gp.object[12].worldX = 63 * gp.tileSize;
        gp.object[12].worldY = 9 * gp.tileSize;

        gp.object[13] = new Obj_ammo();
        gp.object[13].worldX = 31 * gp.tileSize;
        gp.object[13].worldY = 50 * gp.tileSize;

        gp.object[14] = new Obj_ammo();
        gp.object[14].worldX = 11 * gp.tileSize;
        gp.object[14].worldY = 58 * gp.tileSize;

        gp.object[15] = new Obj_ammo();
        gp.object[15].worldX = 53 * gp.tileSize;
        gp.object[15].worldY = 51 * gp.tileSize;

        gp.object[16] = new Obj_ammo();
        gp.object[16].worldX = 91 * gp.tileSize;
        gp.object[16].worldY = 19 * gp.tileSize;

        gp.object[17] = new Obj_ammo();
        gp.object[17].worldX = 77 * gp.tileSize;
        gp.object[17].worldY = 42 * gp.tileSize;

        //Life

        gp.object[18] = new Obj_Life();
        gp.object[18].worldX = 38 * gp.tileSize;
        gp.object[18].worldY = 54 * gp.tileSize;

        gp.object[19] = new Obj_Life();
        gp.object[19].worldX = 32 * gp.tileSize;
        gp.object[19].worldY = 25 * gp.tileSize;

        gp.object[20] = new Obj_Life();
        gp.object[20].worldX = 77 * gp.tileSize;
        gp.object[20].worldY = 32 * gp.tileSize;


    }


    public void setNpc() {
        // First NPC with default dialogue (this will use the setDialogue() method)
        Image[] npcDialogue = {
                ImageLoader.load("/Popups/Narration-01.png"), ImageLoader.load("/Popups/Narration-02.png")
        };
        gp.npc[0] = new Npc(30 * gp.tileSize, 10 * gp.tileSize, 40, 40, 1, gp, npcDialogue);


//        gp.npc[1] = new Npc(38 * gp.tileSize, 20 * gp.tileSize, 40, 40, 1, gp, npc2Dialogue);
    }

    public void setScout() {

//        gp.scout[0] = new Scout(43 * gp.tileSize, 12 * gp.tileSize, 40, 80, 2, gp);
        gp.scout[1] = new Scout(40 * gp.tileSize, 11 * gp.tileSize, 40, 80, 2, gp);

    }

    public void setExplosion() {
        gp.eventHandler.addMine(42 * gp.tileSize, 5 * gp.tileSize, 32, 32, 3);
        gp.eventHandler.addMine(24 * gp.tileSize, 16 * gp.tileSize, 32, 32, 2);
    }

    public void setTank2(){
        // Clear existing enemy tanks
        gp2.Tanks.clear();

        // Create array for 7 enemy tanks + 1 canon = 8 total
        gp2.Tanks2 = new Tank2[4];

        // Tank positions (x, y coordinates in tile units)
        int[][] tankPositions = {
                {53, 37},
                {110, 15},
                {88, 39}
//

        };

        // Create regular enemy tanks (first 7)
        for (int i = 0; i < 1
                ; i++) {
            int worldX = tankPositions[i][0] * gp2.tileSize;
            int worldY = tankPositions[i][1] * gp2.tileSize;

            Tank2 enemy = new Tank2(worldX, worldY, 128, 128, 200.0, null, gp2);

            // Add to both array and ArrayList
            gp2.Tanks2[i]=enemy;
            gp2.Tanks.add(enemy);

            System.out.println("Enemy tank " + (i + 1) + " created at position: " + worldX + ", " + worldY);
        }


        System.out.println("Total enemy tanks created: " + gp2.enemyTanks.size());
    }
    public void setEnemy() {
        // IMPROVED: Better spacing between enemies to prevent clustering
        gp.enemies[0] = new Enemy(17 * gp.tileSize, 25 * gp.tileSize, 50, 50, 1.2, gp);
        gp.enemies[1] = new Enemy(11 * gp.tileSize, 36 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[2] = new Enemy(36 * gp.tileSize, 42 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[3] = new Enemy(25 * gp.tileSize, 57 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[4] = new Enemy(33 * gp.tileSize, 71 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[5] = new Enemy(59 * gp.tileSize, 45 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[6] = new Enemy(58 * gp.tileSize, 28 * gp.tileSize, 50, 40, 1.2, gp);
    }

    public void setSoldiers() {
//         IMPROVED: Better distributed positions and added validation
//        int[][] soldierPositions = {
//                {19, 63}, {22, 68}, {39, 59}, {59, 43}, {80, 54},
//                {68, 18}, {66, 34}, {84, 42}, {66, 58}, {93, 6},
//                {85, 12}, {93, 24}
//        };
//
//        // Create soldiers with validation
//        for (int i = 0; i < soldierPositions.length && i < gp.soldiers.length; i++) {
//            double x = soldierPositions[i][0] * gp.tileSize;
//            double y = soldierPositions[i][1] * gp.tileSize;
//
//            // Add small random offset to prevent exact overlap
//            x += (Math.random() - 0.5) * gp.tileSize * 0.5; // ±25% of tile size
//            y += (Math.random() - 0.5) * gp.tileSize * 0.5;
//
//            gp.soldiers[i] = new Soldier(x, y, 50, 40, 1.2, gp);
//
//            // DEBUG: Print soldier creation
//            System.out.println("Created soldier " + i + " at position: " + x + ", " + y);
//        }
    }
}


