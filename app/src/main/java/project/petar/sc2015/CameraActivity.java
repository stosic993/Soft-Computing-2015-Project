package project.petar.sc2015;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.video.Video;

import java.util.ArrayList;

import model.Pedestrian;

/**
 * Created by Petar on 30-Jan-16.
 */
public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{


    private CameraBridgeViewBase mOpenCvCameraView;
    private static final String TAG=CameraActivity.class.getSimpleName();
    HOGDescriptor hog;
    private MatOfRect foundLocations;
    private MatOfDouble foundWeights;
    Size winStride;
    Size padding;
    Point rectPoint1;
    Point rectPoint2;
    Point fontPoint;

    private boolean isTimeToHog = true;

    private ArrayList<Mat> croppedImages;
    private ArrayList<Pedestrian> pedestrians;
    private BaseLoaderCallback mLoaderCallback;
    private Button back;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "NEUSPESNO LOADOVANJE OPENCV_A");
        }
        setContentView(R.layout.activity_main);
        mLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        Log.i(TAG, "OpenCV loaded successfully");
                        mOpenCvCameraView.enableView();
                    } break;
                    default:
                    {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setMaxFrameSize(540,540);
        foundLocations = new MatOfRect();
        foundWeights = new MatOfDouble();
        winStride = new Size(4, 4);
        padding = new Size(8, 8);
        rectPoint1 = new Point();
        rectPoint2 = new Point();
        fontPoint = new Point();
        final long startTime = System.currentTimeMillis();
        pedestrians = new ArrayList<>();
        back = (Button) findViewById(R.id.back_button);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), MainActivity.class);
                startActivity(i);
                finish();
            }
        });

    }



    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG,"Pocela kamera.");
        hog = new HOGDescriptor();
        hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "Gotova kamera.");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //rotacija
        Mat mRgba = inputFrame.rgba();
        final Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());

        //prilagodjavanje formata
        Imgproc.cvtColor(mRgbaT, mRgbaT,Imgproc.COLOR_RGBA2RGB);
        if(isTimeToHog) {
            Log.i(TAG, "Pozvana metoda asyncHOG.");
            new Thread(new Runnable() {
                public void run() {
                    isTimeToHog = false;
                    hog.detectMultiScale(mRgbaT, foundLocations, foundWeights, 0.0, winStride, padding, 1.05, 2.0, false);
                    isTimeToHog = true;
                }
            }).start();



            findPointsOfInterestsForRoi(mRgbaT);
        }else{
            updatePointsofInterests(mRgbaT);
        }

        for (Rect p : foundLocations.toArray()) {
            Imgproc.rectangle(mRgbaT, new Point(p.x, p.y), new Point(p.x + p.width, p.y + p.height), new Scalar(0, 255, 0));
        }

        for(Pedestrian p : pedestrians){
            for(Point pointP : p.getPointsOfInterests().toArray()){
                Imgproc.rectangle(mRgbaT, new Point(pointP.x, pointP.y), new Point(pointP.x+5, pointP.y + 5), new Scalar(255, 0, 0));
            }
        }

        return mRgbaT;
    }

    private void updatePointsofInterests(Mat image){
        if (pedestrians.isEmpty())
            return;

        Mat tempFrame, lastFrame;
        MatOfPoint2f future = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat error = new MatOfFloat();
        tempFrame = new Mat();

        lastFrame = pedestrians.get(0).getImage();
        Imgproc.cvtColor(image, tempFrame, Imgproc.COLOR_BGR2GRAY);
        for(Pedestrian p : pedestrians){
            try {
                Video.calcOpticalFlowPyrLK(lastFrame, tempFrame, p.getPointsOfInterests(), future, status, error);

                p.setPointsOfInterests(future);
                p.setImage(tempFrame);
            }catch(CvException e){
                Log.e(TAG, "NEUSPESNO PRACENJE, PRESKOCEN FREJM");

            }
        }


    }

    private Mat findPointsOfInterestsForRoi(Mat frame){
        if(foundLocations.toArray().length == 0)
            return frame;

        Mat gray;
        gray = new Mat();
        MatOfPoint points = new MatOfPoint();

        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.goodFeaturesToTrack(gray, points, 100, 0.3, 15);

        pedestrians.clear();

        for(Rect man : foundLocations.toArray()){
            ArrayList<Point> temp = new ArrayList<Point>();
            MatOfPoint2f tempMat = new MatOfPoint2f();
            for(Point p:points.toArray()){
                if(p.inside(man))
                    temp.add(p);
            }
            tempMat.fromList(temp);
            pedestrians.add(new Pedestrian(tempMat, gray));
        }
        return frame;
    }




    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }


}
