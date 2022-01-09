package com.yashsingh.depressiondetection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.ceil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HeartRateMonitor";
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static PowerManager.WakeLock wakeLock = null;

    //Toast
    private Toast mainToast;

    //Beats variable
    public int Beats = 0;
    public double bufferAvgB = 0;

    //DataBase
    public String user;

    //ProgressBar
    private ProgressBar ProgHeart;
    public int ProgP = 0;
    public int inc = 0;

    //Freq + timer variable
    private static long startTime = 0;
    private double SamplingFreq;

    //Arraylist
    public ArrayList<Double> GreenAvgList = new ArrayList<Double>();
    public ArrayList<Double> RedAvgList = new ArrayList<Double>();
    public ArrayList<Double> tslist = new ArrayList<Double>();
    public int counter = 0;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e("Started","Application started");

        user = "USR";

        Log.e("Checking Permission","Permission check");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, 50);
        }
        else {
            next();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,  "mytag::DoNotDimScreen");
        }

////            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
////            previewHolder.addCallback(surfaceCallback);

//        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void next(){
        Log.e("Permission","Camera Permission granted");
        try {
            preview = (SurfaceView) findViewById(R.id.preview);
            Log.e("preview created", "created");
            previewHolder = preview.getHolder();
            Log.e("preview created", "here2");
            previewHolder.addCallback(surfaceCallback);
            Log.e("preview created", "here1");
            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            ProgHeart = findViewById(R.id.HRPB);
            ProgHeart.setProgress(0);
            ProgHeart.setMax(50);
        }catch (Error e){
            Log.e("error", "error"+e);
        }
    }

    //Wakelock + Open device camera + set orientation to 90 degree
    //store system time as a start time for the analyzing process
    //your activity to start interacting with the user.
    // This is a good place to begin animations, open exclusive-access devices (such as the camera)
    @Override
    public void onResume() {
        super.onResume();
        wakeLock.acquire();
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        startTime = System.currentTimeMillis();
    }

    //call back the frames then release the camera + wakelock and Initialize the camera to null
    //Called as part of the activity lifecycle when an activity is going into the background, but has not (yet) been killed. The counterpart to onResume().
    //When activity B is launched in front of activity A,
    // this callback will be invoked on A. B will not be created until A's onPause() returns, so be sure to not do anything lengthy here.
    @Override
    public void onPause() {
        super.onPause();
        wakeLock.release();
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    //getting frames data from the camera and start the heartbeat process
    private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            //if data or size == null ****
            if (data == null) throw new NullPointerException();
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) throw new NullPointerException();

            //Atomically sets the value to the given updated value if the current value == the expected value.
            if (!processing.compareAndSet(false, true)) return;

            //put width + height of the camera inside the variables
            int width = size.width;
            int height = size.height;

            double GreenAvg;
            double RedAvg;

            GreenAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(data.clone(), height, width, 3); //1 stands for red intensity, 2 for blue, 3 for green
            RedAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(data.clone(), height, width, 1); //1 stands for red intensity, 2 for blue, 3 for green

            GreenAvgList.add(GreenAvg);
            RedAvgList.add(RedAvg);

            ++counter; //countes number of frames in 30 seconds


            //To check if we got a good red intensity to process if not return to the condition and set it again until we get a good red intensity
            if (RedAvg < 180) {
                inc = 0;
                ProgP = inc;
                counter = 0;
                ProgHeart.setProgress(ProgP);
                processing.set(false);
                RedAvgList.clear();
                GreenAvgList.clear();
                tslist.clear();
                startTime = System.currentTimeMillis();
                TextView tv1 = (TextView) findViewById(R.id.textprogress);
                tv1.setText("0%");
                TextView tinfo1 = (TextView)findViewById(R.id.textinfo1);
                TextView tinfo2 = (TextView)findViewById(R.id.textinfo2);
                tinfo1.setText("Finger not detected");
                tinfo2.setText("Place your finger correctly");
                TextView tinfo3 = (TextView)findViewById(R.id.textinfo3);
                tinfo3.setText("");
                RedAvgList.clear();
                GreenAvgList.clear();
                tslist.clear();
                startTime = System.currentTimeMillis();
            }

            else
            {
                TextView tinfo1 = (TextView)findViewById(R.id.textinfo1);
                TextView tinfo2 = (TextView)findViewById(R.id.textinfo2);
                tinfo1.setText("Finger Detected !!");
                tinfo2.setText("Measurement in progress...");
            }

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d; //to convert time to seconds
            if (counter>=900) { //when 60 seconds of measuring passes do the following " we chose 30 seconds to take half sample since 60 seconds is normally a full sample of the heart beat

                Log.i("Green Size",Integer.toString(GreenAvgList.size()));
                Log.i("Red Size",Integer.toString(RedAvgList.size()));
                Double[] Green = GreenAvgList.toArray(new Double[GreenAvgList.size()]);
                Double[] Red = RedAvgList.toArray(new Double[RedAvgList.size()]);

                double[] redarray = new double[RedAvgList.size()];
                double[] greenarray = new double[GreenAvgList.size()];
                double[] ts = new double[tslist.size()];

                int it1=0;
                for(Double d:RedAvgList)
                {
                    redarray[it1] = d;
                    it1++;
                }

                int it2=0;
                for(Double d:GreenAvgList)
                {
                    greenarray[it2] = d;
                    it2++;
                }

                int it3 = 0;
                for(Double d:tslist)
                {
                    ts[it3] = d;
                    it3++;
                }

                SamplingFreq = (counter / totalTimeInSecs); //calculating the sampling frequency

                Intent intent =new Intent(MainActivity.this, CalculateHRV.class);
                intent.putExtra("Counter",counter);
                intent.putExtra("Red List",redarray);
                intent.putExtra("Green List",greenarray);
                intent.putExtra("Time Stamp",ts);
                startActivity(intent);
                finish();

//                double HRFreq = Fft.FFT(Green, counter, SamplingFreq); // send the green array and get its fft then return the amount of heartrate per second
//                double bpm = (int) ceil(HRFreq * 60);
//                double HR1Freq = Fft.FFT(Red, counter, SamplingFreq);  // send the red array and get its fft then return the amount of heartrate per second
//                double bpm1 = (int) ceil(HR1Freq * 60);
//
//                // The following code is to make sure that if the heartrate from red and green intensities are reasonable
//                // take the average between them, otherwise take the green or red if one of them is good
//
//                if ((bpm > 45 || bpm < 200)) {
//                    if ((bpm1 > 45 || bpm1 < 200)) {
//
//                        bufferAvgB = (bpm + bpm1) / 2;
//                    } else {
//                        bufferAvgB = bpm;
//                    }
//                } else if ((bpm1 > 45 || bpm1 < 200)) {
//                    bufferAvgB = bpm1;
//                }
//
//                if (bufferAvgB < 45 || bufferAvgB > 200) { //if the heart beat wasn't reasonable after all reset the progresspag and restart measuring
//                    inc = 0;
//                    ProgP = inc;
//                    ProgHeart.setProgress(ProgP);
//                    mainToast = Toast.makeText(getApplicationContext(), "Measurement Failed", Toast.LENGTH_SHORT);
//                    mainToast.show();
//                    startTime = System.currentTimeMillis();
//                    counter = 0;
//                    processing.set(false);
//                    return;
//                }
//
//                Beats = (int) bufferAvgB;
            }

//            if (Beats != 0) { //if beasts were reasonable stop the loop and send HR with the username to results activity and finish this activity
////                Intent i = new Intent(HeartRateProcess.this, HeartRateResult.class);
////                i.putExtra("bpm", Beats);
////                i.putExtra("Usr", user);
////                startActivity(i);
////                finish();
//                Log.e("bpm", "beats="+Beats);
//                Intent intent = new Intent(Heartbeat.this,ShowResult.class);
//                intent.putExtra("Heart Rate",Beats);
//                startActivity(intent);
//                finish();
//            }


            if (RedAvg != 0) { //increment the progresspar

                tslist.add((endTime - startTime) / 1000d);
                ProgP = counter / 18;
                ProgHeart.setProgress(ProgP);

                if(counter%18==0) {
                    TextView tv1 = (TextView) findViewById(R.id.textprogress);
                    ImageView iv1 = (ImageView) findViewById(R.id.i1);
                    ImageView iv2 = (ImageView) findViewById(R.id.i2);
                    ImageView iv3 = (ImageView)findViewById(R.id.i3);
                    tv1.setText(2*ProgP+"%");

                    if(iv1.getVisibility() == View.VISIBLE && iv2.getVisibility() == View.VISIBLE && iv3.getVisibility() == View.VISIBLE)
                    {
                        iv1.setVisibility(View.INVISIBLE);
                        iv2.setVisibility(View.INVISIBLE);
                        iv3.setVisibility(View.INVISIBLE);
                    }
                    //iv2.setVisibility((View.VISIBLE));
                    else if(iv1.getVisibility() == View.VISIBLE && iv2.getVisibility() == View.VISIBLE)
                    {
                        iv3.setVisibility(View.VISIBLE);
                    }
                    else if(iv1.getVisibility() == View.VISIBLE)
                        iv2.setVisibility(View.VISIBLE);
                    else
                        iv1.setVisibility(View.VISIBLE);
                }

                if(counter>250)
                {
                    TextView tinfo3 = (TextView)findViewById(R.id.textinfo3);
                    tinfo3.setText("Glad to see you taking the measurement!!");
                }
            }

            //keeps taking frames tell 30 seconds
            processing.set(false);

        }
    };

    private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                Log.e("PreviewDemo","Imhere");
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(previewCallback);
            } catch (Throwable t) {
                Log.e("PreviewDemo","Exception in setPreviewDisplay()",t);
//                Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

            Camera.Size size = getSmallestPreviewSize(width, height, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
            }

            camera.setParameters(parameters);
            camera.startPreview();
        }


        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    private static Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea < resultArea) result = size;
                }
            }
        }
        return result;
    }
}