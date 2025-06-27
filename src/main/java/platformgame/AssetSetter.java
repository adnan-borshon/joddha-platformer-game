package platformgame;

import platformgame.Entity.Enemy;
import platformgame.Entity.Npc;
import platformgame.Entity.Scout;
import platformgame.Entity.Soldier;
import platformgame.Objects.*;

public class AssetSetter {
    Game gp;

    public AssetSetter(Game gp) {
        this.gp = gp;
    }

    public AssetSetter(Game_2 game2) {
    }

    public void setObject() {
        gp.object[0] = new Obj_Key();
        gp.object[0].worldX = 48 * gp.tileSize;
        gp.object[0].worldY = 50 * gp.tileSize;



        gp.object[3] = new Obj_Door();
        gp.object[3].worldX = 62 * gp.tileSize;
        gp.object[3].worldY = 58 * gp.tileSize;

        gp.object[4] = new Obj_Door();
        gp.object[4].worldX = 60 * gp.tileSize;
        gp.object[4].worldY = 54 * gp.tileSize;

        gp.object[5] = new Obj_Boots();
        gp.object[5].worldX = 50 * gp.tileSize;
        gp.object[5].worldY = 48 * gp.tileSize;

        gp.object[6] = new Obj_Key();
        gp.object[6].worldX = 27 * gp.tileSize;
        gp.object[6].worldY = 10 * gp.tileSize;

        gp.object[7] = new Obj_ammo();
        gp.object[7].worldX = 38 * gp.tileSize;
        gp.object[7].worldY = 10 * gp.tileSize;

        gp.object[8] = new Obj_ammo();
        gp.object[8].worldX = 60 * gp.tileSize;
        gp.object[8].worldY = 48 * gp.tileSize;

        gp.object[9] = new Obj_booth();
        gp.object[9].worldX = 54 * gp.tileSize;
        gp.object[9].worldY = 22 * gp.tileSize;

        gp.object[1] =new Obj_Life();
        gp.object[1].worldX = 25 * gp.tileSize;
        gp.object[1].worldY = 21 * gp.tileSize;


    }

    public void setNpc() {
        gp.npc[0] = new Npc(30 * gp.tileSize, 10 * gp.tileSize, 40, 40, 1, gp);
    }

    public void setScout() {
        gp.scout[0] = new Scout(43 * gp.tileSize, 12 * gp.tileSize, 40, 40, 2, gp);
    }

    public void setExplosion() {
        gp.eventHandler.addMine(42 * gp.tileSize, 5 * gp.tileSize, 32, 32, 3);
        gp.eventHandler.addMine(24 * gp.tileSize, 16 * gp.tileSize, 32, 32, 2);
    }

    public void setEnemy() {
        // ✅ Corrected to match Game.java's enemies[]
        gp.enemies[0] = new Enemy(38 * gp.tileSize, 4 * gp.tileSize, 50, 40, 0.8, gp);
//        gp.enemies[1] = new Enemy(45 * gp.tileSize, 8 * gp.tileSize, 50, 40, 1, gp);
//        gp.enemies[2] = new Enemy(50 * gp.tileSize, 12 * gp.tileSize, 50, 40, 1.2, gp);


        // Add more if needed
    }


    public void setSoldiers(){
        gp.soldiers[0]= new Soldier(36 * gp.tileSize, 19 * gp.tileSize, 50, 40, 1.2, gp);
    }

}
