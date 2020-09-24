package lhb.com.easyclip.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.MediaPlayer;

import android.os.Vibrator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CommonUtils {

    private static MediaPlayer mMediaPlayer;
    private static Vibrator mVibrator;

    /**
     * SDカードにビットマップを保存
     * @param bitName 保存した名前
     * @param mBitmap 画像オブジェクト
     * return 圧縮画像を生成した後の画像パス
     */
    public static String saveMyBitmap(String bitName, Bitmap mBitmap) {
        File f = new File(bitName );
        try {
            f.createNewFile();
        } catch (IOException e) {
            System.out.println("在保存图片时出错：" + e.toString());
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        } catch (Exception e) {
            return "create_bitmap_error";
        }
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitName;
    }


    public static Bitmap transformToCircleBitmap(Bitmap bitmap) {
        try {
            //获取图片的宽度
            int width = bitmap.getWidth();
            Paint paint = new Paint();
            //设置抗锯齿
            paint.setAntiAlias(true);

            //创建一个与原bitmap一样宽度的正方形bitmap
            Bitmap circleBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            //以该bitmap为低创建一块画布
            Canvas canvas = new Canvas(circleBitmap);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //以（width/2, width/2）为圆心，width/2为半径画一个圆
            canvas.drawCircle(width/2, width/2, width/2, paint);

            //设置画笔为取交集模式
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            //裁剪图片
            canvas.drawBitmap(bitmap, 0, 0, paint);

            return circleBitmap;
        } catch (Exception e) {
            return bitmap;
        }
    }
}
