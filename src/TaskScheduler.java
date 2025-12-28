import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer; // Explicit import to avoid conflict

public class TaskScheduler extends JFrame {

    // --- Data Structures ---
    static class Process {
        String pid;
        int arrivalTime;
        int burstTime;
        int remainingTime;
        int executedTime; // For progress bar tracking
        int completionTime;
        int turnAroundTime;
        int waitingTime;
        Color color;

        public Process(String pid, int at, int bt, Color c) {
            this.pid = pid;
            this.arrivalTime = at;
            this.burstTime = bt;
            this.remainingTime = bt;
            this.executedTime = 0;
            this.color = c;
        }

        // Reset state for re-simulation
        public void reset() {
            this.remainingTime = burstTime;
            this.executedTime = 0;
            this.completionTime = 0;
            this.turnAroundTime = 0;
            this.waitingTime = 0;
        }
    }

    // Represents a slice of execution for playback
    static class ExecutionStep {
        String pid;
        int time; // The specific second this step represents
        Color color;

        public ExecutionStep(String pid, int time, Color color) {
            this.pid = pid;
            this.time = time;
            this.color = color;
        }
    }

    // --- UI Components ---
    private JComboBox<String> algoSelector;
    private JTextField txtPid, txtAt, txtBt, txtQuantum;
    private JLabel lblQuantum;
    private DefaultTableModel tableModel;
    private JTable processTable;
    private GanttPanel ganttPanel;
    private JButton btnColorPick;

    // --- State ---
    private ArrayList<Process> processList = new ArrayList<>();
    private ArrayList<ExecutionStep> simulationTimeline = new ArrayList<>();
    private Color nextColor = new Color(70, 130, 180); // Default Steel Blue
    private int processCounter = 1;

    // --- Animation State ---
    private Timer animationTimer;
    private int currentSimTime = 0;
    private int totalSimTime = 0;

    // --- Colors & Fonts ---
    private final Color BG_COLOR = new Color(30, 30, 30);
    private final Color PANEL_COLOR = new Color(45, 45, 45);
    private final Color TEXT_COLOR = new Color(230, 230, 230);
    private final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 16);
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);

    public TaskScheduler() {
        setTitle("CPU Scheduler Simulator - Real Time");
        setSize(1100, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        add(createTopPanel(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1));
        centerPanel.setBackground(BG_COLOR);

        createTable();
        JScrollPane tableScroll = new JScrollPane(processTable);
        tableScroll.getViewport().setBackground(PANEL_COLOR);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(tableScroll);

        ganttPanel = new GanttPanel();
        centerPanel.add(ganttPanel);

        add(centerPanel, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(TEXT_COLOR), "Input", 0, 0, HEADER_FONT, TEXT_COLOR));

        txtPid = styleTextField(new JTextField("P1", 6));
        txtAt = styleTextField(new JTextField(6));
        txtBt = styleTextField(new JTextField(6));

        // Color Picker Button
        btnColorPick = new JButton(" ");
        btnColorPick.setBackground(nextColor);
        btnColorPick.setPreferredSize(new Dimension(30, 30));
        btnColorPick.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        btnColorPick.setToolTipText("Choose Process Color");
        btnColorPick.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Choose Process Color", nextColor);
            if(c != null) {
                nextColor = c;
                btnColorPick.setBackground(c);
            }
        });

        JButton btnAdd = styleButton("Add Process");
        btnAdd.addActionListener(e -> addProcess());

        JButton btnRandom = styleButton("Randomize Inputs");
        btnRandom.setBackground(new Color(180, 100, 50));
        btnRandom.addActionListener(e -> randomizeProcesses());

        panel.add(styleLabel("ID:"));
        panel.add(txtPid);
        panel.add(styleLabel("Arrival:"));
        panel.add(txtAt);
        panel.add(styleLabel("Burst:"));
        panel.add(txtBt);
        panel.add(styleLabel("Color:"));
        panel.add(btnColorPick);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(btnAdd);
        panel.add(btnRandom);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 20));
        panel.setBackground(BG_COLOR);

        String[] algos = {"First Come First Serve (FCFS)", "Round Robin (RR)"};
        algoSelector = new JComboBox<>(algos);
        algoSelector.setBackground(PANEL_COLOR);
        algoSelector.setForeground(TEXT_COLOR);
        algoSelector.setFont(MAIN_FONT);

        lblQuantum = styleLabel("Time Quantum:");
        txtQuantum = styleTextField(new JTextField("2", 5));
        lblQuantum.setVisible(false);
        txtQuantum.setVisible(false);

        algoSelector.addActionListener(e -> {
            boolean isRR = algoSelector.getSelectedIndex() == 1;
            lblQuantum.setVisible(isRR);
            txtQuantum.setVisible(isRR);
            panel.revalidate();
            panel.repaint();
        });

        JButton btnRun = styleButton("Simulate (Animate)");
        btnRun.setBackground(new Color(0, 150, 0));
        btnRun.addActionListener(e -> startSimulation());

        JButton btnReset = styleButton("Reset");
        btnReset.setBackground(new Color(150, 50, 50));
        btnReset.addActionListener(e -> reset());

        panel.add(styleLabel("Algorithm:"));
        panel.add(algoSelector);
        panel.add(lblQuantum);
        panel.add(txtQuantum);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(btnRun);
        panel.add(btnReset);

        return panel;
    }

    private void createTable() {
        // Added "Progress" column
        String[] cols = {"PID", "Color", "Arrival", "Burst", "Progress", "Finish", "Turnaround", "Waiting"};

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        processTable = new JTable(tableModel);
        processTable.setBackground(PANEL_COLOR);
        processTable.setForeground(TEXT_COLOR);
        processTable.setGridColor(Color.GRAY);
        processTable.setRowHeight(30);
        processTable.setFont(MAIN_FONT);

        processTable.getTableHeader().setBackground(new Color(60, 60, 60));
        processTable.getTableHeader().setForeground(TEXT_COLOR);
        processTable.getTableHeader().setFont(HEADER_FONT);

        // Custom Renderers
        processTable.getColumnModel().getColumn(1).setCellRenderer(new ColorRenderer());
        processTable.getColumnModel().getColumn(4).setCellRenderer(new ProgressRenderer());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setBackground(PANEL_COLOR);
        centerRenderer.setForeground(TEXT_COLOR);

        for(int i : new int[]{0, 2, 3, 5, 6, 7}) {
            processTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Hide Color column text, just show box
        processTable.getColumnModel().getColumn(1).setMaxWidth(50);
    }

    // --- Logic & Simulation ---

    private void addProcess() {
        try {
            String pid = txtPid.getText();
            int at = Integer.parseInt(txtAt.getText());
            int bt = Integer.parseInt(txtBt.getText());

            processList.add(new Process(pid, at, bt, nextColor));
            updateTableData();

            processCounter++;
            txtPid.setText("P" + processCounter);
            txtAt.setText("");
            txtBt.setText("");
            txtAt.requestFocus();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid numbers.");
        }
    }

    private void randomizeProcesses() {
        if(processList.isEmpty()) return;
        Random rand = new Random();
        for(Process p : processList) {
            p.arrivalTime = rand.nextInt(10); // 0 to 9
            p.burstTime = rand.nextInt(15) + 1; // 1 to 15
        }
        updateTableData();
        JOptionPane.showMessageDialog(this, "Processes randomized! Click Simulate to run.");
    }

    private void updateTableData() {
        tableModel.setRowCount(0);
        for (Process p : processList) {
            tableModel.addRow(new Object[]{
                    p.pid,
                    p.color,
                    p.arrivalTime,
                    p.burstTime,
                    0, // Initial progress
                    "-", "-", "-"
            });
        }
    }

    private void reset() {
        if(animationTimer != null && animationTimer.isRunning()) animationTimer.stop();
        processList.clear();
        simulationTimeline.clear();
        tableModel.setRowCount(0);
        currentSimTime = 0;
        processCounter = 1;
        txtPid.setText("P1");
        ganttPanel.repaint();
    }

    // --- PRE-CALCULATION & ANIMATION ---

    private void startSimulation() {
        if (processList.isEmpty()) return;

        // 1. Reset Internal State
        for(Process p : processList) p.reset();
        simulationTimeline.clear();
        currentSimTime = 0;

        // 2. Pre-calculate Logic (Generate the timeline)
        // Sort by Arrival Time
        ArrayList<Process> sortedList = new ArrayList<>(processList);
        sortedList.sort(Comparator.comparingInt(p -> p.arrivalTime));

        if (algoSelector.getSelectedIndex() == 0) {
            calculateFCFS(sortedList);
        } else {
            try {
                int q = Integer.parseInt(txtQuantum.getText());
                calculateRR(sortedList, q);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid Quantum");
                return;
            }
        }

        // 3. Start Animation Timer
        totalSimTime = simulationTimeline.size();

        if(animationTimer != null && animationTimer.isRunning()) animationTimer.stop();

        animationTimer = new Timer(500, e -> { // 500ms delay per second of simulation
            if (currentSimTime < totalSimTime) {
                stepAnimation();
            } else {
                ((Timer)e.getSource()).stop();
                finalizeTable(); // Fill in wait/turnaround times
            }
        });
        animationTimer.start();
    }

    private void stepAnimation() {
        ExecutionStep step = simulationTimeline.get(currentSimTime);
        currentSimTime++;

        // Update Progress Bars
        if (!step.pid.equals("IDLE")) {
            for (int i = 0; i < processList.size(); i++) {
                Process p = processList.get(i);
                if (p.pid.equals(step.pid)) {
                    p.executedTime++;
                    // Update Table Progress directly
                    tableModel.setValueAt(p.executedTime, i, 4); // Column 4 is Progress
                    break;
                }
            }
        }

        // Repaint Gantt
        ganttPanel.repaint();
    }

    private void finalizeTable() {
        // Calculate final stats based on completion times found in logic
        for (int i=0; i<processList.size(); i++) {
            Process p = processList.get(i);
            tableModel.setValueAt(p.completionTime, i, 5);
            tableModel.setValueAt(p.turnAroundTime, i, 6);
            tableModel.setValueAt(p.waitingTime, i, 7);
        }
    }

    // --- Logic Generators (Populate simulationTimeline) ---

    private void calculateFCFS(ArrayList<Process> sortedList) {
        int time = 0;
        for (Process p : sortedList) {
            // Idle handling
            while (time < p.arrivalTime) {
                simulationTimeline.add(new ExecutionStep("IDLE", time, Color.GRAY));
                time++;
            }
            // Execution
            for (int i = 0; i < p.burstTime; i++) {
                simulationTimeline.add(new ExecutionStep(p.pid, time, p.color));
                time++;
            }
            p.completionTime = time;
            p.turnAroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnAroundTime - p.burstTime;
        }
    }

    private void calculateRR(ArrayList<Process> sortedList, int quantum) {
        int time = 0;
        int completed = 0;
        int n = sortedList.size();
        Queue<Process> queue = new LinkedList<>();
        Set<Process> inQueue = new HashSet<>();

        // Add processes arriving at 0
        int i = 0;
        while(i < n && sortedList.get(i).arrivalTime <= time) {
            queue.add(sortedList.get(i));
            inQueue.add(sortedList.get(i));
            i++;
        }

        // To detect idle time if queue is empty but not finished
        while(completed < n) {
            if(queue.isEmpty()) {
                simulationTimeline.add(new ExecutionStep("IDLE", time, Color.GRAY));
                time++;
                // Check if new process arrived during idle
                while(i < n && sortedList.get(i).arrivalTime <= time) {
                    queue.add(sortedList.get(i));
                    inQueue.add(sortedList.get(i));
                    i++;
                }
                continue;
            }

            Process p = queue.poll();
            inQueue.remove(p);

            int exec = Math.min(p.remainingTime, quantum);
            for(int k=0; k<exec; k++) {
                simulationTimeline.add(new ExecutionStep(p.pid, time, p.color));
                time++;
                p.remainingTime--;

                // Add arrivals during this second
                while(i < n && sortedList.get(i).arrivalTime <= time) {
                    if(!inQueue.contains(sortedList.get(i)) && sortedList.get(i).remainingTime > 0){
                        queue.add(sortedList.get(i));
                        inQueue.add(sortedList.get(i));
                    }
                    i++;
                }
            }

            if(p.remainingTime > 0) {
                queue.add(p);
                inQueue.add(p);
            } else {
                completed++;
                p.completionTime = time;
                p.turnAroundTime = p.completionTime - p.arrivalTime;
                p.waitingTime = p.turnAroundTime - p.burstTime;
            }
        }
    }

    // --- Custom Renderers ---

    class ColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            c.setBackground((Color) value);
            return c;
        }
    }

    class ProgressRenderer extends JProgressBar implements TableCellRenderer {
        public ProgressRenderer() {
            super(0, 100);
            setStringPainted(true);
            setBackground(PANEL_COLOR);
            setForeground(new Color(0, 180, 0));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int executed = (int) value;
            Process p = processList.get(row);
            int total = p.burstTime;
            setValue(executed);
            setMaximum(total);
            setString(executed + " / " + total);
            return this;
        }
    }

    // --- Styling Helpers ---
    private JLabel styleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_COLOR);
        l.setFont(MAIN_FONT);
        return l;
    }

    private JTextField styleTextField(JTextField tf) {
        tf.setBackground(PANEL_COLOR);
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        tf.setFont(MAIN_FONT);
        return tf;
    }

    private JButton styleButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(70, 130, 180));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(MAIN_FONT);
        return b;
    }

    // --- Gantt Chart Panel ---
    private class GanttPanel extends JPanel {
        public GanttPanel() {
            setBackground(BG_COLOR);
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(TEXT_COLOR), "Gantt Chart (Real Time)", 0, 0, HEADER_FONT, TEXT_COLOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (simulationTimeline.isEmpty() || currentSimTime == 0) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(MAIN_FONT);

            int startX = 20;
            int y = 60;
            int h = 50;

            // Dynamic scale: Width adjusts based on total time, not current time
            int panelWidth = getWidth() - 50;
            double scale = (double) panelWidth / (simulationTimeline.size() + 2);

            // Draw valid steps up to currentSimTime
            for (int i = 0; i < currentSimTime; i++) {
                ExecutionStep step = simulationTimeline.get(i);

                int x = startX + (int) (i * scale);
                int width = (int) scale + 1; // +1 to close gaps

                g2.setColor(step.color);
                g2.fillRect(x, y, width, h);
                g2.setColor(BG_COLOR); // Grid lines
                g2.drawRect(x, y, width, h);

                // Draw PID occasionally (to avoid clutter) or at center of blocks
                // Simple logic: Draw PID if previous step was different
                if (!step.pid.equals("IDLE")) {
                    boolean isStartOfBlock = (i == 0) || !simulationTimeline.get(i-1).pid.equals(step.pid);
                    if (isStartOfBlock) {
                        g2.setColor(Color.WHITE);
                        g2.drawString(step.pid, x + 2, y - 5);
                    }
                }
            }

            // Draw Timeline Ruler
            g2.setColor(Color.GRAY);
            g2.drawLine(startX, y+h+5, startX + (int)(simulationTimeline.size()*scale), y+h+5);
            g2.drawString("0", startX, y+h+20);
            String currentStr = String.valueOf(currentSimTime);
            int curX = startX + (int)(currentSimTime * scale);
            g2.drawString(currentStr, curX, y+h+20);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TaskScheduler().setVisible(true));
    }
}