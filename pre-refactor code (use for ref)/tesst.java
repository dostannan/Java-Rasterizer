import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class tesst {
    public static JFrame frame = new JFrame();
    public static JPanel panel;
    public static BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_3BYTE_BGR);
    public static List<int[]> triangleIndexes = Arrays.asList(new int[][] {
        {1, 2, 4, 3}
    });
    public static List<Float[]> trianglePoints = Arrays.asList(new Float[][] {
        {100f, 100f, 5f},
        {42.6f, 30.513f, 1f},
        {9.8889f, 60.30f, 1f},
        {15.382f, 150.109f, 7f}
    });

    public static void main(String[] args) {
        System.out.println((200 << 16) & 0x00FF0000);
        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                image = new BufferedImage(500, 500, BufferedImage.TYPE_3BYTE_BGR);
                //sorts from least y to most y
                trianglePoints.sort(Comparator.comparing(a -> a[1]));

                int StartY = (int) Math.floor(trianglePoints.get(0)[1]);
                int EndY = (int) Math.floor(trianglePoints.get(trianglePoints.size() - 1)[1]);

                // @SuppressWarnings("unchecked")
                Map<Integer, List<Integer>> xAtY = new HashMap<Integer, List<Integer>>(); 

                for (int i = StartY; i <= EndY; i++) {
                    xAtY.put(i, new ArrayList<Integer>());
                }
                Float[] vert;
                //for pixel z calculation
                Float leastX = (float) 500;
                Float mostX = (float) 0f;

                Float z1 = 0f;
                Float z2 = 0f;
                Float slopeZ = 0f;
                for (int[] faceIndeces : triangleIndexes) {
                    for (int index : faceIndeces) {
                        vert = trianglePoints.get(index - 1);
                        if (vert[0] == null) {
                            continue;
                        }
                        if (vert[0] < leastX) {
                            leastX = vert[0];
                            z1 = vert[2];
                        } else if (vert[0] > mostX) {
                            mostX = vert[0];
                            z2 = vert[2];
                        }
                    }
                    slopeZ = (z2 - z1) / (mostX - leastX);
                    for (int i = 0; i < faceIndeces.length; i++) {
                        int i2 = i != faceIndeces.length - 1 ? i + 1 : 0;
                        Float[] vert1 = trianglePoints.get(faceIndeces[i] - 1);
                        Float[] vert2 = trianglePoints.get(faceIndeces[i2] - 1);
                        
                        Float slope = (vert1[1] - vert2[1]) / (vert1[0] - vert2[0]);

                        int screenX = 0;
                        int screenY = 0;

                        int steps;
                        //if delta x is greater than delta y (less than 1) iterate over x, else (greater than 1) iterate over y
                        if (Math.abs(slope) <= 1) {
                            steps = Math.round(vert1[0] - vert2[0]);

                            for (int x = 0; x != steps + Math.signum(steps); x += steps / Math.abs(steps)) {
                                screenX = (int) (vert2[0] + x);
                                screenY = (int) (vert2[1] + (slope * x));
                                image.setRGB(screenX, screenY, Color.WHITE.getRGB());
                                xAtY.get(screenY).add(screenX);
                            }

                        } else if (Math.abs(slope) > 1) {
                            slope = (vert1[0] - vert2[0]) / (vert1[1] - vert2[1]);
                            steps = Math.round(vert1[1] - vert2[1]);

                            for (int y = 0; y != steps + Math.signum(steps); y += steps / Math.abs(steps)) {
                                screenX = (int) (vert2[0] + (slope * y));
                                screenY = (int) (vert2[1] + y);
                                if (y == -88) {
                                    System.out.println(screenX + ", " + screenY + ", " + steps);
                                }
                                image.setRGB(screenX, screenY, Color.WHITE.getRGB());
                                xAtY.get(screenY).add(screenX);
                            }
                        }
                    }

                }
                List<Integer> xList;
                int size;
        
                System.out.println(slopeZ + ", " + mostX + ", " + z1 + ", " + z2);
                for (int y = StartY; y < EndY; y++) {
                    xList = xAtY.get(y);
                    Collections.sort(xList);
                    size = xList.size();
                    
                    if (size == 0) {
                        continue;
                    }

                    for (int x = xList.get(0); x < xList.get(size - 1); x++) {
                        Float screenZ = (Math.min((slopeZ * (x - leastX)), 1)) * 254;
                        
                        image.setRGB(x, y, screenZ.intValue());
                    }
                }
                g.drawImage(image, 0, 0, this);
            }
        };

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.add(panel);
        frame.setVisible(true);

        gameLoop();
    }

    private static void gameLoop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                panel.repaint();
                try {
                    TimeUnit.MILLISECONDS.sleep(30);
                } catch (InterruptedException e) {}
            }
        }).start();
    }
}
