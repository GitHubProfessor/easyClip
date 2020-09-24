package lhb.com.easyclip;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import lhb.com.easyclip.util.CommonUtils;
import lhb.com.easyclip.view.CorpToView;
import lhb.com.easyclip.view.FileUtil;

import static lhb.com.easyclip.view.FileUtil.getRealFilePathFromUri;

public class MainActivity extends Activity {

    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 104;
    private static final int REQUEST_CODE_CAMERA = 1;
    private CorpToView imageView;
    private File cameraPictureTempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (CorpToView) findViewById(R.id.corpToView1);
        initPermissions(CAMERA);
    }

    public void openCamera(View view) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            // create the file for save camera
            cameraPictureTempFile = new File(FileUtil.checkDirPath(Environment.getExternalStorageDirectory().getPath() + "/image/"), System.currentTimeMillis() + ".jpg");

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //设置7.0中共享文件，分享路径定义在xml/file_paths.xml
                intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // lhb.easyclip.fileProvider与AndroidManifest.xml中一样才行
                Uri contentUri = FileProvider.getUriForFile(MainActivity.this, "lhb.com.easyclip.fileProvider", cameraPictureTempFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            } else {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraPictureTempFile));
            }
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                Uri uri = Uri.fromFile(cameraPictureTempFile);
                String path = getRealFilePathFromUri(this, uri);
                imageView.showImage(path);
                break;
        }
    }

    public void saveImage(View view) {
        Bitmap clipImage;
        clipImage = imageView.getClipRectImage();
        if (clipImage == null) {
            return;
        }

        Uri mSaveUri = Uri.fromFile(new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".png"));

        if (mSaveUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = getContentResolver().openOutputStream(mSaveUri);
                if (outputStream != null) {
                    //将图片变成圆形图片
                    Bitmap roundedBitmap = CommonUtils.transformToCircleBitmap(clipImage);
                    roundedBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                }
            } catch (IOException ex) {
                Log.e("android", "Cannot open file: " + mSaveUri, ex);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            String imageUrl = mSaveUri.getPath();
        }

    }

    public void rotateImage(View view) {
        imageView.rotate90();

    }

    /**
     * 権限を確認してください
     */
    private final static int CAMERA = 0;
    private final static int CAMERA_PERMISSION_CODE = 1;

    @TargetApi(Build.VERSION_CODES.M)
    private void initPermissions(int pFlag) {
        if (pFlag == CAMERA) {
            int version = android.os.Build.VERSION.SDK_INT;
            if (version >= 23) {
                if ((checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);
                }
            }
        }
    }
}