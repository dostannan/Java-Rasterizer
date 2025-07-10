import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.lang.Math;

public class ViewFrustum {
    private List<Float[]> farPlanePoints;
    private Float planeEdgeLengths;
    private Float[] origin;
    private Float volume;
    private Float distance;
    private Float fov;
    private Float[] farPlaneWH; //far plane width and height
    private Float unitHeight;
    private Float unitWidth;

    private List<int[]> frustumFaceIndeces = Arrays.asList(new int[][]{
        {1, 2, 4, 3},
        {1, 5, 3},
        {1, 5, 2},
        {2, 5, 4},
        {3, 5, 4}
    });

    public ViewFrustum(Float[] frustumOrigin, Float frustumFov, Float viewDistance) {
        origin = frustumOrigin;
        fov = frustumFov;
        distance = viewDistance;
        
        calculateFrustum();
    }

    public void updateFrustum(Float[] frustumOrigin, Float frustumFov, Float viewDistance) {
        origin = frustumOrigin;
        fov = frustumFov;
        distance = viewDistance;

        calculateFrustum();
    }
    //calculates the 4 far plane points and the volume
    //camera doesnt rotate, the world rotates around it, so we only have to take into account the camera origin (or whatever origin we're using)
    public void calculateFrustum() {
        farPlanePoints = new ArrayList<Float[]>();
        
        Float aspectRatio = (float) (newMain.screenSize()[0] / newMain.screenSize()[1]);
        //half of the far plane's width and height
        unitHeight = newMain.tan(fov / 2);
        unitWidth = unitHeight * aspectRatio;

        Float fpHeightHalf = unitHeight * distance;
        Float fpWidthHalf = unitWidth * distance;

        farPlaneWH = new Float[]{fpWidthHalf, fpHeightHalf};
        //from top left to bottom right, like how we read a book in english
        farPlanePoints.add(new Float[]{origin[0] - fpWidthHalf, origin[1] + fpHeightHalf, origin[2] - distance});
        farPlanePoints.add(new Float[]{origin[0] + fpWidthHalf, origin[1] + fpHeightHalf, origin[2] - distance});
        farPlanePoints.add(new Float[]{origin[0] - fpWidthHalf, origin[1] - fpHeightHalf, origin[2] - distance});
        farPlanePoints.add(new Float[]{origin[0] + fpWidthHalf, origin[1] - fpHeightHalf, origin[2] - distance});
        //for volumes, uses pyramid volume equation as the frustum im using isnt really a frustum, it has 5 points not 8
        //height is distance, length is width, height is.. height. never saw that one coming.
        volume = (fpHeightHalf * fpWidthHalf * distance * 4) / 3;

        planeEdgeLengths = (float) Math.sqrt(Math.pow(fpHeightHalf, 2f) + Math.pow(distance, 2f));
    }  
    //checks if a point is within is view frustum by connecting the point to each vertex of the frustum, breaking it into 4 triangular pyramids and 1 square pyramid,
    //if the volume of all these is equal to the volume of the view frustum, the point is within the view frustum and the function will return true, if not the funtion returns false.
    //doesnt account for lines that cross one of the view frustum planes, will be fixed at a later date but right now im too lazy
    public boolean isPointInFrustum(Float[] point) {
        for(int i = 0; i < 3; i++) {
            // point[i] -= origin[i];
        }

        Float slopeVert  = ((farPlanePoints.get(0)[1] - origin[1]) / (farPlanePoints.get(0)[2] - origin[2]));
        Float slopeHoriz = ((farPlanePoints.get(0)[0] - origin[0]) / (farPlanePoints.get(0)[2] - origin[2]));

        Float slopeDiv = (float) Math.sqrt(Math.pow(slopeVert, 2f) + 1f);

        Float heightTop    = Math.abs(((point[2] *  slopeVert)  - point[1]) / slopeDiv);
        Float heightBottom = Math.abs(((point[2] * -slopeVert)  - point[1]) / slopeDiv);
        Float heightLeft   = Math.abs(((point[2] * -slopeHoriz) - point[0]) / slopeDiv);
        Float heightRight  = Math.abs(((point[2] *  slopeHoriz) - point[0]) / slopeDiv);

        Float frontPyramidVol = (farPlaneWH[0] * farPlaneWH[1] * 4) * (Math.abs(point[2] - farPlanePoints.get(0)[2])) / 3;
        Float topPrismVol = (farPlaneWH[0] * planeEdgeLengths) * (heightTop) / 3;
        Float leftPrismVol = (farPlaneWH[1] * planeEdgeLengths) * (heightLeft) / 3;
        Float rightPrismVol = (farPlaneWH[1] * planeEdgeLengths) * (heightRight) / 3;
        Float bottomPrismVol = (farPlaneWH[0] * planeEdgeLengths) * (heightBottom) / 3; 

        Float combinedVol = frontPyramidVol + topPrismVol + leftPrismVol + rightPrismVol + bottomPrismVol;

        // System.out.println(frontPyramidVol + ", " + topPrismVol);

        return (int)(combinedVol * 1000f) <= (int)(volume * 1000f);
    } 

    public Obj frustumToObj() {
        List<Float[]> farPlaneCopy = newMain.deepCopyList(farPlanePoints);
        farPlaneCopy.add(origin);

        Obj frustumObj = new Obj(farPlaneCopy, frustumFaceIndeces);

        return frustumObj;
    }
}