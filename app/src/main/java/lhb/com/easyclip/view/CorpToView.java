package lhb.com.easyclip.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

@SuppressLint("AppCompatCustomView")
public class CorpToView extends ImageView {

    private static final int NONE = 0;

    private static final int DRAG = 1;

    private static final int ZOOM = 2;

    private int mood = NONE;

    private Matrix matrix = new Matrix();
    private Matrix currentMatrix = new Matrix();

    private PointF startPoint = new PointF();
    private PointF lastPoint = new PointF();

    private PointF centerPointForZoom;

    private float twoFingerDistanceBeforeZoom;

    private RectF viewRectF;

    private String mImagePath;

    private Bitmap mBmpToCrop;

    private RectF clipRect;

    private Paint clipCirclePaint;
    private Paint clipCircleBorderPaint;
    private Xfermode xfermode;

    private int touchPosition;

    private boolean isClip = false;

    private boolean isFirstDraw = true;

    public CorpToView(Context context) {
        super(context);
        init(context);
    }

    public CorpToView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        Paint mBmpPaint;
        mBmpPaint = new Paint();
        mBmpPaint.setAntiAlias(true);
        mBmpPaint.setFilterBitmap(true);

        clipCirclePaint = new Paint();
        clipCirclePaint.setAntiAlias(true);


        clipCircleBorderPaint = new Paint();
        clipCircleBorderPaint.setStyle(Style.STROKE);
        clipCircleBorderPaint.setColor(Color.parseColor("#4D575F"));
        clipCircleBorderPaint.setStrokeWidth(3);
        clipCircleBorderPaint.setAntiAlias(true);
        xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBmpToCrop == null) return;
        if(isFirstDraw) {
            setViewRectF();
            setClipRectDefaultPosition();
            setImageDefaultPosition();
            isFirstDraw = false;
        }

        if (mBmpToCrop != null) {

            canvas.drawBitmap(mBmpToCrop, matrix, null);
            currentMatrix.set(matrix);

            if(!isClip) {
                //通过Xfermode的DST_OUT来产生中间的透明裁剪区域，一定要另起一个Layer（层）
                canvas.saveLayer(0, 0, this.getWidth(), this.getHeight(), null, Canvas.ALL_SAVE_FLAG);
                canvas.drawColor(Color.parseColor("#6F010101")); //#a8000000

                //中间的透明的圆
                clipCirclePaint.setXfermode(xfermode);
                canvas.drawCircle(this.clipRect.centerX(), this.clipRect.centerY(), this.getHeight()/2, clipCirclePaint);

                //白色的圆边框
                canvas.drawCircle(this.clipRect.centerX(), this.clipRect.centerY(), this.getHeight()/2, clipCircleBorderPaint);
            }
        }
    }

    private boolean isInClipCircle(Matrix myMatrix) {

        float[] myMatrixvalues = new float[9];
        myMatrix.getValues(myMatrixvalues);
        int imageOutClipLeft = Math.round(myMatrixvalues[2]);
        int imageOutClipTop =  Math.round(myMatrixvalues[5]);

        System.out.println("[isInClipCircle中matirx]的left:" + imageOutClipLeft);
        System.out.println("[isInClipCircle中matirx]的top::" + imageOutClipTop);

        float scale = myMatrixvalues[0];
        int imageOUtClipBootom = Math.round(mBmpToCrop.getHeight() * scale) - Math.abs(imageOutClipTop) - Math.round(clipRect.height());
        int imageOUtClipRight = Math.round(mBmpToCrop.getWidth()* scale) - Math.abs(imageOutClipLeft) - Math.round(clipRect.width());

        System.out.println("======================================================");
        System.out.println("[isInClipCircle中matirx]的Right:" + imageOUtClipRight);
        System.out.println("[isInClipCircle中matirx]的Bootem:" + imageOUtClipBootom);

        if(imageOutClipTop > 0) {
            return true;
        }

        if(imageOutClipLeft > 0) {
            return true;
        }

        if(imageOUtClipBootom < 0) {
            return true;
        }

        if(imageOUtClipRight < 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                mood = DRAG;
                startPoint.set(event.getX(), event.getY());
                lastPoint.set(event.getX(),event.getY());

                currentMatrix.set(matrix);
                break;
            case MotionEvent.ACTION_MOVE :
                if (mood == DRAG){
                    moveImage(event);
                }else if (mood == ZOOM){

                  scaleImage(event);
                }

                lastPoint.set(event.getX(),event.getY());
                break;
            case MotionEvent.ACTION_UP:
                mood = NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mood = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mood = ZOOM;
                twoFingerDistanceBeforeZoom = calculateFingersSlideDistance(event);
                if (twoFingerDistanceBeforeZoom > 10f) {
                    centerPointForZoom = calculateCenterPointForZoom(event);
                    currentMatrix.set(matrix);
                }
                break;
        }

        if(!isInClipCircle(matrix)){
            invalidate();
        }
        return true;
    }

    /**
     * show the origin image by the image path that user give
     * @param picPath
     */
    public void showImage(String picPath) {
        this.mImagePath = picPath;
        mBmpToCrop = BitmapFactory.decodeFile(mImagePath);
        invalidate();
    }

    public void showImage(Bitmap bitmap) {
        mBmpToCrop =bitmap;
        invalidate();
    }

    public void rotate90(){
        //matrix.setRotate(90,mBmpToCrop.getWidth()/2,mBmpToCrop.getHeight()/2);

        matrix.setRotate(90,mBmpToCrop.getWidth()/2,mBmpToCrop.getHeight()/2);
        Bitmap bitmap = Bitmap.createBitmap(mBmpToCrop,0,0,mBmpToCrop.getWidth(),mBmpToCrop.getHeight(),matrix,true);
        mBmpToCrop = bitmap;
        Matrix matrix1 = new Matrix();
        matrix.set(matrix1);
        setImageDefaultPosition();
        invalidate();
    }

    private void setViewRectF() {
        viewRectF = new RectF();
        viewRectF.left = 0;
        viewRectF.top = 0;
        viewRectF.right =  getWidth();
        viewRectF.bottom = getHeight();
    }

    private void setClipRectDefaultPosition() {
        final float CLIP_RECT_WIDTH = 200f;
        final float CLIP_RECT_HEIGHT = 200f;

        clipRect = new RectF();

      /*  clipRect.left = (viewRectF.width() - CLIP_RECT_WIDTH) / 2;
        clipRect.top = (viewRectF.height() - CLIP_RECT_HEIGHT) / 2;
        clipRect.right = clipRect.left + CLIP_RECT_WIDTH;;
        clipRect.bottom = clipRect.top + CLIP_RECT_HEIGHT;*/

        clipRect.left = viewRectF.left;
        clipRect.top = viewRectF.top;
        clipRect.right = viewRectF.right;
        clipRect.bottom = viewRectF.bottom;

        System.out.println("裁剪框的left:" + clipRect.left);
        System.out.println("裁剪框的top:" + clipRect.top);

    }

    private void setImageDefaultPosition () {

        float dx = 0;
        float dy = 0;
        // image less than clip circle
        if(mBmpToCrop.getWidth() < viewRectF.width()) {
            dx = (viewRectF.width() - mBmpToCrop.getWidth()) / 2;
        }

        if(mBmpToCrop.getHeight() < viewRectF.height()) {
            dy = (viewRectF.height() - mBmpToCrop.getHeight()) / 2;
        }

        // image bigger than clip circle
        if(mBmpToCrop.getWidth()>=viewRectF.width() && mBmpToCrop.getHeight()>=viewRectF.height()) {
            dx = (viewRectF.width() - mBmpToCrop.getWidth()) / 2;
            dy = (viewRectF.height() - mBmpToCrop.getHeight()) / 2;
        }

        System.out.println("center dx:" + dx);
        System.out.println("center dy:" + dy);

        matrix.postTranslate(Math.round(dx),Math.round(dy));
    }

    /**
     * get the image in the clip rect area
     * @return
     */
    public Bitmap getClipRectImage() {
        isClip = true;
        destroyDrawingCache();
        setDrawingCacheEnabled(true);
        buildDrawingCache();
        Bitmap mBitmap  = getDrawingCache();
        isClip = false;
        return Bitmap.createBitmap(mBitmap, (int) clipRect.left, (int) clipRect.top, (int) clipRect.width(), (int) clipRect.height());
    }

    public void showCilpRectImage() {

        isClip = true;
        destroyDrawingCache();
        setDrawingCacheEnabled(true);
        buildDrawingCache();
        Bitmap mBitmap  = getDrawingCache();

        mBmpToCrop =Bitmap.createBitmap(mBitmap, (int) clipRect.left, (int) clipRect.top, (int) clipRect.width(), (int) clipRect.height());
        matrix = new Matrix();
        isClip = false;
        invalidate();
    }

    private void moveImage (MotionEvent event) {


        int dx = Math.round(event.getX() - startPoint.x);
        int dy = Math.round(event.getY() - startPoint.y);


        Matrix myMatrix = new Matrix();
        myMatrix.set(matrix);
        myMatrix.postTranslate(Math.round(dx), Math.round(dy));
        if(isInClipCircle(myMatrix)) {
            return;
        }

        matrix.set(currentMatrix);
        matrix.postTranslate(Math.round(dx), Math.round(dy));


        startPoint.x = event.getX() ;
        startPoint.y = event.getY() ;
    }

    private PointF calculateCenterPointForZoom(MotionEvent event) {
        float midx = event.getX(1) + event.getX(0);
        float midy = event.getY(1) + event.getY(0);
        return new PointF(midx/2,midy/2);
    }

    private float calculateFingersSlideDistance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    private void scaleImage(MotionEvent event) {

        float twoFingerDistanceAfterZoom = calculateFingersSlideDistance(event);
        if (twoFingerDistanceAfterZoom > 10f) {
            float scale = twoFingerDistanceAfterZoom / twoFingerDistanceBeforeZoom;


            Matrix myMatrix = new Matrix();
            myMatrix.set(matrix);
            myMatrix.postScale(scale, scale, centerPointForZoom.x, centerPointForZoom.y);
            if(isInClipCircle(myMatrix)) {
                return;
            }

            matrix.set(currentMatrix);
            matrix.postScale(scale, scale, centerPointForZoom.x, centerPointForZoom.y);

        }
    }
}