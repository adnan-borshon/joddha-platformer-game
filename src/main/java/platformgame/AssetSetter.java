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
        gp.object[0] = new Obj_Boots();
        gp.object[0].worldX = 36 * gp.tileSize;
        gp.object[0].worldY = 5 * gp.tileSize;

        gp.object[1] = new Obj_booth();
        gp.object[1].worldX = 54 * gp.tileSize;
        gp.object[1].worldY = 22 * gp.tileSize;

//key
        gp.object[2] = new Obj_Key();
        gp.object[2].worldX = 68 * gp.tileSize;
        gp.object[2].worldY = 7 * gp.tileSize;
        gp.object[3] = new Obj_Key();
        gp.object[3].worldX = 4 * gp.tileSize;
        gp.object[3].worldY = 60 * gp.tileSize;
        gp.object[3] = new Obj_Key();
        gp.object[3].worldX = 81 * gp.tileSize;
        gp.object[3].worldY = 55 * gp.tileSize;

        //key opener
        gp.object[4] = new Obj_key_opener();
        gp.object[4].worldX = 15 * gp.tileSize;
        gp.object[4].worldY = 62 * gp.tileSize;

        gp.object[5] = new Obj_key_opener();
        gp.object[5].worldX = 67 * gp.tileSize;
        gp.object[5].worldY = 49 * gp.tileSize;

        gp.object[6] = new Obj_key_opener();
        gp.object[6].worldX = 80 * gp.tileSize;
        gp.object[6].worldY = 33 * gp.tileSize;

        //granade

        gp.object[7] = new Obj_granade();
        gp.object[7].worldX = 44 * gp.tileSize;
        gp.object[7].worldY = 9 * gp.tileSize;

        gp.object[8] = new Obj_granade();
        gp.object[8].worldX = 92 * gp.tileSize;
        gp.object[8].worldY = 9 * gp.tileSize;

        //granade launcher

        gp.object[9] = new Obj_granade_launcher();
        gp.object[9].worldX = 68 * gp.tileSize;
        gp.object[9].worldY = 7 * gp.tileSize;

        gp.object[10] = new Obj_granade_launcher();
        gp.object[10].worldX = 90 * gp.tileSize;
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
//public void setLauncherAndOpener(){
//
//        //key opener
//        gp.Opener[0]=new Obj_key_opener();
//        gp.Opener[0].worldX=15*gp.tileSize;
//        gp.Opener[0].worldY=62*gp.tileSize;
//    gp.Opener[1]=new Obj_key_opener();
//    gp.Opener[1].worldX=67*gp.tileSize;
//    gp.Opener[1].worldY=49*gp.tileSize;
//
//    gp.Opener[2]=new Obj_key_opener();
//    gp.Opener[2].worldX=80*gp.tileSize;
//    gp.Opener[2].worldY=33*gp.tileSize;
//
//    //granade launcher
//
//    gp.Launcher[0]= new Obj_granade_launcher();
//    gp.Launcher[0].worldX=63*gp.tileSize;
//    gp.Launcher[0].worldY=26*gp.tileSize;
//
//    gp.Launcher[1]= new Obj_granade_launcher();
//    gp.Launcher[1].worldX=90*gp.tileSize;
//    gp.Launcher[1].worldY=45*gp.tileSize;
//}


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
        // IMPROVED: Better spacing between enemies to prevent clustering
        gp.enemies[0] = new Enemy(17 * gp.tileSize, 25 * gp.tileSize, 50, 50, 1.2, gp);
        gp.enemies[1] = new Enemy(11 * gp.tileSize, 36 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[2] = new Enemy(36 * gp.tileSize, 42 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[3] = new Enemy(25 * gp.tileSize, 57 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[4] = new Enemy(33 * gp.tileSize, 71 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[5] = new Enemy(57 * gp.tileSize, 59 * gp.tileSize, 50, 40, 1.2, gp);
        gp.enemies[6] = new Enemy(58 * gp.tileSize, 28 * gp.tileSize, 50, 40, 1.2, gp);
    }

    public void setSoldiers() {
        // IMPROVED: Better distributed positions and added validation
        int[][] soldierPositions = {
                {19, 63}, {22, 68}, {39, 59}, {59, 43}, {80, 54},
                {68, 18}, {66, 34}, {84, 42}, {66, 58}, {93, 6},
                {85, 12}, {93, 24}
        };

        // Create soldiers with validation
        for (int i = 0; i < soldierPositions.length && i < gp.soldiers.length; i++) {
            double x = soldierPositions[i][0] * gp.tileSize;
            double y = soldierPositions[i][1] * gp.tileSize;

            // Add small random offset to prevent exact overlap
            x += (Math.random() - 0.5) * gp.tileSize * 0.5; // ±25% of tile size
            y += (Math.random() - 0.5) * gp.tileSize * 0.5;

            gp.soldiers[i] = new Soldier(x, y, 50, 40, 1.2, gp);

            // DEBUG: Print soldier creation
            System.out.println("Created soldier " + i + " at position: " + x + ", " + y);
        }
    }

    // NEW: Method to validate entity placement and fix overlaps
    public void validateEntityPlacement() {
        System.out.println("Validating entity placement...");

        // Check soldiers
        for (int i = 0; i < gp.soldiers.length; i++) {
            if (gp.soldiers[i] != null) {
                // Check if this soldier overlaps with others
                for (int j = i + 1; j < gp.soldiers.length; j++) {
                    if (gp.soldiers[j] != null) {
                        double distance = Math.hypot(
                                gp.soldiers[i].getX() - gp.soldiers[j].getX(),
                                gp.soldiers[i].getY() - gp.soldiers[j].getY()
                        );

                        if (distance < gp.tileSize) {
                            System.out.println("WARNING: Soldiers " + i + " and " + j + " are too close!");
                            // Slightly move the second soldier
                            gp.soldiers[j].setPosition(
                                    gp.soldiers[j].getX() + gp.tileSize,
                                    gp.soldiers[j].getY()
                            );
                        }
                    }
                }
            }
        }
    }
}