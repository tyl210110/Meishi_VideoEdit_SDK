package tech.qt.com.meishivideoeditsdk.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import tech.qt.com.meishivideoeditsdk.camera.filter.GPUFilter;

/**
 * Created by chenchao on 2017/12/5.
 */

public class GLRender implements GLSurfaceView.Renderer,SurfaceTexture.OnFrameAvailableListener{
    public SurfaceTexture mSurfaceTexture;
    private int mCameraTextureId;
    private GLSurfaceView glSurfaceView;
    private CameraWraper mCamera;
    private GPUFilter mFilter;
    private int mViewWidth;
    private int mViewHeight;
    private LinkedList<Runnable> mRunOnDraw;
    public GLRender(){

    }
    private static final String vts
            = "uniform mat4 uMVPMatrix;\n"
            + "uniform mat4 uTexMatrix;\n"
            + "attribute highp vec4 aPosition;\n"
            + "attribute highp vec4 aTextureCoord;\n"
            + "varying highp vec2 vTextureCoord;\n"
            + "\n"
            + "void main() {\n"
            + "	gl_Position = uMVPMatrix * aPosition;\n"
            + "	vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n"
            + "}\n";
    private static final String fgs//绘制视频层的
            = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "varying highp vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
            + "}";
    private static final float[] VERTICES = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
    private static final float[] TEXCOORD = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };

    private static final int FLOAT_SZ = Float.SIZE / 8;
    private static final int VERTEX_NUM = 4;
    private static final int VERTEX_SZ = 4 * 2;

    private final float[] mMvpMatrix = new float[16];
    private final float[] mTexMatrix = new float[16];
    private int mProgramId = -1;


    private int mMVPMatrixLoc = -1;
    private int mPositionLoc = -1;
    private int mTexMatrixLoc = -1;
    private int mTextureCoordLoc = -1;
    private int mTextureLoc = -1;
    private FloatBuffer pVertex;
    private FloatBuffer pTexCoord;

    public GLRender(CameraWraper camera,GLSurfaceView glSurfaceView){
        mCamera = camera;
        this.glSurfaceView = glSurfaceView;
        mRunOnDraw = new LinkedList<>();
    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mCameraTextureId = OpenGLUtils.generateOES_SurfaceTexture();
        mSurfaceTexture = new SurfaceTexture(mCameraTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);


        if(mFilter!=null){
            mFilter.init();
        }
        mCamera.setPreviewTexture(mSurfaceTexture);

        mCamera.startPreview();
        Log.e("onSurfaceCreated","1");
    }
    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        mViewWidth = i;
        mViewHeight  = i1;
        GLES20.glViewport(0,0,i,i1);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mSurfaceTexture.updateTexImage();
        drawVideoFrame();
    }

    private void drawVideoFrame() {
        if(mFilter!=null) {
            runPendingOnDrawTasks();
            mFilter.onDrawFrame(mCameraTextureId, mSurfaceTexture,mViewWidth,mViewHeight);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.e("request Render","1");
        glSurfaceView.requestRender();
    }

    public void setmFilter(final GPUFilter mFilter) {
        this.mFilter = mFilter;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                mFilter.init();
            }
        });
    }
    public void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }
}
