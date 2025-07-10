import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import java.lang.Math;
import java.nio.Buffer;
import java.io.File;
import java.io.IOException;

public class newMain {
    public static Random rand = new Random();

    private static JFrame mainFrame = new JFrame();
    private static JPanel mainPanel;

    private static List<Color> currentFaceColors = new ArrayList<Color>();

    private static List<Obj> objsToRender = new ArrayList<Obj>();
    
    private static int framesThisSecond = 0;
    private static int lastFpsCount = 0;

    private static int[] screenDimensions = {500, 500};
    private static BufferedImage zBufferedImage = new BufferedImage(screenDimensions[0], screenDimensions[1], BufferedImage.TYPE_3BYTE_BGR);
    private static BufferedImage displayImage = new BufferedImage(screenDimensions[0], screenDimensions[1], BufferedImage.TYPE_3BYTE_BGR);
    //xyz, allows for slowing and negative directions, like -x and +x
    private static Float[] movingDirections = {0f, 0f, 0f};
    private static Float[] rotateDirections = {0f, 0f, 0f};
    //scalars for moving and rotating directions
    private static Float playerMoveSpeed = 0.6f;
    private static Float playerRotateSpeed = 5f;
    //added to camera Y
    private static Float playerHeight = 2f;

    private static Float[] cameraOrigin = {0f, playerHeight, 0f};
    //used for frustum calculations, will only recalculate if the origin changes between frames
    private static Float[] cameraRot = {0f, 0f, 0f};
    private static Float cameraFov = 180f;

    private static int maxFPS = 30;
    //in m/s^2
    private static Float gravity = 1.5f;

    private static Color backgroundColor = new Color(0, 0, 0);

    private static Float friction = 0.06f;

    private static HashSet<Integer> hashYIndexList = new HashSet<Integer>();
    private static Map<Integer, List<xz>> xAtY = new HashMap<Integer, List<xz>>();

    public static void main(String[] args) {
        // for (int i = 0; i < 1000; i++) {
        //     currentFaceColors.add(new Color(rand.nextInt(254), rand.nextInt(254), rand.nextInt(254)));
        // }

        final Obj cube;
        final Obj sphere;
        final Obj wow;
        final Obj floor;

        try {
            cube = new Obj("C:/Users/dayto/Downloads/projects/java project/test models/floor.obj");
            // sphere = new Obj("sphere.obj");
            // wow = new Obj("wow.obj");
            // floor = new Obj("test.obk.obj");
        } catch (IOException e) {
            throw new RuntimeException("no obj found!");
        }
        //also ill add the test objects back
        objsToRender.add(cube);
        // objsToRender.add(floor);
        // objsToRender.add(sphere);    
        // objsToRender.add(wow);

        // Image textureImage;
        // try {
        //     textureImage = ImageIO.read(new File("texture.jpg"));
        // } catch (IOException e) {
        //     throw new RuntimeException("no texture");
        // }
        // BufferedImage texture = (BufferedImage) textureImage;
        
        mainPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D zGraphics = zBufferedImage.createGraphics();
                Graphics2D displayGraphics = displayImage.createGraphics();
                
                // graphics.setBackground(backgroundColor);
                zGraphics.clearRect(0, 0, screenDimensions[0], screenDimensions[1]);
                displayGraphics.clearRect(0, 0, screenDimensions[0], screenDimensions[1]);

                for (Obj object : objsToRender) {
                    List<int[]> faceIndexes = object.getFaceIndexes();
                    List<int[]> uvIndexes = object.getUVIndexes();
                    List<Float[]> screenVertices = convertToScreenPos(cameraOrigin, cameraRot, cameraFov, object.getVertices());
                    List<Float[]> uvCoords = object.getUVCoords();
                    // List<Float[]> faceVerts;

                    OUTERLOOP: for (int[] indexList : faceIndexes) {

                        int startY = screenDimensions[1];
                        int endY = 0;
                        
                        int outOfBounds = 0;
                        for (int index : indexList) {
                            outOfBounds += screenVertices.get(index - 1)[3];
                        }

                        if (outOfBounds == indexList.length) {
                            continue;
                        }

                        xAtY = new HashMap<Integer, List<xz>>();
                        // for (int y : hashYIndexList) {
                        //     xAtY.get(y).clear();
                        // }
                        // hashYIndexList.clear();
                        //calc z for each LINE pixel, then use the z's for each group of x's/y to get the slope PER SCAN LINE, then use that to get the z for each pixel
                        INNERLOOP: for (int i = 0; i < indexList.length; i++) {
                            int i2 = i != indexList.length - 1 ? i + 1 : 0;
                            
                            Float[] vert1 = screenVertices.get(indexList[i] - 1);
                            Float[] vert2 = screenVertices.get(indexList[i2] - 1);
                            Float[] vert1UV = uvCoords.get(indexList[i] - 1);
                            Float[] vert2UV = uvCoords.get(indexList[i2] - 1);

                            if (vert1[1] < startY) {
                                startY = vert1[1].intValue();
                            } 
                            if (vert1[1] > endY) {
                                endY = vert1[1].intValue();
                            }

                            Float slope = (vert1[1] - vert2[1]) / (vert1[0] - vert2[0]);
                            Float zSlope = (vert1[2] - vert2[2]) / (vert1[0] - vert2[0]);

                            Float uSlope = (vert1UV[0] - vert2UV[0]) / (vert1[0] - vert2[0]);
                            Float vSlope = (vert1UV[1] - vert2UV[1]) / (vert1[1] - vert2[1]);

                            int steps;
                            //if delta x is greater than delta y (less than 1), iterate over x, else (greater than 1) iterate over y
                            if (Math.abs(slope) <= 1) {
                                steps = (int) (vert1[0] - vert2[0]);

                                // System.out.println(zSlope + " x " + vert1[2] + ", " + vert2[2]);

                                // if (vert1[0] < 0) {
                                //     startValue = (int) Math.abs(vert1[0]);
                                // } else if (vert1[0] > screenDimensions[0]) {
                                //     continue;
                                // } else if (vert1[0] + steps > screenDimensions[0]) {
                                //     steps = (int) (screenDimensions[0] - vert1[0]);
                                // }
        
                                for (int x = 0; x != steps + Math.signum(steps); x += steps / Math.abs(steps)) {
                                    int screenX = vert2[0].intValue() + x;
                                    Integer screenY = (int) (vert2[1] + (slope * x));
                                    Float screenZ = (vert2[2] + (zSlope * x));

                                    Float pointU = uSlope * x  + vert1UV[0];
                                    Float pointV = vSlope * (screenY - vert2[1])  + vert1UV[1];
                                    // if (screenZ > -0.1) {
                                    //     continue INNERLOOP;
                                    // }

                                    // if (screenZ > 0) {
                                    //     continue;
                                    // }
                                    
                                    // image.setRGB(screenX, screenY, Color.BLACK.getRGB());
                                    if (xAtY.get(screenY) == null) {
                                        xAtY.put(screenY, new ArrayList<xz>());
                                    }
                                    // if (!hashYIndexList.contains(screenY)) {
                                    //     hashYIndexList.add(screenY);
                                    // }
                                    xAtY.get(screenY).add(new xz(screenX, screenY, screenZ, pointU, pointV));
                                }
        
                            } else if (Math.abs(slope) > 1) {
                                slope = (vert1[0] - vert2[0]) / (vert1[1] - vert2[1]);
                                zSlope = (vert1[2] - vert2[2]) / (vert1[1] - vert2[1]);

                                steps = (int) (vert1[1] - vert2[1]);

                                // if (vert1[1] < 0) {
                                //     startValue = (int) Math.abs(vert1[1]);
                                // } else if (vert1[1] > screenDimensions[1]) {
                                //     continue;
                                // } else if (vert1[1] + steps > screenDimensions[1]) {
                                //     steps = (int) (screenDimensions[1] - vert1[1]);
                                // }

                                // System.out.println(vert1[1] + ", " + vert2[1]);

                                // System.out.println(zSlope + " y" + vert1[2] + ", " + vert2[2]);
        
                                for (int y = 0; y != steps + Math.signum(steps); y += steps / Math.abs(steps)) {
                                    int screenX = (int) (vert2[0] + (slope * y));
                                    Integer screenY = vert2[1].intValue() + y;
                                    Float screenZ = (vert2[2] + (zSlope * y));

                                    Float pointU = uSlope * (screenX - vert2[0]) + vert1UV[0];
                                    Float pointV = vSlope * y + vert1UV[1];

                                    // if (screenZ > -1) {
                                    //     ;
                                    // }

                                    // if (screenZ > 0) {
                                    //     continue;
                                    // }

                                    // image.setRGB(screenX, screenY, Color.BLACK.getRGB());
                                    if (xAtY.get(screenY) == null) {
                                        xAtY.put(screenY, new ArrayList<xz>());
                                    }
                                    // if (!hashYIndexList.contains(screenY)) {
                                    //     hashYIndexList.add(screenY);
                                    // }
                                    xAtY.get(screenY).add(new xz(screenX, screenY, screenZ, pointU, pointV));
                                    
                                }
                            }
                        }
                        List<xz> xList;
                        int size;
                        int changeInX;
                        int changeInY;
                        Float xzSlope;
                        xz point1;
                        xz point2;
                        Float textureXSlope;
                        Float textureYSlope;
                        for (int y = Math.max(startY, 1); y <= Math.min(screenDimensions[1] - 1, endY); y++) {
                            xList = xAtY.get(y);

                            if (xList == null) {
                                continue;
                            }

                            size = xList.size();

                            Collections.sort(xList, xz.xzCompare);

                            point1 = xList.get(size - 1);
                            point2 = xList.get(0);

                            changeInX = (point1.getX() - point2.getX());

                            // System.out.println(changeInX + ", " + (endY - startY));

                            if (changeInX == 0) {
                                changeInX = 1;
                            }

                            textureXSlope = (point1.getU() - point2.getU()) / (changeInX);
                            textureYSlope = (point1.getV() - point2.getV()) / (changeInX);

                            // if (y == startY + 1) {
                            //     System.out.println(textureYSlope);
                            // }

                            if (changeInX == 0) {
                                xzSlope = 0f;
                            } else {
                                xzSlope = ((point1.getZ() - point2.getZ()) / changeInX);
                            }
                            int count = 0;
                            for (int x = Math.max(point2.getX(), 0); x <= Math.min(screenDimensions[0] - 1, point1.getX()); x++) {
                                Float screenZ = (point2.getZ() + (x - point2.getX()) * xzSlope) / -20;

                                // if (y == startY && x == point2.getX()) {
                                //     System.out.println(point1.getV() + ", " + point2.getV());
                                // }   
                                
                                count += 1;
                                if (screenZ < (0.01)) {
                                    continue;
                                }

                                int color = 255 - (int) (255 * screenZ);
                                if ((zBufferedImage.getRGB(x, y) >> 16 & 0xFF) < color) {
                                    zBufferedImage.setRGB(x, y, getIntFromColor(color, color, color));
                                    displayImage.setRGB(x, y, getIntFromColor((int) ((point1.getU()) * 254), (int) ((point1.getV()) * 254), 0));
                                }
                            }
                            System.out.println(count);
                        }
                    }
                }
                g.drawImage(zBufferedImage, 0, 0, this); 
                zGraphics.dispose();
                displayGraphics.dispose();
            }
        };
        mainFrame.setFocusTraversalKeysEnabled(false);
        mainFrame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 87: movePlayer(null, null, -0.5f); //w
                             break;
                    case 65: movePlayer(0.5f, null, null); //a
                             break;
                    case 83: movePlayer(null, null, 0.5f); //s
                             break;
                    case 68: movePlayer(-0.5f, null, null); //d
                             break;
                    
                    case 81: rotatePlayer(null, 1f, null); //q
                             break;
                    case 69: rotatePlayer(null, -1f, null); //e
                             break;
                    
                    // case 82: rotatePlayer(-1f, null, null); //r
                            //  break;
                    // case 70: rotatePlayer(1f, null, null); //f
                            //  break;

                    case 32: movePlayer(null, 0.4f, null); //space
                             break;
                    case 16: changePlayerSpeed(1.2f); //shift
                             break;
                    case 17: changePlayerHeight(1f);
                             break;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 87: movePlayer(null, null, 0f); //w
                             break;
                    case 65: movePlayer(0f, null, null); //a
                             break;
                    case 83: movePlayer(null, null, 0f); //s
                             break;
                    case 68: movePlayer(0f, null, null); //d
                             break;
                    
                    case 81: rotatePlayer(null, 0f, null); //q
                             break;
                    case 69: rotatePlayer(null, 0f, null); //e
                             break;
                    
                    // case 82: rotatePlayer(0f, null, null); //r
                            //  break;
                    // case 70: rotatePlayer(0f, null, null); //f
                            //  break;

                    case 16: changePlayerSpeed(0.6f); //shift
                             break;
                    case 17: changePlayerHeight(2f);
                             break;
                }
            }
        }); 

        mainFrame.setSize(screenDimensions[0], screenDimensions[1]);
        mainFrame.add(mainPanel);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);

        GameLoop();
    }

    private static void GameLoop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Float lastDirX = 0f;
                Float lastDirZ = 0f;
                while(true) {
                    framesThisSecond++;
                    int frameTimeGap = (int)(1000 / maxFPS);

                    //dx, dy, and dz are local (based on the cameras rotation), and will get turned into the global coordinates that will be added to the cameras global coordinates
                    //dy (up and down) stays the same because camera wont roll

                    if (movingDirections[2] != 0f) {
                        lastDirZ += Math.abs(lastDirZ) < Math.abs(movingDirections[2]) ? friction * Math.signum(movingDirections[2]) : 0f;
                    } else {
                        if (Math.signum(lastDirZ) == Math.signum(lastDirZ - (friction * Math.signum(lastDirZ)))) {
                            lastDirZ -= friction * Math.signum(lastDirZ);
                        } else {
                            lastDirZ = 0f;
                        }
                    }

                    if (movingDirections[0] != 0f) {
                        lastDirX += Math.abs(lastDirX) < Math.abs(movingDirections[0]) ? friction * Math.signum(movingDirections[0]) : 0f;
                    } else {
                        if (Math.signum(lastDirX) == Math.signum(lastDirX - (friction * Math.signum(lastDirX)))) {
                            lastDirX -= friction * Math.signum(lastDirX);
                        } else {
                            lastDirX = 0f;
                        }
                    }

                    Float globalVectX = ((-lastDirZ * sin(cameraRot[1])) + (lastDirX * cos(cameraRot[1]))) * playerMoveSpeed;
                    Float globalVectZ = ((lastDirZ * cos(cameraRot[1])) + (lastDirX * sin(cameraRot[1]))) * playerMoveSpeed;
                    
                    cameraOrigin[0] += globalVectX;
                    cameraOrigin[2] += globalVectZ;
                    
                    cameraOrigin[1] += movingDirections[1];

                    if (cameraOrigin[1] < playerHeight) {
                        cameraOrigin[1] = playerHeight;
                        movingDirections[1] = 0f;
                    } else {
                        movingDirections[1] -= gravity * frameTimeGap / 1000;
                    }

                    // if (cameraOrigin != lastFrameCameraOrigin) {
                    // frustum.updateFrustum(cameraOrigin, 100f, 10f);
                    //     lastFrameCameraOrigin = cameraOrigin;
                    // }

                    cameraRot[0] += rotateDirections[0] * playerRotateSpeed;
                    cameraRot[1] += rotateDirections[1] * playerRotateSpeed;
                    cameraRot[2] += rotateDirections[2] * playerRotateSpeed;
                    mainPanel.repaint();
                    //~30 fps
                    try {
                        TimeUnit.MILLISECONDS.sleep(frameTimeGap);
                    } catch (InterruptedException e) {}
                }
            }
        }).start();
        //fps counter
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {}
                    lastFpsCount = framesThisSecond;
                    framesThisSecond = 0;
                }
            }
        }).start();
    }
    //converts 3d world coordinates to 2d screen pane coordinates
    private static List<Float[]> convertToScreenPos(Float[] origin, Float[] rotation, Float fov, List<Float[]> points) {
        //apply rotation matrix based on camera "rotation"; camera doesnt rotate, everything else does in relation to it.
        //rotation XYZ gives the rotation of a point relative to the camera's location and rotation
        //no need to convert to radians as the cos and sin functions already do that
        //https://math.stackexchange.com/questions/1882276/combining-all-three-rotation-matrices
        List<Float[]> screenPoints = new ArrayList<Float[]>();
        Float[] rotXYZ = {(360f - rotation[0]), (360f - rotation[1]), (360f - rotation[2])};
        for (Float[] pointToClone : points) {
            Float[] point = pointToClone.clone();
            point[0] -= origin[0];
            point[1] -= origin[1];
            point[2] -= origin[2];
            
            Float pointZ = 
                        //    point[2];
                        point[0] * ((cos(rotXYZ[0]) * sin(rotXYZ[1]) * cos(rotXYZ[2])) + (sin(rotXYZ[0]) * sin(rotXYZ[2]))) + 
                        point[1] * ((cos(rotXYZ[0]) * sin(rotXYZ[1]) * sin(rotXYZ[2])) - (sin(rotXYZ[0]) * cos(rotXYZ[2]))) + 
                        point[2] * ((cos(rotXYZ[0]) * cos(rotXYZ[1])));

            Float pointX = 
                        //    point[0];
                        point[0] * (cos(rotXYZ[1]) * cos(rotXYZ[2])) + 
                        point[1] * (cos(rotXYZ[1]) * sin(rotXYZ[2])) + 
                        point[2] * (-sin(rotXYZ[1]));

            Float pointY = 
                        //    point[1];
                        point[0] * ((sin(rotXYZ[0]) * sin(rotXYZ[1]) * cos(rotXYZ[2])) - (cos(rotXYZ[0]) * sin(rotXYZ[2]))) + 
                        point[1] * ((sin(rotXYZ[0]) * sin(rotXYZ[1]) * sin(rotXYZ[2]) + (cos(rotXYZ[0]) * cos(rotXYZ[2])))) +
                        point[2] * ((sin(rotXYZ[0]) * cos(rotXYZ[1])));

            // //checks if point is in the frustum, if not it gives a point off screen that will be culled in repaint()
            // if (checkFrustum && !frustum.isPointInFrustum(point)) {
            //     // return new Float[]{screenDimensions[0] + 10f, screenDimensions[1] + 10f};
            //     lineColors.add(new Color(255, 0, 0));
            // } else {
            //     lineColors.add(new Color(0, 0, 0));
            // }

            //math fuckery, thank you turtleAndrew on replit
            //https://replit.com/talk/learn/Highwayman-Thats-all-good-Its-just-a/57491/491601

            Float mX;
            Float mY;

            Float screenZ = pointZ;

            if (pointZ > -0.1f) {
                // pointZ = -0f;
                mX = -pointX * 10;
                mY = -pointY * 10;
            }  else {
                mX = pointX / pointZ;
                mY = pointY / pointZ;
            }

            Float outOfBounds = 0f;

            Float screenX = (mX * fov) + pointX + (screenDimensions[0] / 2);
            Float screenY = (mY * fov) + pointY + (screenDimensions[1] / 2);

            if ((screenX < 0 || screenX > screenDimensions[0]) &&
                (screenY < 0 || screenY > screenDimensions[0])) {
                outOfBounds = 1f;    
            }

            // screenZ = Math.max(Math.min(pointZ, 1), -20);
            // System.out.println(screenZ);

            screenPoints.add(new Float[]{ screenX, screenY, screenZ, outOfBounds });
        }
        return screenPoints;
    } 
    //deep copy so that copied lists dont just point to the same address in memory, its an actual copy
    public static List<Float[]> deepCopyList(List<Float[]> listToCopy) {
        List<Float[]> copiedList = new ArrayList<Float[]>();
        for(Float[] item : listToCopy) {
            copiedList.add(item.clone());
        }
        return copiedList;
    }

    public static void movePlayer(Float dx, Float dy, Float dz) {
        if (cameraOrigin[1] <= playerHeight) {
            movingDirections[1] = dy != null ? dy : movingDirections[1];
        }

        movingDirections[0] = dx != null ? dx : movingDirections[0];
        movingDirections[2] = dz != null ? dz : movingDirections[2];
    }

    public static void rotatePlayer(Float dThetaX, Float dThetaY, Float dThetaZ) {
        rotateDirections[0] = dThetaX != null ? dThetaX : rotateDirections[0];
        rotateDirections[1] = dThetaY != null ? dThetaY : rotateDirections[1];
        rotateDirections[2] = dThetaZ != null ? dThetaZ : rotateDirections[2];
    }

    public static void changePlayerSpeed(Float speed) {
        playerMoveSpeed = speed;
    }

    public static void changePlayerHeight(Float newHeight) {
        playerHeight = Math.abs(newHeight);
    }

    public static Float getDistanceToPlayer(Float[] point1) {
        return (float) Math.sqrt(Math.pow(cameraOrigin[0] - point1[0], 2) + Math.pow(cameraOrigin[1] - point1[1], 2) + Math.pow(cameraOrigin[2] - point1[2], 2));
    }

    //for readability, all take degrees and return degrees
    public static Float cos(Float theta) {return (float)Math.cos(theta * Math.PI / 180);}
    public static Float sin(Float theta) {return (float)Math.sin(theta * Math.PI / 180);}
    public static Float tan(Float theta) {return (float)Math.tan(theta * Math.PI / 180);}

    public static int[] screenSize() {
        return screenDimensions;
    }

    public static int getIntFromColor(int Red, int Green, int Blue){
        Red = (Red << 16) & 0xFF0000; //Shift red 16-bits and mask out other stuff
        Green = (Green << 8) & 0x00FF00; //Shift Green 8-bits and mask out other stuff
        Blue = Blue & 0x0000FF; //Mask out anything not blue.
    
        return 0x000000 | Red | Green | Blue;
    }
}
//im making a 3d game from scratch, which means i take the vertices from all the objects in the game, comvert them to screen coordinates, and draw them, all manually.