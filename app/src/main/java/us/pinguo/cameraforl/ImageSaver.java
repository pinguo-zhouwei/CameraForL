package us.pinguo.cameraforl;

import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 保存图片
 * Created by zhouwei on 14-10-24.
 */
public class ImageSaver implements Runnable {
    String JPEG_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/CameraL/JpegImage";
    public  final String TAG = "ImageSaver";
    private Image mImage;
    private File mFile;
    private DngCreator mDngCreator = null;
    public ImageSaver(File file,Image image){
        this.mImage = image;
        this.mFile = file;
    }
    public ImageSaver(Image image,File file,DngCreator dngCreator){
        this.mImage = image;
        this.mFile = file;
        this.mDngCreator = dngCreator;
    }
    @Override
    public void run() {
      //这里保存图片
        if(mDngCreator==null) {//JEPG格式的图片
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            writeFile(generateFile(), bytes);
        }else{
            try {
                mDngCreator.writeImage(new FileOutputStream(generateFile().toString()),mImage);
            }catch (Exception e){
               e.printStackTrace();
            }finally {
                mDngCreator.close();
                mImage.close();
            }

        }

    }
    /**
     *  保存照片
     * @param mediaFile 存储照片的目录
     * @param data 字节数组
     */
    public  void writeFile(File mediaFile, byte[] data) {
        System.out.println("保存了一张照片...............");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(mediaFile);
            out.write(data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data"+e);
            e.printStackTrace();
        } finally {
            try {
                out.close();
                mImage.close();//注意在这儿要调用Image的close()方法
            } catch (Exception e) {
            }
        }
    }
    /**
     * 构造照片的存储路径
     * @return
     */
    public  File generateFile(){
        if (! mFile.exists()){
            if (! mFile.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }
        String timeStamp = System.currentTimeMillis()+"";

        File meidaFile;
        if(mDngCreator==null) {
            meidaFile = new File(mFile.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        }else{
           meidaFile =  new File(mFile.getPath() + File.separator + "IMG_" + timeStamp + ".dng");
        }
        return meidaFile ;
    }
}
