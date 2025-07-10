import java.awt.Color;
import java.awt.image.BufferedImage;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Frame {
    private static Player player = Game.getPlayer();
    private static int[] screenDimensions = Game.getScreenDimensions();
    //precalculated components for the matricPerspectiveProject() function
    private static Float[] matrixComponents = new Float[]{
        screenDimensions[0] / (((float) screenDimensions[0] / screenDimensions[1]) * (float) Math.tan(player.getFov() / 2)),  //x
        screenDimensions[1] / (float) Math.tan(player.getFov() / 2),                                                                              //y
        (-1 * player.getClipPlanes()[1]) / (player.getClipPlanes()[1] - player.getClipPlanes()[0]), //z
        (-2 * player.getClipPlanes()[1] * player.getClipPlanes()[0]) / (player.getClipPlanes()[1] - player.getClipPlanes()[0])  //w
    };
    //generates list of points, including point1 and point2, connecting point1 and point2 on a 2d plane
    //linePoints is a parameter in case you want to add to an already existing list, set to null otherwise, same with the xAtY parameter (see fillFaceScanLine for more info)
    public static int[] generateLine(Float[] point1, Float[] point2, Float[] point1UV, Float[] point2UV, int[] maxAndMinY, Map<Integer, List<Float[]>> xAtY) {
        if (maxAndMinY == null) {
            maxAndMinY = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        }
        if (xAtY == null) {
            xAtY = new HashMap<Integer, List<Float[]>>();
        }

        Float deltaX = (float) (point2[0] - point1[0]);
        Float deltaY = (float) (point2[1] - point1[1]);
        Float deltaU = (point2UV[0] - point1UV[0]);
        Float deltaV = (point2UV[1] - point1UV[1]);
        //all change depending on if we're iterating over x or y
        Float zSlope; 
        Float xySlope;
        Float uSlope; //u is texture coord over x
        Float vSlope; //v is texture coord over y
        //if delta x is greater than delta y (slope is less than 1), iterate over x, else iterate over y
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            xySlope = deltaY / deltaX;
            zSlope = (point2[2] - point1[2]) / (deltaX);
            uSlope = (deltaU) / (deltaX);
            vSlope = (deltaV) / (deltaX);
            
            for(int x = 0; x != deltaX + Math.signum(deltaX); x += Math.signum(deltaX)) {
                int y = (int) (x * xySlope + point1[1]);
                Float z = (x * zSlope) + point1[2];
                Float u = (x * uSlope) + point1UV[0];
                Float v = (x * vSlope) + point1UV[1];
                
                if (xAtY.get(y) == null) {
                    xAtY.put(y, new ArrayList<Float[]>());
                }
                
                xAtY.get(y).add(new Float[]{(x + point1[0]), z, u, v});
                //used to find the largest and smallest y value of the face
                //index 0 is the min, index 1 is the max
                if (y > maxAndMinY[1]) { 
                    maxAndMinY[1] = y;
                }
                if (y < maxAndMinY[0]) {
                    maxAndMinY[0] = y;
                }
            }
        } else {
            xySlope = deltaX / deltaY;
            zSlope = (point2[2] - point1[2]) / (deltaY);
            uSlope = (deltaU) / (deltaY);
            vSlope = (deltaV) / (deltaY);

            for(int y = 0; y != deltaY + Math.signum(deltaY); y += Math.signum(deltaY)) {
                int x = (int) (y * xySlope + point1[0]);
                int offsetY = (int) (y + point1[1]);
                Float z = (y * zSlope) + point1[2];
                Float u = (y * uSlope) + point1UV[0];
                Float v = (y * vSlope) + point1UV[1];

                if (xAtY.get(offsetY) == null) {
                    xAtY.put(offsetY, new ArrayList<Float[]>());
                }

                try {  
                    xAtY.get(offsetY).add(new Float[]{(float) x, z, u, v});
                } catch (OutOfMemoryError e) {
                    System.out.println(xAtY.size());
                    throw new RuntimeException();
                }

                if (offsetY > maxAndMinY[1]) {
                    maxAndMinY[1] = offsetY;
                }
                if (offsetY < maxAndMinY[0]) {
                    maxAndMinY[0] = offsetY;
                }
            }
        }
        return maxAndMinY;
    }
    //scan line conversion; create a list of all the x coordinates at a given y coordinate, find the largest and smallest in the list, then draw a line between them
    public static void fillObjScanLine(Obj object, BufferedImage zBuffer, BufferedImage frameBuffer, boolean justOutline) {

        List<Float[]> screenCoords = new ArrayList<Float[]>();

        for(Float[] point : object.getVertices()) { // perspective project each point
            Float[] screenPoint = perspectiveProject(point, Game.getPlayer().getPosition(), Game.getPlayer().getRotation(), Game.getPlayer().getFov());
            screenCoords.add(screenPoint);
        }

        Map<Integer, List<Float[]>> xAtY = new HashMap<Integer, List<Float[]>>();

        for (int f = 0; f < object.getFaceIndexes().size(); f++) { //cycle through faces
            xAtY.clear();

            int[] indexList = object.getFaceIndexes().get(f);
            int[] indexUVList = object.getUVIndexes().get(f);

            int[] maxAndMinY = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
            //check if all points of face are in front of the player, if not dont bother rendering it
            int boundsCount = 0;
            for (int i = 0; i < indexList.length; i++) {
                boundsCount += screenCoords.get(indexList[i] - 1)[4];
            }
            if (boundsCount == indexList.length ) {
                continue;
            }
            //generate edges of the face
            for (int i = 0; i < indexList.length; i++) {
                int i2 = (i != indexList.length - 1) ? i + 1 : 0;
                Float[] point1 = screenCoords.get(indexList[i] - 1);
                Float[] point2 = screenCoords.get(indexList[i2] - 1);

                Float[] point1UV = object.getUVCoords().get(indexUVList[i] - 1);
                Float[] point2UV = object.getUVCoords().get(indexUVList[i2] - 1);

                generateLine(point1, point2, point1UV, point2UV, maxAndMinY, xAtY);
            }
            //fill face
            BufferedImage texture = Game.getTextureFromRepo(object.getTexturePaths().get(f));
            // System.out.println(Game.getTextureFromRepo(object.getTexturePaths().get(f)));
            int color = 1;
            for(int y = Math.max(maxAndMinY[0], 0); y < Math.min(maxAndMinY[1], screenDimensions[0] - 1); y++) {
                List<Float[]> xList = xAtY.get(y);

                if (xList == null) {
                    continue;
                }

                Float[][] maxAndMinX = maxAndMin(xList);

                int deltaX = (int) (maxAndMinX[1][0] - maxAndMinX[0][0]);
                
                int firstValue = (int) Math.max(maxAndMinX[0][0], 0);
                int lastValue  = (int) Math.min(maxAndMinX[1][0], screenDimensions[0] - 1);

                Float zSlope;
                Float uSlope;
                Float vSlope;

                if (deltaX == 0) {
                    zSlope = 0f;
                    uSlope = 0f;
                    vSlope = 0f;
                } else {
                    zSlope = (maxAndMinX[1][1] - maxAndMinX[0][1]) / (deltaX);
                    uSlope = (maxAndMinX[1][2] - maxAndMinX[0][2]) / (deltaX);
                    vSlope = (maxAndMinX[1][3] - maxAndMinX[0][3]) / (deltaX);
                }
                for (int x = firstValue; x <= lastValue; x++) {

                    Float z = (zSlope * (x - maxAndMinX[0][0])) + maxAndMinX[0][1]; //find z

                    if (z >= 1f || z < 0) {
                        continue;
                    }

                    Float u = (uSlope * (x - maxAndMinX[0][0])) + maxAndMinX[0][2]; //find u
                    Float v = (vSlope * (x - maxAndMinX[0][0])) + maxAndMinX[0][3]; //find v

                    // if (x == firstValue + 20 && y == Math.min(maxAndMinY[1], screenDimensions[0] - 1) - 50) {
                    //     System.out.println(lastValue);
                    //     frameBuffer.setRGB(x, y, Color.RED.getRGB());
                    //     continue;
                    // }

                    u *= texture.getWidth() - 1;
                    v *= texture.getHeight() - 1;

                    int zColor = (int) ((255 * z));

                    //get red of pixel already at x and y, if its whiter (closer to camera) dont render this pixel
                    if ((zBuffer.getRGB(x, y) >> 16 & 0x0FF) > zColor) {
                        continue;
                    }

                    color = texture.getRGB(u.intValue(), v.intValue());
                    // color = rgbToSRGB(Math.round(u * 255), Math.round(v * 255), 0);
                    
                    //add fog effect to reduce harsh far clip plane
                    color = mixColors(color, Game.getSkyColor(), (float) Math.pow(1 - z, 3));
                    
                    //using rgbToSRGB instead of Color.getRGB() in order to not create a new color object per pixel 
                    zBuffer.setRGB(x, y, rgbToSRGB(zColor, zColor, zColor));
                    //display uv coords
                    frameBuffer.setRGB(x, y, color);
                    
                }
            }
        }
    }
    //perspective projection, converts inputted points from world space to view space by offsetting them by the cameras position and rotation
    public static Float[] perspectiveProject(Float[] point, Float[] origin, Float[] rotation, Float fov) {
        //apply rotation matrix based on the opposite of the camera rotation; camera doesnt rotate, everything else does in relation to it.
        //rotation XYZ gives the rotation of a point relative to the camera's location and rotation
        Float[] rotXYZ = {(360 - rotation[0]), (360 - rotation[1]), (360 - rotation[2])};

        Float[] pointCopy = point.clone(); //clone the point so it doesnt change the original

        pointCopy[0] -= origin[0];
        pointCopy[1] -= origin[1];
        pointCopy[2] -= origin[2];
        
        Float pointZ = 
                    //    point[2];
                    pointCopy[0] * ((cos(rotXYZ[0]) * sin(rotXYZ[1]) * cos(rotXYZ[2])) + (sin(rotXYZ[0]) * sin(rotXYZ[2]))) + 
                    pointCopy[1] * ((cos(rotXYZ[0]) * sin(rotXYZ[1]) * sin(rotXYZ[2])) - (sin(rotXYZ[0]) * cos(rotXYZ[2]))) + 
                    pointCopy[2] * ((cos(rotXYZ[0]) * cos(rotXYZ[1])));

        Float pointX = 
                    //    point[0];
                    pointCopy[0] * (cos(rotXYZ[1]) * cos(rotXYZ[2])) + 
                    pointCopy[1] * (cos(rotXYZ[1]) * sin(rotXYZ[2])) + 
                    pointCopy[2] * (-sin(rotXYZ[1]));

        Float pointY = 
                    //    point[1];
                    pointCopy[0] * ((sin(rotXYZ[0]) * sin(rotXYZ[1]) * cos(rotXYZ[2])) - (cos(rotXYZ[0]) * sin(rotXYZ[2]))) + 
                    pointCopy[1] * ((sin(rotXYZ[0]) * sin(rotXYZ[1]) * sin(rotXYZ[2]) + (cos(rotXYZ[0]) * cos(rotXYZ[2])))) +
                    pointCopy[2] * ((sin(rotXYZ[0]) * cos(rotXYZ[1])));

        //index 4 is the w, which will stay 1 until inheriting the view space z, which the x, y, and z are divided by to get the "further back objects are smaller" effect
        //index 5 is the "within bounds" variable, it is 0 if the point is within the view of the camera, and 1 otherwise
        Float[] rotatedPoint = new Float[]{pointX, pointY, pointZ, 1f, 0f};
        //project with perspective projection matrix
        Float[] screenCoords = matrixPerspectiveProject(rotatedPoint);

        return screenCoords;
    }
    //takes 4 index view space coordinate (x, y, z, w) and converts it to clip space coords with a 4x4 perspective projection matrix:
    //[ 1 / (apectRatio * tan(fov/2)),        0,                0,                                     0,                ]
    //[              0,                1 / tan(fov/2),          0,                                     0,                ]
    //[              0,                       0, -(zFar + zNear) / (zFar - zNear), (-2 * zFar * zNear) / (zFar - zNear), ]
    //[              0,                       0,               -1,                                     0                 ]
    //when put through the matrix, the x, y, and z will be normalized between -1 and 1, of which the x and y will be multiplied by the width and height of the screen,
    //this allows them to be used with different screen resolutions, granted the aspect ratio stays the same, though if the aspect ratio changed, the first component can be recalculated
    //to cut down on the amount of math being done, the matrix components are precalculated and retrieved with the matrixComponents[] list.
    //concept and matrix gotten from: https://ogldev.org/www/tutorial12/tutorial12.html, however no code was taken or looked at, going in raw >:)
    public static Float[] matrixPerspectiveProject(Float[] point){
        Float z = point[2]; //temp variable
        point[0] *= matrixComponents[0];                //x
        point[1] *= matrixComponents[1];                //y
        point[2] = (point[2] * matrixComponents[2]) + (point[3] * matrixComponents[3]); //z
        point[3] = -z;                                                                  //w
        //clip space to normalized deivce space from divinding x y and z by w
        point[0] /= point[3];
        point[1] /= point[3];
        point[2] /= point[3];
        //decimals are truncated and centered
        point[0] = (float) Math.floor(point[0]) + (screenDimensions[0] / 2);
        point[1] = (float) Math.floor(point[1]) + (screenDimensions[1] / 2);
        point[2] = (1 - point[2]) * 200;
        // System.out.println(point[2]);

        if (!pointWithinBounds(Game.getScreenDimensions(), point, 0)) {
            point[4] = 1f;
        }
        
        return point;
    }
    //find min and max values in a list, faster than sorting bc you only cycle through once, O(n)
    public static Float[][] maxAndMin(List<Float[]> list) {
        Float[] max = new Float[]{(float) Integer.MIN_VALUE, 0f};
        Float[] min = new Float[]{(float) Integer.MAX_VALUE, 0f};
 
        for(Float[] value: list) {
            if (value[0] > max[0]) {max = value;}
            if (value[0] < min[0]) {min = value;}
        }

        return new Float[][]{min, max};
    }
    //checks if a point is within the bounds of bounds[], restricted to the x and the y 
    //also checks z, index 2, which is a number between 0 and 1 if the point is between the two predefined clip planes, and otherwise under 0 or above 1
    //inclusive should be 0 or 1
    public static boolean pointWithinBounds(int[] bounds, Float[] point, int exclusive) {
        return (point[0] <= bounds[0] - exclusive &&
                point[1] <= bounds[1] - exclusive && 
                point[0] >= exclusive && 
                point[1] >= exclusive &&
                point[2] > 0 && 
                point[2] <= 1);
    }
    //thank you to user7091705 and Quasimondo on stackoverflow
    //https://stackoverflow.com/questions/2630925/whats-the-most-effective-way-to-interpolate-between-two-colors-pseudocode-and/4787257#4787257
    //i hate bitwise
    public static int mixColors(int a, int b, float ratio){
        int mask1 = 0x00ff00ff;
        int mask2 = 0xff00ff00;
    
        int f2 = (int)(256 * ratio);
        int f1 = 256 - f2;
    
        return (((((a & mask1) * f1) + ((b & mask1) * f2)) >> 8) & mask1) 
             | (((((a & mask2) * f1) + ((b & mask2) * f2)) >> 8) & mask2);
    }
    //thanks you to initramfs on stackoverflow
    //https://stackoverflow.com/questions/18022364/how-to-convert-rgb-color-to-int-in-java
    public static int rgbToSRGB(int red, int green, int blue) {
        //color int dynamics: 0xAlpha-Red-Green-Blue

        red = (red << 16) & 0x00FF0000; //shift red 16 bits over and mask it out
        green = (green << 8) & 0x0000FF00; //same with green but shift it 8 bits instead
        blue = blue & 0x000000FF; //same with blue but dont shift it at all

        return 0xFF000000 | red | green | blue; //FF for alpha, then OR everything together
    }

    //to keep things pretty
    public static float sin(Float theta) {
        return (float) Math.sin(Math.PI * theta / 180);
    }
    public static float cos(Float theta) {
        return (float) Math.cos(Math.PI * theta / 180);
    }
}
