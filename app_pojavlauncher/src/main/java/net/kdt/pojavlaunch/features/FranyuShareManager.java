package net.kdt.pojavlaunch.features;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import net.kdt.pojavlaunch.instances.Instance;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class FranyuShareManager {
    public static String exportCode(Instance i){
        String json="{\"name\":\""+escape(i.name)+"\",\"versionId\":\""+escape(i.versionId)+"\",\"renderer\":\""+escape(i.renderer)+"\",\"jvmArgs\":\""+escape(i.jvmArgs)+"\",\"controlLayout\":\""+escape(i.controlLayout)+"\"}";
        return "FRANYU1:"+Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
    public static Bitmap createQr(String code,int size) throws Exception { BitMatrix m=new MultiFormatWriter().encode(code,BarcodeFormat.QR_CODE,size,size); Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888); for(int x=0;x<size;x++)for(int y=0;y<size;y++)b.setPixel(x,y,m.get(x,y)?Color.BLACK:Color.WHITE); return b; }
    private static String escape(String s){return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"");}
    private FranyuShareManager(){}
}
