/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.hwabo.uvcctest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity extends Activity {
    private static final boolean DEBUG = true;    // TODO set false when production
    private static final String TAG = "MainActivity";

    private final Object mSync = new Object();
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    // for open&start / stop&close camera preview
    private ImageButton mCameraButton;
    private boolean isActive, isPreview;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };

    private UsbControlBlock ctrlBlock;
//	final boolean createNew;
//	ctrlBlock = mCtrlBlocks.get(device);
//	if (ctrlBlock == null) {
//		ctrlBlock = new UsbControlBlock(USBMonitor.this, device);
//		mCtrlBlocks.put(device, ctrlBlock);
//		createNew = true;
//	} else {
//		createNew = false;
//	}
//	if (mOnDeviceConnectListener != null) {
//		mOnDeviceConnectListener.onConnect(device, ctrlBlock, createNew);
//	}

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);//创建


        //TextureView 和 Sufaceview 可以自行选择
        mTextureView = (TextureView) findViewById(R.id.camera_surface_view);
        mTextureView.setRotation(90);//旋转90度
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurfaceTexture = surface;
                Log.i(TAG, "onSurfaceTextureAvailable:==" + mSurfaceTexture);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "onSurfaceTextureSizeChanged :  width==" + width + "height=" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.i(TAG, "onSurfaceTextureDestroyed :  ");
                if (mUVCCamera != null) {
                    mUVCCamera.stopPreview();
                }
                mSurfaceTexture = null;
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart:");
        //注意此处的注册和反注册  注册后会有相机usb设备的回调
        synchronized (mSync) {
            if (mUSBMonitor != null) {
                mUSBMonitor.register();
            }
        }
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop:");
        synchronized (mSync) {
            if (mUSBMonitor != null) {
                mUSBMonitor.unregister();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy:");
        synchronized (mSync) {
            isActive = isPreview = false;
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera.close();
                mUVCCamera = null;
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
//        mUVCCameraView = null;
//        mCameraButton = null;
        super.onDestroy();
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            if (mUVCCamera == null) {
                // XXX calling CameraDialog.showDialog is necessary at only first time(only when app has no permission).
                CameraDialog.showDialog(MainActivity.this);
            } else {
                synchronized (mSync) {
                    mUVCCamera.destroy();
                    mUVCCamera = null;
                    isActive = isPreview = false;
                }
            }
        }
    };

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Log.v(TAG, "onAttach:");
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();

//			 ctrlBlock = new UsbControlBlock(mUSBMonitor, device);
            //	ctrlBlock = mCtrlBlocks.get(device);
//	if (ctrlBlock == null) {
//		ctrlBlock = new UsbControlBlock(USBMonitor.this, device);
//		mCtrlBlocks.put(device, ctrlBlock);
//		createNew = true;
//	} else {
//		createNew = false;
//	}
//	if (mOnDeviceConnectListener != null) {
//		mOnDeviceConnectListener.onConnect(device, ctrlBlock, createNew);
//	}
            if (device.getDeviceClass() == 239 && device.getDeviceSubclass() == 2) {
                mUSBMonitor.requestPermission(device);
            }
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            Log.v(TAG, "onConnect:");
            synchronized (mSync) {
                if (mUVCCamera != null) {
                    mUVCCamera.destroy();
                }
                isActive = isPreview = false;
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mSync) {
                        final UVCCamera camera = new UVCCamera();
                        Log.v(TAG, "创建相机完成时间:" + System.currentTimeMillis());

                        camera.open(ctrlBlock);
                        Log.i(TAG, "supportedSize:" + camera.getSupportedSize());
                        try {
                            //设置预览尺寸 根据设备自行设置
//                            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                        } catch (final IllegalArgumentException e) {
                            try {
                                // fallback to YUV mode
                                //设置预览尺寸 根据设备自行设置
//                                camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                            } catch (final IllegalArgumentException e1) {
                                camera.destroy();
                                return;
                            }
                        }
//                        mPreviewSurface = mUVCCameraView.getHolder().getSurface();//使用Surfaceview的接口
                        if (mSurfaceTexture != null) {
                            isActive = true;
//                            camera.setPreviewDisplay(mPreviewSurface);//使用Surfaceview的接口
                            camera.setPreviewTexture(mSurfaceTexture);
                            Log.v(TAG, "设置相机参数准备启动预览时间:" + System.currentTimeMillis());
                            camera.startPreview();
                            Log.v(TAG, "设置相机参数准备启动预览完成时间:" + System.currentTimeMillis());

                            List<Size> supportedSizeList = camera.getSupportedSizeList();
                            System.out.println("size个数" + supportedSizeList.size());
                            for (Size s : supportedSizeList) {
                                System.out.println("size=" + s.width + "***" + s.height);
                            }
//                            camera.setFrameCallback(iFrameCallback, UVCCamera.PIXEL_FORMAT_YUV420SP);//设置回调 和回调数据类型
//                            设置预览尺寸 根据设备自行设置
//                            camera.setPreviewSize(640, 480);
                            previewSize = camera.getPreviewSize();
                            isPreview = true;

                        }
                        synchronized (mSync) {
                            mUVCCamera = camera;
                        }
                    }
                }
            });
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            Log.v(TAG, "onDisconnect:");
            // XXX you should check whether the comming device equal to camera device that currently using
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    synchronized (mSync) {
                        if (mUVCCamera != null) {
                            mUVCCamera.close();
//                            if (mPreviewSurface != null) {
//                                mPreviewSurface.release();
//                                mPreviewSurface = null;
//                            }
                            isActive = isPreview = false;
                        }
                    }
                }
            }, 0);
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Log.v(TAG, "onDettach:");
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    IFrameCallback iFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer byteBuffer) {
            Log.v(TAG, "相机帧数回调时间:" + System.currentTimeMillis());
            byte[] array = byteBuffer.array();//nv21 数据
//            Bitmap bitmap = YUV420SPDataToBitmap(array, previewSize.width, previewSize.height);//转换bitmap
            System.out.println("帧数回调=width" + previewSize.width + " height=" + previewSize.height);
        }
    };
    Size previewSize;


    /**
     * YUV420sp原始预览数据转 bitmap
     *
     * @param bytes
     * @param w
     * @param h
     * @return
     */
    public static Bitmap YUV420SPDataToBitmap(byte[] bytes, int w, int h) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        YuvImage yuvimage = new YuvImage(bytes, ImageFormat.NV21, w, h, null);
        ByteArrayOutputStream baos;
        byte[] rawImage;
        baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, w, h), 100, baos);// 80--JPG图片的质量[0-100],100最高
        rawImage = baos.toByteArray();
//            将rawImage转换成bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
        return bitmap;
    }
//
//    private final SurfaceHolder.Callback mSurfaceViewCallback = new SurfaceHolder.Callback() {
//        @Override
//        public void surfaceCreated(final SurfaceHolder holder) {
//            Log.v(TAG, "surfaceCreated:");
//        }
//
//        @Override
//        public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
//            if ((width == 0) || (height == 0)) return;
//            Log.v(TAG, "surfaceChanged:");
//            mPreviewSurface = holder.getSurface();
//            synchronized (mSync) {
//                if (isActive && !isPreview && (mUVCCamera != null)) {
//                    mUVCCamera.setPreviewDisplay(mPreviewSurface);
//                    mUVCCamera.startPreview();
//                    isPreview = true;
//                }
//            }
//        }
//
//        @Override
//        public void surfaceDestroyed(final SurfaceHolder holder) {
//            Log.v(TAG, "surfaceDestroyed:");
//            synchronized (mSync) {
//                if (mUVCCamera != null) {
//                    mUVCCamera.stopPreview();
//                }
//                isPreview = false;
//            }
//            mPreviewSurface = null;
//        }
//    };
}
