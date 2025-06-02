package platformgame;

import platformgame.Objects.Obj_Door;
import platformgame.Objects.Obj_Key;

public class AssetSetter {
    Game gp;
    public AssetSetter(Game gp){
        this.gp=gp;
    }

    public void setObject(){

        gp.object[0]=  new Obj_Key();
        gp.object[0].worldX = 48* gp.tileSize;
        gp.object[0].worldY = 50* gp.tileSize;

        gp.object[1]=  new Obj_Key();
        gp.object[1].worldX = 60* gp.tileSize;
        gp.object[1].worldY = 54* gp.tileSize ;

        gp.object[2]=  new Obj_Door();
        gp.object[2].worldX = 50* gp.tileSize;
        gp.object[2].worldY = 52* gp.tileSize ;

        gp.object[3]=  new Obj_Door();
        gp.object[3].worldX = 62* gp.tileSize;
        gp.object[3].worldY = 58* gp.tileSize ;

    }
}
