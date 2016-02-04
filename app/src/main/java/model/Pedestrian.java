package model;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;

/**
 * Created by Petar on 26-Jan-16.
 */
public class Pedestrian {
    private MatOfPoint2f pointsOfInterests;
    private Mat image;



    public Mat getImage() {
        return image;
    }
    public void setImage(Mat image) {
        this.image = image;
    }
    public Pedestrian(){
        pointsOfInterests = new MatOfPoint2f();
    }
    public Pedestrian(MatOfPoint2f param, Mat im){
        pointsOfInterests = param;
        image = im;
    }

    public MatOfPoint2f getPointsOfInterests() {
        return pointsOfInterests;
    }

    public void setPointsOfInterests(MatOfPoint2f pointsOfInterests) {
        this.pointsOfInterests = pointsOfInterests;
    }
    public void clearArray(){
        pointsOfInterests.empty();
    }
}
