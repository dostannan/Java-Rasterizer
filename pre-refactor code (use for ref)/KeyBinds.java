import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class KeyBinds {
    Action moveAction;
    Action releaseAction;
    
    public KeyBinds(JPanel panel) {
        moveAction = new MoveAction();
        releaseAction = new ReleaseAction();

        panel.getInputMap().put(KeyStroke.getKeyStroke("W"), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("A"), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("S"), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("D"), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("Q"), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("E"), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("R"), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("F"), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0), "moveAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "moveAction");
        panel.getActionMap().put("moveAction", moveAction);

        panel.getInputMap().put(KeyStroke.getKeyStroke("released W"), "releaseAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("released A"), "releaseAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("released S"), "releaseAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("released D"), "releaseAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("released Q"), "releaseAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("released E"), "releaseAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("released R"), "releaseAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke("released F"), "releaseAction");
        panel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0), "releaseAction");
        panel.getActionMap().put("releaseAction", releaseAction);

    }

    public class MoveAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println(e.getActionCommand());
            // switch (e.getActionCommand()) {
            //     case " ": newMain.movePlayer(null, 0.8f, null);
            //               break;
            //     case "w": newMain.movePlayer(null, null, -1f);
            //               break;
            //     case "a": newMain.movePlayer(1f, null, null);
            //               break;
            //     case "s": newMain.movePlayer(null, null, 1f);
            //               break;
            //     case "d": newMain.movePlayer(-1f, null, null);
            //               break;
            //     case "q": newMain.rotatePlayer(null, 1f, null);
            //               break;
            //     case "e": newMain.rotatePlayer(null, -1f, null);
            //               break;
            //     case "r": newMain.rotatePlayer(-1f, null, null);
            //               break;
            //     case "f": newMain.rotatePlayer(1f, null, null);
            //               break;
            // }
        }
    }

    public class ReleaseAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {

            // switch (e.getActionCommand()) {
            //     case "w": newMain.movePlayer(null, null, 0f);
            //               break;
            //     case "a": newMain.movePlayer(0f, null, null);
            //               break;
            //     case "s": newMain.movePlayer(null, null, 0f);
            //               break;
            //     case "d": newMain.movePlayer(0f, null, null);
            //               break;
            //     case "q": newMain.rotatePlayer(null, 0f, null);
            //               break;
            //     case "e": newMain.rotatePlayer(null, 0f, null);
            //               break;
            //     case "r": newMain.rotatePlayer(0f, null, null);
            //               break;
            //     case "f": newMain.rotatePlayer(0f, null, null);
            //               break;
            // }
        }
    }
}
