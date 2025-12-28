import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.Timer;

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

        public void reset() {
            this.remainingTime = burstTime;
            this.executedTime = 0;
            this.completionTime = 0;
            this.turnAroundTime = 0;
            this.waitingTime = 0;
        }
    }

    static class ExecutionStep {
        String pid;
        int time;
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
    private JButton btnRun, btnStop, btnReset; // Controls

    // --- State ---
    private ArrayList<Process> processList = new ArrayList<>();
    private ArrayList<ExecutionStep> simulationTimeline = new ArrayList<>();
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
        setTitle("CPU Scheduler Simulator");
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

        // Controls
        btnRun = styleButton("Simulate");
        btnRun.setBackground(new Color(0, 150, 0));
        btnRun.addActionListener(e -> startSimulation());

        btnStop = styleButton("Stop");
        btnStop.setBackground(new Color(200, 100, 0)); // Orange-ish
        btnStop.setEnabled(false); // Disabled initially
        btnStop.addActionListener(e -> stopSimulation());

        btnReset = styleButton("Reset");
        btnReset.setBackground(new Color(150, 50, 50));
        btnReset.addActionListener(e -> reset());

        panel.add(styleLabel("Algorithm:"));
        panel.add(algoSelector);
        panel.add(lblQuantum);
        panel.add(txtQuantum);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(btnRun);
        panel.add(btnStop); // Added Stop Button
        panel.add(btnReset);

        return panel;
    }

    private void createTable() {
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

        processTable.getColumnModel().getColumn(1).setMaxWidth(50);
    }

    // --- Logic & Simulation ---

    private Color generateRandomColor() {
        // Generates bright, distinct colors (High Saturation, High Brightness)
        float hue = (float) Math.random();
        return Color.getHSBColor(hue, 0.75f, 0.9f);
    }

    private void addProcess() {
        try {
            String pid = txtPid.getText();
            int at = Integer.parseInt(txtAt.getText());
            int bt = Integer.parseInt(txtBt.getText());

            // Random Color automatically assigned
            processList.add(new Process(pid, at, bt, generateRandomColor()));
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
        if(processList.isEmpty()) {
            // If empty, create 5 random processes
            for(int i=0; i<5; i++) {
                processList.add(new Process("P"+(processCounter++), 0, 0, generateRandomColor()));
            }
        }

        Random rand = new Random();
        for(Process p : processList) {
            p.arrivalTime = rand.nextInt(10);
            p.burstTime = rand.nextInt(10) + 1;
            // Re-randomize color for fresh look
            p.color = generateRandomColor();
        }
        updateTableData();
        txtPid.setText("P" + processCounter); // Update counter display
    }

    private void updateTableData() {
        tableModel.setRowCount(0);
        for (Process p : processList) {
            tableModel.addRow(new Object[]{
                    p.pid,
                    p.color,
                    p.arrivalTime,
                    p.burstTime,
                    0,
                    "-", "-", "-"
            });
        }
    }

    private void reset() {
        stopSimulation();
        processList.clear();
        simulationTimeline.clear();
        tableModel.setRowCount(0);
        currentSimTime = 0;
        processCounter = 1;
        txtPid.setText("P1");
        ganttPanel.repaint();
    }

    // --- ANIMATION CONTROLS ---

    private void startSimulation() {
        if (processList.isEmpty()) return;

        // 1. Reset Internal State
        for(Process p : processList) p.reset();
        simulationTimeline.clear();
        currentSimTime = 0;
        updateTableData(); // clear old results from table

        // 2. Pre-calculate Logic
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

        // 3. Start Animation
        totalSimTime = simulationTimeline.size();

        if(animationTimer != null && animationTimer.isRunning()) animationTimer.stop();

        btnRun.setEnabled(false);
        btnStop.setEnabled(true);

        animationTimer = new Timer(500, e -> {
            if (currentSimTime < totalSimTime) {
                stepAnimation();
            } else {
                stopSimulation();
                finalizeTable();
            }
        });
        animationTimer.start();
    }

    private void stopSimulation() {
        if(animationTimer != null) {
            animationTimer.stop();
        }
        btnRun.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private void stepAnimation() {
        ExecutionStep step = simulationTimeline.get(currentSimTime);
        currentSimTime++;

        if (!step.pid.equals("IDLE")) {
            for (int i = 0; i < processList.size(); i++) {
                Process p = processList.get(i);
                if (p.pid.equals(step.pid)) {
                    p.executedTime++;
                    tableModel.setValueAt(p.executedTime, i, 4);
                    break;
                }
            }
        }
        ganttPanel.repaint();
    }

    private void finalizeTable() {
        for (int i=0; i<processList.size(); i++) {
            Process p = processList.get(i);
            tableModel.setValueAt(p.completionTime, i, 5);
            tableModel.setValueAt(p.turnAroundTime, i, 6);
            tableModel.setValueAt(p.waitingTime, i, 7);
        }
    }

    // --- Logic Generators ---

    private void calculateFCFS(ArrayList<Process> sortedList) {
        int time = 0;
        for (Process p : sortedList) {
            while (time < p.arrivalTime) {
                simulationTimeline.add(new ExecutionStep("IDLE", time, Color.GRAY));
                time++;
            }
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

        int i = 0;
        while(i < n && sortedList.get(i).arrivalTime <= time) {
            queue.add(sortedList.get(i));
            inQueue.add(sortedList.get(i));
            i++;
        }

        while(completed < n) {
            if(queue.isEmpty()) {
                simulationTimeline.add(new ExecutionStep("IDLE", time, Color.GRAY));
                time++;
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

            int panelWidth = getWidth() - 50;
            double scale = (double) panelWidth / (simulationTimeline.size() + 2);

            for (int i = 0; i < currentSimTime; i++) {
                ExecutionStep step = simulationTimeline.get(i);

                int x = startX + (int) (i * scale);
                int width = (int) scale + 1;

                g2.setColor(step.color);
                g2.fillRect(x, y, width, h);
                g2.setColor(BG_COLOR);
                g2.drawRect(x, y, width, h);

                if (!step.pid.equals("IDLE")) {
                    boolean isStartOfBlock = (i == 0) || !simulationTimeline.get(i-1).pid.equals(step.pid);
                    if (isStartOfBlock) {
                        g2.setColor(Color.WHITE);
                        g2.drawString(step.pid, x + 2, y - 5);
                    }
                }
            }

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