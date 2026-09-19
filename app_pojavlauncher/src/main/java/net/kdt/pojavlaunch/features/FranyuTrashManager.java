package net.kdt.pojavlaunch.features;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;

public final class FranyuTrashManager {
    private static File root(){ return new File(Tools.DIR_GAME_HOME,".franyu_trash"); }
    public static File move(Instance instance) throws IOException {
        File src=instance.getGameDirectory();
        File parent=src.getParentFile();
        if(parent==null)throw new IOException("Invalid instance path");
        File trash=new File(root(),System.currentTimeMillis()+"-"+src.getName());
        if(!root().exists()&&!root().mkdirs())throw new IOException("Unable to create trash");
        if(!src.renameTo(trash))throw new IOException("Unable to move instance to trash");
        try(FileWriter w=new FileWriter(new File(trash,".trash_timestamp"))){w.write(String.valueOf(System.currentTimeMillis()));}
        return trash;
    }
    public static void purgeExpired(){ File[] f=root().listFiles(File::isDirectory); if(f==null)return; long cut=System.currentTimeMillis()-7L*86400000L; for(File x:f)if(x.lastModified()<cut)org.apache.commons.io.FileUtils.deleteQuietly(x); }
    private FranyuTrashManager(){}
}
