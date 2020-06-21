package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.ResponseButton;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class OverlayFrame extends JFrame {
    private Robot robot;
    private float screenSize = 1920;
    private Color backgroundColor = Color.decode("#110E0D");
    private Color altColor = Color.decode("#323534");
    private Color textColor = new Color(220, 220, 220);

    private JFrame markerFrame;

    private boolean persistPosition = PropertyManager.getInstance().getBooleanProp("overlay_persist_pos", false);
    private boolean showMarkerOnEnter = PropertyManager.getInstance().getBooleanProp("overlay_marker_on_enter", false);

    public OverlayFrame(boolean in, String playerName, String item, String price, int x, int y, String stashTab, int stashX, int stashY) {
        super("PoeTradeHelper Overlay");

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setFocusableWindowState(false);
        setFocusable(false);
        setAlwaysOnTop(true);
        setVisible(false);

        // Without this, the window is draggable from any non transparent
        // point, including points  inside textboxes.
        getRootPane().putClientProperty("apple.awt.draggableWindowBackground", false);

        getContentPane().setLayout(new BorderLayout());
        getRootPane().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.DARK_GRAY));

        // Header
        JLabel playerNameLabel = new JLabel(playerName);
        playerNameLabel.setForeground(textColor);
        playerNameLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        playerNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JLabel priceLabel = new JLabel(price, SwingConstants.CENTER);
        priceLabel.setForeground(textColor);

        JButton whois = getButton("?", e -> whois(playerName));
        JButton invite = getButton("+", e -> invite(playerName));
        JButton hideout = getButton("H", e -> hideout(playerName));
        JButton trade = getButton("T", e -> trade(playerName));
        JButton kick = getButton("-", e -> kick(in ? playerName : PropertyManager.getInstance().getPlayerList().get(0)));
        JButton whisper = getButton("W", e -> whisper(playerName, "", false));
        JButton close = getButton("X", e -> dispose());

        JPanel topButtonPanel = new JPanel();
        topButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        if (in) {
            topButtonPanel.add(whois);
            topButtonPanel.add(invite);
        } else {
            topButtonPanel.add(hideout);
        }
        topButtonPanel.add(trade);
        topButtonPanel.add(kick);
        topButtonPanel.add(whisper);
        topButtonPanel.add(close);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout(10, 10));
        headerPanel.setBackground(altColor);
        headerPanel.add(playerNameLabel, BorderLayout.WEST);
        headerPanel.add(priceLabel, BorderLayout.CENTER);
        headerPanel.add(topButtonPanel, BorderLayout.EAST);

        // Center
        JPanel centerPanel = new JPanel();
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        centerPanel.setBackground(backgroundColor);
        JLabel text = new JLabel();
//        text.setForeground(new Color(100,100,100));
        text.setForeground(textColor);
        text.setHorizontalTextPosition(SwingConstants.LEFT);
        if (stashTab != null && !stashTab.isEmpty()) {
            text.setText(item + " - (stashed in " + stashTab + ")");
        } else {
            text.setText(item);
        }
        centerPanel.add(text);

        // Response buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(backgroundColor);

        List<ResponseButton> buttons;
        if (in) {
            buttons = OverlayManager.getInstance().getOverlayConfig().getIncomingButtons();
        } else {
            buttons = OverlayManager.getInstance().getOverlayConfig().getOutgoingButtons();
        }

        for (ResponseButton button : buttons) {
            buttonPanel.add(getButton(button.getLabel(), e -> {
                whisper(playerName, button.getMessage(), true);
                if (button.isClose()) {
                    dispose();
                }
            }));
        }
        getContentPane().add(BorderLayout.NORTH, headerPanel);
        getContentPane().add(BorderLayout.CENTER, centerPanel);
        getContentPane().add(BorderLayout.SOUTH, buttonPanel);

        MouseInputAdapter mouseListener = new MouseInputAdapter() {
            private Point location;
            private MouseEvent pressed;

            @Override
            public void mouseClicked(MouseEvent e) {
                switch (e.getButton()) {
                    case MouseEvent.BUTTON1:
                        if (stashX > 0 && stashY > 0) {
                            if (markerFrame == null || !markerFrame.isVisible()) {
                                renderStashMarker(stashX, stashY);
                            } else {
                                markerFrame.dispose();
                                markerFrame = null;
                            }
                        }
                        break;
                    case MouseEvent.BUTTON3: // right mouse click
                        dispose();
                        break;
                    case MouseEvent.BUTTON2: // middle mouse click
                        OverlayManager.getInstance().disposeAll();
                        break;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = e;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (persistPosition) {
                    OverlayManager.getInstance().setOverlayLocation(getLocation());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (showMarkerOnEnter && stashX > 0 && stashY > 0) {
                    renderStashMarker(stashX, stashY);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {

            }

            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                location = getLocation(location);
                int x = location.x - pressed.getX() + e.getX();
                int y = location.y - pressed.getY() + e.getY();
                setLocation(x, y);
            }
        };
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);

        pack();
        int width = 400;
        setSize(width, 100);
        if (x == -1) {
            x = Math.round(screenSize / 2f - width / 2f);
        }
        setLocation(x, y);
        setVisible(true);
    }

    private JButton getButton(String label, ActionListener listener) {
        JButton button = new JButton();
        button.addActionListener(listener);
        button.setText(label);
        button.setForeground(textColor);
        button.setBackground(Color.decode("#1d1d1d"));
        Border line = new LineBorder(Color.decode("#323534"));
        Border margin = new EmptyBorder(5, 10, 5, 10);
        Border compound = new CompoundBorder(line, margin);
        button.setBorder(compound);
        return button;
    }

    private void whois(String playerName) {
        chatCommand("/whois " + playerName);
    }

    private void whisper(String playerName, String msg, boolean send) {
        chatCommand("@" + playerName + " " + msg, send);
    }

    private void kick(String playerName) {
        chatCommand("/kick " + playerName);
        dispose();
    }

    private void trade(String playerName) {
        chatCommand("/tradewith " + playerName);
    }

    private void invite(String playerName) {
        chatCommand("/invite " + playerName);
    }

    private void hideout(String playerName) {
        chatCommand("/hideout " + playerName);
    }

    public void chatCommand(String msg) {
        chatCommand(msg, true);
    }

    public void chatCommand(String msg, boolean send) {
        StringSelection selection = new StringSelection(msg);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        // Remove old text window content
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyPress(KeyEvent.VK_BACK_SPACE);
        robot.keyRelease(KeyEvent.VK_BACK_SPACE);

        // Paste
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);

        if (send) {
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
        }
    }

    public void renderStashMarker(int x, int y) {
        if (markerFrame == null) {
            markerFrame = new JFrame("Transparent Window");

            markerFrame.getRootPane().setOpaque(false);
            markerFrame.setUndecorated(true);
            markerFrame.setBackground(new Color(0, 0, 0, 0));
            markerFrame.setLocationRelativeTo(null);
            markerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            markerFrame.setFocusableWindowState(false);
            markerFrame.setFocusable(false);
            markerFrame.setAlwaysOnTop(true);
            markerFrame.setVisible(false);
            markerFrame.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    markerFrame.dispose();
                    markerFrame = null;
                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });

            markerFrame.getContentPane().setLayout(new BorderLayout());

            markerFrame.getContentPane().add(getCellPlaceholder());
            markerFrame.pack();

            int size = 53;
            markerFrame.setSize(size, size);
            int xLoc = 14 + size * (x - 1);
            int yLoc = 158 + size * (y - 1);
            markerFrame.setLocation(xLoc, yLoc);
        }

        if (!markerFrame.isVisible()) {
            markerFrame.setVisible(true);
        }
    }

    // Mercury trade creates whole grid and sets visible what is active...
    private JPanel getCellPlaceholder() {
        JPanel cell = new JPanel();
        cell.setOpaque(true);
        cell.setBackground(new Color(50, 0, 0, 0));
        cell.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255)));
        return cell;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (markerFrame != null) {
            markerFrame.dispose();
            markerFrame = null;
        }
    }
}