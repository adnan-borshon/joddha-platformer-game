package platformgame;

import platformgame.Entity.Npc;
import platformgame.Entity.Scout;
import platformgame.Objects.Obj_Boots;
import platformgame.Objects.Obj_Door;
import platformgame.Objects.Obj_Key;
import platformgame.Objects.Obj_ammo;
import platformgame.Objects.Obj_booth; // ✅ Added import for booth

public class AssetSetter {
    Game gp;
    public AssetSetter(Game gp){
        this.gp = gp;
    }

    public void setObject(){

        gp.object[0] = new Obj_Key();
        gp.object[0].worldX = 48 * gp.tileSize;
        gp.object[0].worldY = 50 * gp.tileSize;

        gp.object[1] = new Obj_Key();
        gp.object[1].worldX = 50 * gp.tileSize;
        gp.object[1].worldY = 54 * gp.tileSize;

        gp.object[2] = new Obj_Door();
        gp.object[2].worldX = 50 * gp.tileSize;
        gp.object[2].worldY = 52 * gp.tileSize;

        gp.object[6] = new Obj_Key();
        gp.object[6].worldX = 27 * gp.tileSize;
        gp.object[6].worldY = 10 * gp.tileSize;

        gp.object[3] = new Obj_Door();
        gp.object[3].worldX = 62 * gp.tileSize;
        gp.object[3].worldY = 58 * gp.tileSize;

        gp.object[4] = new Obj_Door();
        gp.object[4].worldX = 60 * gp.tileSize;
        gp.object[4].worldY = 54 * gp.tileSize;

        gp.object[5] = new Obj_Boots();
        gp.object[5].worldX = 50 * gp.tileSize;
        gp.object[5].worldY = 48 * gp.tileSize;

        // ✅ Ammo pickups
        gp.object[7] = new Obj_ammo();
        gp.object[7].worldX = 38 * gp.tileSize;
        gp.object[7].worldY = 10 * gp.tileSize;

        gp.object[8] = new Obj_ammo();
        gp.object[8].worldX = 60 * gp.tileSize;
        gp.object[8].worldY = 48 * gp.tileSize;

        // ✅ Telephone Booth (new object)
        gp.object[9] = new Obj_booth();
        gp.object[9].worldX = 54 * gp.tileSize;
        gp.object[9].worldY = 22 * gp.tileSize;

    }

    public void setNpc(){
        gp.npc[0] = new Npc(30 * gp.tileSize, 10 * gp.tileSize, 40, 40, 1, gp);
    }

    public void setScout(){
        gp.scout[0] = new Scout(43 * gp.tileSize, 12 * gp.tileSize, 40, 40, 2, gp);
    }

    public void setExplosion(){
        gp.eventHandler.addMine(42 * gp.tileSize, 5 * gp.tileSize, 32, 32, 3);
        gp.eventHandler.addMine(24 * gp.tileSize, 16 * gp.tileSize, 32, 32, 2);
    }
}
