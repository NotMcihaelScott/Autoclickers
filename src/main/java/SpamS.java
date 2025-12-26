import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpamS extends JFrame implements NativeKeyListener, NativeMouseInputListener {
    private Robot robot;
    private volatile boolean isRunning = false;
    private boolean isRecording = false;
    private ScheduledExecutorService executor;

    private List<RecordedEvent> recordedEvents = new ArrayList<>();
    private long startTime;

    private JComboBox<String> modeCombo;
    private JComboBox<String> mouseBtnCombo;
    private JTextField keyInput, loopInput;
    private JSlider speedSlider;
    private JLabel statusLabel;
    private JCheckBox shiftCheck, ctrlCheck;

    public SpamS() {
        try {
            robot = new Robot();
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);
        } catch (Exception e) { e.printStackTrace(); }
        initUI();
    }

    private void initUI() {
        setTitle("AutoClicker & Recorder Pro");
        setSize(400, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        mainPanel.add(new JLabel("Select Mode:"));
        modeCombo = new JComboBox<>(new String[]{"Keynotes", "Click Mouse", "Record & Playback"});
        modeCombo.setMaximumSize(new Dimension(400, 30));
        mainPanel.add(modeCombo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        mainPanel.add(new JLabel("Letter (for Keynotes) / Button Mouse (for Click):"));
        keyInput = new JTextField("S");
        keyInput.setMaximumSize(new Dimension(400, 30));
        mainPanel.add(keyInput);

        mouseBtnCombo = new JComboBox<>(new String[]{"Left", "Right", "Middle"});
        mouseBtnCombo.setMaximumSize(new Dimension(400, 30));
        mainPanel.add(mouseBtnCombo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel loopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loopPanel.add(new JLabel("Repeat Record (0 = Infinit):"));
        loopInput = new JTextField("1", 5);
        loopPanel.add(loopInput);
        mainPanel.add(loopPanel);

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        shiftCheck = new JCheckBox("with Shift"); ctrlCheck = new JCheckBox("with Ctrl");
        checkPanel.add(shiftCheck); checkPanel.add(ctrlCheck);
        mainPanel.add(checkPanel);

        mainPanel.add(new JLabel("Speed Spammer (ms):"));
        speedSlider = new JSlider(1, 1000, 10);
        mainPanel.add(speedSlider);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        statusLabel = new JLabel("STATUS: STOPPED", SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(new JLabel("F9 = Record | ` = Play/Stop Spammer", SwingConstants.CENTER));

        add(mainPanel);
        setVisible(true);
    }

    private void handleToggleRecording() {
        isRecording = !isRecording;
        if (isRecording) {
            recordedEvents.clear();
            startTime = System.currentTimeMillis();
            statusLabel.setText("RECORDING...");
            statusLabel.setForeground(Color.ORANGE);
        } else {
            statusLabel.setText("RECORD SALVED");
            statusLabel.setForeground(Color.BLUE);
        }
    }

    private void handleTogglePlayback() {
        if (isRunning) {
            stopAction();
            statusLabel.setText("STOPPED");
            statusLabel.setForeground(Color.RED);
            isRunning = false;
        } else {
            isRunning = true;
            statusLabel.setText("RUNNING...");
            statusLabel.setForeground(new Color(0, 128, 0));
            startAction();
        }
    }

    private void startAction() {
        String mode = (String) modeCombo.getSelectedItem();

        if ("Record & Playback".equals(mode)) {
            int loops;
            try { loops = Integer.parseInt(loopInput.getText()); } catch (Exception e) { loops = 1; }
            final int finalLoops = loops;

            new Thread(() -> {
                int count = 0;
                while (isRunning && (finalLoops == 0 || count < finalLoops)) {
                    for (int i = 0; i < recordedEvents.size(); i++) {
                        if (!isRunning) break;
                        RecordedEvent ev = recordedEvents.get(i);
                        long delay = (i == 0) ? ev.timestamp : ev.timestamp - recordedEvents.get(i - 1).timestamp;
                        try { Thread.sleep(Math.max(0, delay)); } catch (Exception e) {}
                        simulateRecordedEvent(ev);
                    }
                    count++;
                }
                isRunning = false;
                SwingUtilities.invokeLater(() -> { statusLabel.setText("FINISHED"); statusLabel.setForeground(Color.RED); });
            }).start();
        } else {
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(this::executeSpam, 0, speedSlider.getValue(), TimeUnit.MILLISECONDS);
        }
    }

    private void executeSpam() {
        try {
            if (shiftCheck.isSelected()) robot.keyPress(KeyEvent.VK_SHIFT);
            if (ctrlCheck.isSelected()) robot.keyPress(KeyEvent.VK_CONTROL);

            String mode = (String) modeCombo.getSelectedItem();
            if ("Keynotes".equals(mode)) {
                String text = keyInput.getText().trim().toUpperCase();
                if (!text.isEmpty()) {
                    int code = KeyEvent.getExtendedKeyCodeForChar(text.charAt(0));
                    robot.keyPress(code);
                    robot.keyRelease(code);
                }
            } else {
                String mouseBtn = (String) mouseBtnCombo.getSelectedItem();
                int mask = InputEvent.BUTTON1_DOWN_MASK;
                if ("Right".equals(mouseBtn)) mask = InputEvent.BUTTON3_DOWN_MASK;
                if ("Middle".equals(mouseBtn)) mask = InputEvent.BUTTON2_DOWN_MASK;
                robot.mousePress(mask);
                robot.mouseRelease(mask);
            }

            if (ctrlCheck.isSelected()) robot.keyRelease(KeyEvent.VK_CONTROL);
            if (shiftCheck.isSelected()) robot.keyRelease(KeyEvent.VK_SHIFT);
        } catch (Exception e) {}
    }

    private void simulateRecordedEvent(RecordedEvent ev) {
        try {
            if (ev.type.equals("move")) robot.mouseMove(ev.x, ev.y);
            else if (ev.type.equals("m_press")) robot.mousePress(getMouseMask(ev.value));
            else if (ev.type.equals("m_release")) robot.mouseRelease(getMouseMask(ev.value));
            else if (ev.type.equals("k_press")) {
                int code = NativeToAwtKey(ev.value);
                if (code != 0) robot.keyPress(code);
            }
            else if (ev.type.equals("k_release")) {
                int code = NativeToAwtKey(ev.value);
                if (code != 0) robot.keyRelease(code);
            }
        } catch (Exception e) {}
    }

    private int getMouseMask(int btn) {
        if (btn == 2) return InputEvent.BUTTON3_DOWN_MASK;
        if (btn == 3) return InputEvent.BUTTON2_DOWN_MASK;
        return InputEvent.BUTTON1_DOWN_MASK;
    }

    private int NativeToAwtKey(int nativeCode) {
        // Mapare simplificată pentru caractere de bază
        switch (nativeCode) {
            case NativeKeyEvent.VC_W: return KeyEvent.VK_W;
            case NativeKeyEvent.VC_A: return KeyEvent.VK_A;
            case NativeKeyEvent.VC_S: return KeyEvent.VK_S;
            case NativeKeyEvent.VC_D: return KeyEvent.VK_D;
            case NativeKeyEvent.VC_SPACE: return KeyEvent.VK_SPACE;
            case NativeKeyEvent.VC_ENTER: return KeyEvent.VK_ENTER;
            case NativeKeyEvent.VC_1: return KeyEvent.VK_1;
            case NativeKeyEvent.VC_2: return KeyEvent.VK_2;
            case NativeKeyEvent.VC_3: return KeyEvent.VK_3;
            default: return 0; // Pentru restul tastelor e nevoie de o mapare completă
        }
    }

    private void stopAction() {
        if (executor != null) executor.shutdownNow();
    }

    @Override public void nativeMouseMoved(NativeMouseEvent e) { if (isRecording) record(e.getX(), e.getY(), "move", 0); }
    @Override public void nativeMousePressed(NativeMouseEvent e) { if (isRecording) record(e.getX(), e.getY(), "m_press", e.getButton()); }
    @Override public void nativeMouseReleased(NativeMouseEvent e) { if (isRecording) record(e.getX(), e.getY(), "m_release", e.getButton()); }
    @Override public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_BACKQUOTE) handleTogglePlayback();
        else if (e.getKeyCode() == NativeKeyEvent.VC_F9) handleToggleRecording();
        else if (isRecording) record(0, 0, "k_press", e.getKeyCode());
    }
    @Override public void nativeKeyReleased(NativeKeyEvent e) { if (isRecording && e.getKeyCode() != NativeKeyEvent.VC_F9) record(0, 0, "k_release", e.getKeyCode()); }

    private void record(int x, int y, String type, int value) {
        recordedEvents.add(new RecordedEvent(System.currentTimeMillis() - startTime, x, y, type, value));
    }

    public static void main(String[] args) {
        try { GlobalScreen.registerNativeHook(); } catch (Exception e) {}
        SpamS app = new SpamS();
        GlobalScreen.addNativeKeyListener(app);
        GlobalScreen.addNativeMouseMotionListener(app);
        GlobalScreen.addNativeMouseListener(app);
    }

    private static class RecordedEvent {
        long timestamp; int x, y, value; String type;
        RecordedEvent(long t, int x, int y, String tp, int v) { this.timestamp = t; this.x = x; this.y = y; this.type = tp; this.value = v; }
    }
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}
    @Override public void nativeMouseClicked(NativeMouseEvent e) {}
    @Override public void nativeMouseDragged(NativeMouseEvent e) {}
}