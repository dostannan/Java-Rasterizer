/* 
(yes im aware my whole codebase looks like freshly boiled spaghetti this is just practice)
TO-DO LIST:
̶ ̶a̶.̶ ̶g̶e̶t̶ ̶p̶o̶i̶n̶t̶s̶ ̶s̶h̶o̶w̶i̶n̶g̶,̶ ̶3̶d̶ ̶b̶u̶t̶ ̶s̶a̶v̶e̶ ̶z̶ ̶c̶o̶o̶r̶d̶ ̶f̶o̶r̶ ̶l̶a̶t̶e̶r̶,̶ ̶j̶u̶s̶t̶ ̶d̶o̶ ̶o̶r̶t̶h̶o̶g̶r̶a̶p̶h̶i̶c̶
̶ ̶b̶.̶ ̶c̶o̶n̶n̶e̶c̶t̶ ̶w̶i̶t̶h̶ ̶l̶i̶n̶e̶s̶
̶ ̶c̶.̶ ̶s̶c̶a̶n̶ ̶l̶i̶n̶e̶ ̶c̶o̶n̶v̶e̶r̶s̶i̶o̶n̶
̶ ̶d̶.̶ ̶i̶n̶t̶e̶g̶r̶a̶t̶e̶ ̶z̶ ̶c̶o̶o̶r̶d̶,̶ ̶p̶e̶r̶s̶p̶e̶c̶t̶i̶v̶e̶ ̶p̶r̶o̶j̶e̶c̶t̶i̶o̶n̶
̶ ̶e̶.̶ ̶a̶l̶l̶o̶w̶ ̶r̶o̶t̶a̶t̶i̶o̶n̶ ̶a̶n̶d̶ ̶m̶o̶v̶e̶m̶e̶n̶t̶
̶ ̶f̶.̶ ̶d̶e̶p̶t̶h̶ ̶b̶u̶f̶f̶e̶r̶
̶ ̶g̶.̶ ̶p̶e̶r̶s̶p̶e̶c̶t̶i̶v̶e̶ ̶c̶o̶r̶r̶e̶c̶t̶ ̶t̶e̶x̶t̶u̶r̶e̶ ̶m̶a̶p̶p̶i̶n̶g̶
h. fix depth errors at far distances
i. OPTIMIZE PLEASE
j. occlusion culling and frustum culling (do frustum first, just culling offscreen verts isnt working)
k. support for more MTL functions like smooth shading, and ambient, specular, and diffuse colors
*/

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class Game {
    //Game Parameters
        //rendering
        private static boolean justOutline = false;
        private static boolean justDepthBuffer = false;
        private static Float renderDistance = 25f; //maximum z before the object/face wont render
        private static Color backgroundColor = new Color(108, 58, 60);
        private static BufferedImage backgroundImage;
        //loads and holds all textures in the ./textures folder so that they dont have to be loaded on the fly, just referenced
        //the keys in the hash are just the name of the texture file, ie. "texture.jpg" or "wall.png"
        private static HashMap<String, BufferedImage> textureRepo = new HashMap<String, BufferedImage>();
        private static BufferedImage defaultTexture;
        //camera
        private static int[] screenDimensions = new int[] {500, 500};
        private static Player player = new Player(90f, 2f, -0.01f, -40f);
        private static int fps = 30; 
    //---
    
    private static JFrame mainFrame = new JFrame("Game");
    private static JPanel mainPanel;

    //whats drawn on the screen, in this case mainPanel
    private static BufferedImage frameBuffer = new BufferedImage(screenDimensions[0], screenDimensions[1], BufferedImage.TYPE_INT_RGB);;
    //depth buffer, pixels closer to the camera will have a value and color closer to 1 and white, used to put closer things in front of further away things
    private static BufferedImage depthBuffer = new BufferedImage(screenDimensions[0], screenDimensions[1], BufferedImage.TYPE_BYTE_GRAY);

    public static void main(String[] args) {
        //no practical use, just for readability
        clearConsole();
        //create default textures
        try {
            defaultTexture = ImageIO.read(new File("textures/defaultmissing.png"));
            backgroundImage = ImageIO.read(new File("textures/background.png"));
        } catch (IOException e) {}
        
        List<Obj> objsToRender = new ArrayList<Obj>();

        try {
            objsToRender.add(new Obj("test models/cubeTextured.obj"));
        } catch(IOException e) {
            e.printStackTrace();
        }

        mainPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                depthBuffer.getGraphics().clearRect(0, 0, 1000, 1000);

                Graphics frameBufferGraphics = frameBuffer.getGraphics();
                frameBufferGraphics.drawImage(backgroundImage, 0, 0, this);
                // frameBufferGraphics.fillRect(0, 0, 1000, 1000);

                for (Obj object : objsToRender) {
                    Frame.fillObjScanLine(object, depthBuffer, frameBuffer, justOutline);
                }

                if (justDepthBuffer) {
                    g.drawImage(depthBuffer, 0, 0, 1000, 1000, this);
                } else {
                    g.drawImage(frameBuffer, 0, 0, 1000, 1000, this);
                }
            }
        };

        addKeyBindsToFrame(mainFrame);
        mainFrame.setSize(1000, 1000);
        mainFrame.add(mainPanel);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);

        GameLoop();
    }
    //main game loop
    public static void GameLoop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    player.move();
                    player.rotate();

                    mainPanel.repaint();

                    try {
                        TimeUnit.MILLISECONDS.sleep((int) (1000 / fps));
                    } catch (InterruptedException e) {
                        System.out.println("game loop cease");
                    }
                }
            }
        }).start();
    }
    //for readability and easy changeability
    public static void addKeyBindsToFrame(JFrame frame) {
        frame.setFocusTraversalKeysEnabled(false);
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 9: justDepthBuffer = !justDepthBuffer;
                             break;
                    case 87: player.setPosPerFrameChange(null, null, 0.5f); //w
                             break;
                    case 65: player.setPosPerFrameChange(0.5f, null, null); //a
                             break;
                    case 83: player.setPosPerFrameChange(null, null, -0.5f); //s
                             break;
                    case 68: player.setPosPerFrameChange(-0.5f, null, null); //d
                             break;
                    
                    case 81: player.setRotPerFrameChange(null, -3f, null); //q
                             break;
                    case 69: player.setRotPerFrameChange(null, 3f, null); //e
                             break;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 87: player.setPosPerFrameChange(null, null, 0f); //w
                             break;
                    case 65: player.setPosPerFrameChange(0f, null, null); //a
                             break;
                    case 83: player.setPosPerFrameChange(null, null, 0f); //s
                             break;
                    case 68: player.setPosPerFrameChange(0f, null, null); //d
                             break;
                    
                    case 81: player.setRotPerFrameChange(null, 0f, null); //q
                             break;
                    case 69: player.setRotPerFrameChange(null, 0f, null); //e
                             break;
                }
            }
        });
    }

    public static int[] getScreenDimensions() {
        return screenDimensions;
    }

    public static Player getPlayer() {
        return player;
    }

    public static Float getRenderDistance() {
        return renderDistance;
    }

    public static BufferedImage getTextureFromRepo(String fileName) {
        if (fileName != null && textureRepo.containsKey(fileName)) {
            return textureRepo.get(fileName);
        }
        // System.out.println(fileName);
        return defaultTexture;
    }

    public static void addTextureToRepo(String fileName) {
        BufferedImage texture;
        try {
            texture = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.out.println("cant find " + fileName);
            return;
        }

        if (fileName != null && !textureRepo.containsKey(fileName)) {
            textureRepo.put(fileName, texture);
        }
    }

    public static int getSkyColor() {
        return backgroundColor.getRGB();
    } 

    //clears console
    public static void clearConsole() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    } 
}
