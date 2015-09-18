package com.example.miguel.basicvuforia;

//import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;


import com.qualcomm.ar.pl.DebugLog;//Para depurar pero he usado log
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.Frame;
import com.qualcomm.vuforia.Marker;
import com.qualcomm.vuforia.MarkerTracker;
import com.qualcomm.vuforia.PIXEL_FORMAT;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vec2F;
import com.qualcomm.vuforia.Vuforia;
import com.example.miguel.basicvuforia.SampleApplication.SampleApplicationControl;
import com.example.miguel.basicvuforia.SampleApplication.SampleApplicationException;
import com.example.miguel.basicvuforia.SampleApplication.SampleApplicationSession;

//Miguel: Librería para trabajar con las imágenes
import com.qualcomm.vuforia.Frame;



import com.example.miguel.basicvuforia.SampleApplication.utils.LoadingDialogHandler;
import com.example.miguel.basicvuforia.SampleApplication.utils.SampleApplicationGLView;
import com.example.miguel.basicvuforia.SampleApplication.utils.Texture;
import com.example.miguel.basicvuforia.R;

import java.nio.ByteBuffer;
import java.util.Vector;


//JMONEYENGINE
 import com.jme3.asset.AssetManager;
 import com.jme3.scene.Spatial;
 import com.jme3.app.SimpleApplication;
 import com.jme3.app.Application;

 import com.jme3.font.BitmapText;
 import com.jme3.light.DirectionalLight;
 import com.jme3.material.Material;
 import com.jme3.math.Vector3f;
 import com.jme3.scene.Geometry;
 import com.jme3.scene.Spatial;
 import com.jme3.scene.shape.Box;




//public class MainActivity extends. AppCompatActivity {
public class MainActivity extends Activity implements  SampleApplicationControl{

    private static final String LOGTAG = "FrameMarkers";

    SampleApplicationSession vuforiaAppSession;

    // Our OpenGL view:
    private SampleApplicationGLView mGlView;

    // Our renderer:
    private FrameMarkerRenderer mRenderer;

    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    private RelativeLayout mUILayout;

    private Marker dataSet[];

    private GestureDetector mGestureDetector;

    private boolean mFlash = false;
    private boolean mContAutofocus = false;
    private boolean mIsFrontCameraActive = false;

    private View mFlashOptionView;

    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
            this);

    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

    boolean mIsDroidDevice = false;

    public static AssetManager assetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "Miguel: onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //MIG: Creamos una nueva aplicación Vuforia
        vuforiaAppSession = new SampleApplicationSession(this);

        //MIG: Pantalla de animación de PROGRESS-BAR para saber que hace algo.
        startLoadingAnimation();

        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Sirve para detectar los gestos, es decir, cuando se toca la pantalla o otro tipo de gesto.
        // Es una clase que viene más abajo.
        mGestureDetector = new GestureDetector(this, new GestureListener());

        //MIG:Cargar las Texturas
        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures();


       // loadNinja();

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
                "droid");
    }

    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();


        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            // Generates a Handler to trigger autofocus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);

                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);

            return true;
        }
    }

    private void startLoadingAnimation()
    {
        //MIG: Esto poner la pantalla de PROGRESO  para saber que estamos haciendo algo.
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,
                null, false);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.GREEN);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);

        // Shows the loading indicator at start
        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

    }

    // Called when the activity will start interacting with the user.
    @Override
    protected void onResume()
    {
        Log.d(LOGTAG, "Miguel: onResume");
        super.onResume();

        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        Log.d(LOGTAG, "Miguel: Antes de Intentar iniciar sesión");

        try
        {
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }

        Log.d(LOGTAG, "Miguel: Despues de Intentar iniciar sesión");

        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

    }

    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        // Turn off the flash
        if (mFlashOptionView != null && mFlash)
        {
            // OnCheckedChangeListener is called upon changing the checked state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                ((Switch) mFlashOptionView).setChecked(false);
            } else
            {
                ((CheckBox) mFlashOptionView).setChecked(false);
            }
        }

        try
        {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
    }

    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }

        // Unload texture:
        mTextures.clear();
        mTextures = null;

        System.gc();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //--------------------------------------
    // Importación de librerías de implement

    // MIG: Inicializa la aplicación y la cámara.
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {

        if (exception == null)
        {
            initApplicationAR();

            mRenderer.mIsActive = true;

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            // Hides the Loading Dialog
            loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            try
            {
                vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
            } catch (SampleApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }

            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

            if (result)
                mContAutofocus = true;
            else
                Log.e(LOGTAG, "Unable to enable continuous autofocus");

       //Mig: Lo quito por ahora pues no lo utilizo
       /*    mSampleAppMenu = new SampleAppMenu(this, this, "Frame Markers",
                    mGlView, mUILayout, null);
            setSampleAppMenuSettings();
            */

        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }

    // Initializes AR application components.
    // MIG: LLamado en la inicialización.
    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new FrameMarkerRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);

    }

    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        MainActivity.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }

    //MIG: Aquí es donde obtenemos cada frame y trabajamos con el
    //como se indica en el libro
    @Override
    public void onQCARUpdate(State state)
    {
        com.qualcomm.vuforia.Image imageRGB565 = null;
        Frame frame = state.getFrame();

        for (int i = 0; i < frame.getNumImages(); ++i) {
            com.qualcomm.vuforia.Image image = frame.getImage(i);
                  if (image.getFormat() == PIXEL_FORMAT.RGB565) {
                      imageRGB565 = image;
                break;
            }
        }

        Log.d(LOGTAG, "Miguel: onQCARUpdate");

        if (imageRGB565 != null) {
            ByteBuffer pixels = imageRGB565.getPixels();
            byte[] pixelArray = new byte[pixels.remaining()];
            pixels.get(pixelArray, 0, pixelArray.length);
            int imageWidth = imageRGB565.getWidth();
            int imageHeight = imageRGB565.getHeight();
            int stride = imageRGB565.getStride();
            Log.d("MIguel: Image", "Image width: " + imageWidth);
            Log.d("MIguel: Image", "Image height: " + imageHeight);
            Log.d("MIguel: Image", "Image stride: " + stride);
            Log.d("MIguel: Image", "First pixel byte: " + pixelArray[0]);
        }


    }

    @Override
    public boolean doInitTrackers()
    {
     // Indicate if the trackers were initialized correctly
        boolean result = true;

        // Initialize the marker tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker trackerBase = trackerManager.initTracker(MarkerTracker
                .getClassType());
        MarkerTracker markerTracker = (MarkerTracker) (trackerBase);

        if (markerTracker == null)
        {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        return result;

    }
    // To be called to load the trackers' data
    public boolean doLoadTrackersData(){
        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager
                .getTracker(MarkerTracker.getClassType());
        if (markerTracker == null)
            return false;


        //MIG: Aqui es donde decimos con que vamos a Trabajar si con FrameMarker-ImageTracker
        // En nuestro caso con FrameMarker y el 0 de los 512
        dataSet = new Marker[1];

        dataSet[0] = markerTracker.createFrameMarker(0, "MarkerQ", new Vec2F(
                50, 50));
        if (dataSet[0] == null)
        {
            Log.e(LOGTAG, "Failed to create frame marker Q.");
            return false;
        }

        Log.i(LOGTAG, "Successfully initialized MarkerTracker.");

        return true;
    };


    // To be called to start tracking with the initialized trackers and their
    // loaded data
    public boolean doStartTrackers(){
        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager
                .getTracker(MarkerTracker.getClassType());
        if (markerTracker != null)
            markerTracker.start();

        return result;
    };


    // To be called to stop the trackers
    public boolean doStopTrackers(){
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager
                .getTracker(MarkerTracker.getClassType());
        if (markerTracker != null)
            markerTracker.stop();

        return result;
    };


    // To be called to destroy the

//trackers' data
    public boolean doUnloadTrackersData(){
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        return result;
    };


    // To be called to deinitialize the trackers
    public boolean doDeinitTrackers(){
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(MarkerTracker.getClassType());

        return result;
    };


    // This callback is called after the Vuforia initialization is complete,
    // the trackers are initialized, their data loaded and
    // tracking is ready to start


    private void showToast(String text)
    {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    //Devuelve si la cámara esta activa y lo usa en FrameMarkerRenderer
    boolean isFrontCameraActive()
    {
        return false;
    }

    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_Q.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_C.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_A.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_R.png",
                getAssets()));
    }

    private void loadNinja()
    {
        Log.d(LOGTAG, "Miguel: antes cargar ninja");
        Spatial ninja = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        Log.d(LOGTAG, "Miguel: despues cargar ninja");
    }

    /*boolean isFrontCameraActive()
    {
        return mIsFrontCameraActive;
    }*/


}
