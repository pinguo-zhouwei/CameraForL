package us.pinguo.cameraforl.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import us.pinguo.cameraforl.AutoFitTextureView;
import us.pinguo.cameraforl.ImageSaver;
import us.pinguo.cameraforl.R;

/**
 * Created by zhouwei on 14-10-24.
 */
public class CameraFragment extends Fragment implements TextureView.SurfaceTextureListener,View.OnClickListener{
   /** 提供一个空的构造方法*/
    public CameraFragment(){}
   /**
    * 转换屏幕预览和JPEG的朝向一致
    */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    //----- view -------//
    @InjectView(R.id.capture)
    View capture;
    @InjectView(R.id.btn_add_et)
    ImageView btnAddET;
    @InjectView(R.id.btn_reduce_et)
    ImageView btnReduceET;
    @InjectView(R.id.btn_add_iso)
    ImageView btnAddISO;
    @InjectView(R.id.btn_reduce_iso)
    ImageView btnRedeceISO;
    @InjectView(R.id.preview)
    AutoFitTextureView  mTextureView;
    @InjectView(R.id.tv_expostureTime)
    TextView tvexposeTime;
    @InjectView(R.id.tv_iso)
    TextView tvIso;
    //button 点击事件
    @OnClick(R.id.capture)
    public void capture(){
      //takePicture();
      lockFocus();
    }

    /**
     * 保存设置信息
     */
   private SharedPreferences mSharedPreferences;
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

    private int mCurrentETIndex= 0;

    private int mCurrentISOIndex = 0;

    private long mCurrentET =1;
    private int mCurrentISO= 100;

    private long[] mET = new long[]{1,2,4,6,8,15,30,60,100,125,250,500,1000,2000,4000,6000,8000,16000};

    private int[] mISO = new int[]{100,200,400,800,1000,1600,3200,6400};

    /**
     * 当前的相机Id
     */
    private String mCameraId;

    private CameraCaptureSession mCameraCaptureSession;
    /**
     * 预览用的Builder
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;
    /**
     * 拍照用的Builder
     */
    private CaptureRequest.Builder builder;
    /**
     * 处理JPEG图片
     */
    private ImageReader mImageReader;
    /**
     * 处理Raw(DNG格式)的图片
     */
    private ImageReader mRawImageReader;

    private CameraDevice mCameraDevice;
    /**
     * 处理照片的Handler
     */
    private Handler mBackgroundHandler ;
    /**
     * 预览屏幕的大小
     */
    private Size mPreviewSize;

    private CaptureRequest mPreviewRequest;
    /**
     * 防止阻碍UI线程，开一个线程在后台处理
     */
    private HandlerThread mBackgroundThread;
    /**
     * 阻止app退出之前close相机
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    /** 拍照后返回的结果*/
    private TotalCaptureResult mReusult;
    /** 当前状态为预览状态 */
    private int mState = STATE_PREVIEW;
    /**
     * 播放拍照时的声音
     */
    private MediaActionSound mediaActionSound ;

    private Image mRawImage;

    private CameraCharacteristics mCameraCharacteristics;
    /**
     * 拍照请求最终返回的数据接口
     */
    final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
             //在这里存储照片
            /** 存储JPEG格式照片的文件*/
            File JpegFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"CameraForL");
            Image image = reader.acquireNextImage();
            mBackgroundHandler.post(new ImageSaver(JpegFile,image));

        }
    };
    final ImageReader.OnImageAvailableListener mOnRaeImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            /** 存储DNG格式照片的文件**/
            mRawImage = reader.acquireNextImage();

        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaActionSound = new MediaActionSound();
        mediaActionSound.load(MediaActionSound.SHUTTER_CLICK);
        mSharedPreferences = getActivity().getSharedPreferences("config",Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       View view = inflater.inflate(R.layout.camera_fragment,container,false);
        ButterKnife.inject(this,view);
        view.findViewById(R.id.btn_add_et).setOnClickListener(this);
        view.findViewById(R.id.btn_add_iso).setOnClickListener(this);
        view.findViewById(R.id.btn_reduce_et).setOnClickListener(this);
        view.findViewById(R.id.btn_reduce_iso).setOnClickListener(this);
        view.findViewById(R.id.btn_album).setOnClickListener(this);
        view.findViewById(R.id.btn_setting).setOnClickListener(this);
        capture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        takePicture();
                        break;
                    case MotionEvent.ACTION_UP:
                        unlockFocus();
                        break;
                }
                return true;
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        startThread();
        /**
         *当屏幕关闭或者屏幕退到后台时，这个surface texture 已经可以用了，并且"onSurfaceTextureAvailable"
         *
         * 不会被调用，在这种情况下，我们可以从这儿开启一个预览并启动Camera，否则，我们需要等待,直到textureListener里面的
         * surface 可以用为止
         */
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    static CameraFragment getInstance(){
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }
    /**
     * 开启线程
     */
    public void startThread(){
        mBackgroundThread = new HandlerThread("CameraThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    @Override
    public void onPause() {
        super.onPause();
       closeCamera();//关闭相机
       stopBackgroundThread();//停止后台线程
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 关闭当前的相机设备
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraCaptureSession) {
               // mCameraCaptureSession.abortCaptures();
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            if(null!= mRawImageReader){
                mRawImageReader.close();
                mRawImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开相机的回调，openCamera方法调用后被called
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback(){

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            if(mCameraDevice!=null){
                mCameraDevice.close();
                mCameraDevice = null;
            }

        }
    };
    /**
     * 提交capture后的处理回调
     */
    public CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
//            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            mReusult = result;
         //   process(result);
        }

        private void process(CaptureResult result) {
            switch (1) {
                case STATE_PREVIEW : {//预览状态
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {//
                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        int aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_WAITING_NON_PRECAPTURE;
                            takePicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    int aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (CaptureResult.CONTROL_AE_STATE_PRECAPTURE == aeState) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    } else if (CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED == aeState) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    int aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (CaptureResult.CONTROL_AE_STATE_PRECAPTURE != aeState) {
                        mState = STATE_PICTURE_TAKEN;
                        takePicture();
                    }
                    break;
                }
            }
        }
    };
    /**
     *  根据当前的CameraId 打开相机
     * @param width
     * @param height
     */
    public void openCamera(int width,int height){
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }

    }

    /**
     * 创建相机预览
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface,mRawImageReader.getSurface(),mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            //当session准备好的时候，就开开始显示预览
                            mCameraCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCameraCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *  为预览mTextureView配置必要的转换矩阵，
     *  这个方法在preview的大小被setOutputSize()方法判定之后被调用。
     *
     * @param viewWidth
     * @param viewHeight
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * 为相机设置一些变量
     * @param width
     * @param height
     */
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                    mCameraCharacteristics    = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
               Range<Integer>[]fps =  mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                for(int i=0;i<fps.length;i++){
                    System.out.println("fps:"+fps[i]);
                }
                StreamConfigurationMap map = mCameraCharacteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                int format =  map.getOutputFormats()[2];
                Size size =map.getOutputSizes(format)[0];
               //初始化DNG ImageReader,用来读取dng格式图片的数据
               mRawImageReader = ImageReader.newInstance(size.getWidth(),size.getHeight(),format,7);
               mRawImageReader.setOnImageAvailableListener(mOnRaeImageAvailableListener,mBackgroundHandler);
                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
           Log.e("CameraFragment","this device not support Camera API2");
        }
    }

    /**
     * 选择预览的分辨率
     * @param choices
     * @param width
     * @param height
     * @param aspectRatio
     * @return
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e("CameraFragment", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        openCamera(width,height);//打开相机
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
      configureTransform(width,height);//
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    /**
     * 拍照
     */
    public void takePicture(){
        System.out.println("-------------------->执行takePicture");
        try {
        final Activity activity = getActivity();
        if(null==activity || null==mCameraDevice){
            return;
        }
        //取出保存在ShatePreference里面的设置信息
            /** 是否自动曝光*/
          boolean isOpenAE = mSharedPreferences.getBoolean("open_ae_mode",false);
          /** 是否保存dng格式的图片*/
          boolean isSaveDng = mSharedPreferences.getBoolean("save_dng",false);
          System.out.println("isSacedng："+isSaveDng);
          System.out.println("isOpenAE："+isOpenAE);
         //创建一个用于拍照的CameraRequest.builder
            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            if(isSaveDng){
                builder.addTarget(mRawImageReader.getSurface());   //保存dng
                builder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON); // Required for RAW capture
            }else{
                builder.addTarget(mImageReader.getSurface());   //保存jpeg
            }
          //设置参数
            if(isOpenAE){
                builder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                builder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }else{
                builder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,1000000000l/mCurrentET);//设置曝光时间，要在CONTROL_AE_MODE 设置为OFF的情况下才生效
                builder.set(CaptureRequest.SENSOR_SENSITIVITY,mCurrentISO);//设置 ISO，感光度
            }
            //设置连续帧
            Range<Integer> fps[] = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,fps[fps.length-1]);//设置每秒30帧
        //设置orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            builder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                File DngFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"CameraL/DngImage");
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    if(mRawImage!=null){
                        DngCreator dngCreator = null;
                        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
                        try {
                            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(mCameraId);
                            dngCreator = new DngCreator(cameraCharacteristics, result);
                            dngCreator.writeImage(new FileOutputStream(generateFile()), mRawImage);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                dngCreator.close();
                              //  mRawImage.close();//注意在这儿要调用Image的close()方法
                            } catch (Exception e) {
                            }
                        }
                    }

                }
                private  File generateFile(){
                    if (! DngFile.exists()){
                        if (! DngFile.mkdirs()){
                            Log.d("CameraFragment", "failed to create directory");
                            return null;
                        }
                    }
                    String timeStamp = System.currentTimeMillis()+"";
                    File    meidaFile =  new File(DngFile.getPath() + File.separator + "IMG_" + timeStamp + ".dng");
                    return meidaFile ;
                }
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    mBackgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK);
                        }
                    });
                }
            };
         mCameraCaptureSession.stopRepeating();//停止重复预览请求
         mCameraCaptureSession.setRepeatingRequest(builder.build(), captureCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void runPrecaptureSequence() {
        System.out.println("-------------------->执行runPrecaptureSequence");
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(), callback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 拍照之前，锁定聚焦，之后会调用takePicture
     */
    private void lockFocus() {

        System.out.println("-------------------->lockFocus");
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
          //  for(int i=0;i<5;i++){
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(), callback,
                        mBackgroundHandler);
          // }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    CameraCaptureSession.CaptureCallback callback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            System.out.println("-------------------->onCaptureCompleted");
            int afState = result.get(CaptureResult.CONTROL_AF_STATE);
            if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                int aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                System.out.println("-----------state--------->"+aeState);
                if (aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED ||aeState==CaptureRequest.CONTROL_AE_STATE_PRECAPTURE) {
                    mState = STATE_WAITING_NON_PRECAPTURE;
                    takePicture();
                } else {
                    runPrecaptureSequence();
                }
            }
        }
    };

    /**
     *释放聚焦
     */
    private void unlockFocus() {
        System.out.println("-------------------->unlockFocus");
        try {
            // Reset the autofucos trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_add_et:
                if(mCurrentETIndex<mET.length-1){
                    mCurrentETIndex +=1;
                    mCurrentET = mET[mCurrentETIndex];
                    tvexposeTime.setText("ET:1/"+mCurrentET);
                }else {
                    Toast.makeText(getActivity(),"已经是最大值了",Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.btn_reduce_et:
                if(mCurrentETIndex>0){
                    mCurrentETIndex = mCurrentETIndex-1;
                    mCurrentET = mET[mCurrentETIndex];
                    tvexposeTime.setText("ET:1/"+mCurrentET);
                }else{
                    Toast.makeText(getActivity(),"已经是最小值了",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_add_iso:
                if(mCurrentISOIndex<mISO.length-1){
                    mCurrentISOIndex +=1;
                    mCurrentISO = mISO[mCurrentISOIndex];
                    tvIso.setText("ISO:"+mCurrentISO);
                }else{
                    Toast.makeText(getActivity(),"已经是最大值了",Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.btn_reduce_iso:
                if(mCurrentISOIndex>0){
                    mCurrentISOIndex-=1;
                    mCurrentISO = mISO[mCurrentISOIndex];
                    tvIso.setText("ISO:"+mCurrentISO);
                }else{
                    Toast.makeText(getActivity(),"已经是最小值了",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_album:
                Intent intent = new Intent(getActivity(),AlbumActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_setting:
                Intent intent2 = new Intent(getActivity(),SettingActivity.class);
                startActivity(intent2);
                break;
        }
    }

    /**
     * 基于区域来比较两个Size
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }


}
