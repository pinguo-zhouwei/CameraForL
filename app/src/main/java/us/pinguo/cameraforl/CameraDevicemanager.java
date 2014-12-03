package us.pinguo.cameraforl;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.view.Surface;

import java.util.Arrays;

import static android.hardware.camera2.CameraDevice.*;

/**
 * Created by zhouwei on 14-10-24.
 */
public class CameraDevicemanager{

    public CameraDevice getmCameraDevice() {
        return mCameraDevice;
    }

    private CameraDevice mCameraDevice;
    /**
     * Camera state: Showing camera preview.
     */
    public  static final int  STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    public static final int STATE_WAITING_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    public static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    public static final int STATE_WAITING_NON_PRECAPTURE = 3;
    /**
     * Camera state: Picture was taken.
     */
    public static final int STATE_PICTURE_TAKEN = 4;

    private CameraCaptureSession mCameraCaptureSession;

    private AutoFitTextureView mTextureView;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;


    /**
     * 打开相机
     * @param cameraId
     * @param handler
     * @param activity
     */
    public  void openCamera(String cameraId,Handler handler,Activity activity){
        CameraManager manager  = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            manager.openCamera(cameraId,mStateCallback,handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private  StateCallback mStateCallback = new StateCallback(){

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
           mCameraDevice.close();
           mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
          mCameraDevice.close();
          mCameraDevice = null;
        }
    };


}
