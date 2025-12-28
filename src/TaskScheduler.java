import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class TaskScheduler extends JFrame {

    // --- Data Structures ---
    static class Process {
        String pid;
        int arrivalTime;
        int burstTime;
        int remainingTime; // For RR
        int completionTime;
        int turnAroundTime;
        int waitingTime;

        public Process(String pid, int at, int bt) {
            this.pid = pid;
            this.arrivalTime = at;
            this.burstTime = bt;
            this.remainingTime = bt;
        }
    }

    static class GanttBlock {
        String pid;
        int startTime;
        int endTime;

        public GanttBlock(String pid, int start, int end) {
            this.pid = pid;
            this.startTime = start;
            this.endTime = end;
        }
    }

    // --- UI Components ---
    private JComboBox<String> algoSelector;
    private JTextField txtPid, txtAt, txtBt, txtQuantum;
    private JLabel lblQuantum;
    private DefaultTableModel tableModel;
    private JTable processTable;
    private GanttPanel ganttPanel;

    // --- State ---
    private ArrayList<Process> processList = new ArrayList<>();
    private ArrayList<GanttBlock> ganttLog = new ArrayList<>();

    // --- Colors (Dark Theme) ---
    private final Color BG_COLOR = new Color(30, 30, 30);
    private final Color PANEL_COLOR = new Color(45, 45, 45);
    private final Color TEXT_COLOR = new Color(230, 230, 230);
    private final Color ACCENT_COLOR = new Color(70, 130, 180); // Steel Blue

    public TaskScheduler() {
        setTitle("CPU Scheduler Simulator");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        // 1. Top Panel (Inputs)
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 2. Center Panel (Table + Gantt)
        JPanel centerPanel = new JPanel(new GridLayout(2, 1));
        centerPanel.setBackground(BG_COLOR);

        // Table
        createTable();
        JScrollPane tableScroll = new JScrollPane(processTable);
        tableScroll.getViewport().setBackground(PANEL_COLOR);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(tableScroll);

        // Gantt Chart Area
        ganttPanel = new GanttPanel();
        centerPanel.add(ganttPanel);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Panel (Controls)
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(TEXT_COLOR), "Input", 0, 0, null, TEXT_COLOR));

        txtPid = styleTextField(new JTextField(5));
        txtAt = styleTextField(new JTextField(5));
        txtBt = styleTextField(new JTextField(5));

        JButton btnAdd = styleButton("Add Process");
        btnAdd.addActionListener(e -> addProcess());

        panel.add(styleLabel("Process ID:"));
        panel.add(txtPid);
        panel.add(styleLabel("Arrival Time:"));
        panel.add(txtAt);
        panel.add(styleLabel("Burst Time:"));
        panel.add(txtBt);
        panel.add(btnAdd);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        panel.setBackground(BG_COLOR);

        // Algorithm Selector
        String[] algos = {"First Come First Serve (FCFS)", "Round Robin (RR)"};
        algoSelector = new JComboBox<>(algos);
        algoSelector.setBackground(PANEL_COLOR);
        algoSelector.setForeground(TEXT_COLOR);

        // Dynamic Quantum Inputs
        lblQuantum = styleLabel("Time Quantum:");
        txtQuantum = styleTextField(new JTextField("2", 5));

        // Initially hide Quantum (FCFS is default)
        lblQuantum.setVisible(false);
        txtQuantum.setVisible(false);

        // Listener for Visibility Toggle
        algoSelector.addActionListener(e -> {
            boolean isRR = algoSelector.getSelectedIndex() == 1;
            lblQuantum.setVisible(isRR);
            txtQuantum.setVisible(isRR);
            panel.revalidate();
            panel.repaint();
        });

        JButton btnRun = styleButton("Simulate");
        btnRun.setBackground(new Color(0, 150, 0)); // Greenish
        btnRun.addActionListener(e -> runSimulation());

        JButton btnReset = styleButton("Reset");
        btnReset.setBackground(new Color(150, 50, 50)); // Reddish
        btnReset.addActionListener(e -> reset());

        panel.add(styleLabel("Algorithm:"));
        panel.add(algoSelector);
        panel.add(lblQuantum);
        panel.add(txtQuantum);
        panel.add(Box.createHorizontalStrut(20)); // Spacer
        panel.add(btnRun);
        panel.add(btnReset);

        return panel;
    }

    private void createTable() {
        String[] cols = {"PID", "Arrival", "Burst", "Finish", "Turnaround", "Waiting"};
        tableModel = new DefaultTableModel(cols, 0);
        processTable = new JTable(tableModel);

        processTable.setBackground(PANEL_COLOR);
        processTable.setForeground(TEXT_COLOR);
        processTable.setGridColor(Color.GRAY);
        processTable.setRowHeight(25);

        // Header styling
        processTable.getTableHeader().setBackground(new Color(60, 60, 60));
        processTable.getTableHeader().setForeground(TEXT_COLOR);

        // Center alignment
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setBackground(PANEL_COLOR); // Ensure cells keep dark bg
        centerRenderer.setForeground(TEXT_COLOR);
        for(int i=0; i<processTable.getColumnCount(); i++){
            processTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    // --- Logic Methods ---

    private void addProcess() {
        try {
            String pid = txtPid.getText();
            int at = Integer.parseInt(txtAt.getText());
            int bt = Integer.parseInt(txtBt.getText());

            if(pid.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter PID"); return; }

            processList.add(new Process(pid, at, bt));
            tableModel.addRow(new Object[]{pid, at, bt, "-", "-", "-"});

            // Clear inputs
            txtPid.setText("");
            txtAt.setText("");
            txtBt.setText("");
            txtPid.requestFocus();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers.");
        }
    }

    private void reset() {
        processList.clear();
        ganttLog.clear();
        tableModel.setRowCount(0);
        ganttPanel.repaint();
    }

    private void runSimulation() {
        if (processList.isEmpty()) return;

        ganttLog.clear();
        // Reset process state for re-runs
        for(Process p : processList) {
            p.remainingTime = p.burstTime;
        }

        // Sort by Arrival Time initially for consistency
        processList.sort(Comparator.comparingInt(p -> p.arrivalTime));

        if (algoSelector.getSelectedIndex() == 0) {
            solveFCFS();
        } else {
            try {
                int q = Integer.parseInt(txtQuantum.getText());
                solveRR(q);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid Time Quantum");
                return;
            }
        }

        updateTableResult();
        ganttPanel.repaint();
    }

    private void solveFCFS() {
        int currentTime = 0;

        for (Process p : processList) {
            if (currentTime < p.arrivalTime) {
                currentTime = p.arrivalTime; // Idle time
            }

            int start = currentTime;
            currentTime += p.burstTime;
            int end = currentTime;

            p.completionTime = end;
            p.turnAroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnAroundTime - p.burstTime;

            ganttLog.add(new GanttBlock(p.pid, start, end));
        }
    }

    private void solveRR(int quantum) {
        int currentTime = 0;
        int completed = 0;
        int n = processList.size();

        // Queue for Ready processes
        Queue<Process> queue = new LinkedList<>();

        // Helper list to track who is in queue to avoid duplicates
        Set<Process> inQueue = new HashSet<>();

        // Add first process(es)
        // Find min arrival
        int minAt = processList.get(0).arrivalTime;
        currentTime = minAt;

        // Add all processes that arrive at start
        int i = 0;
        while(i < n && processList.get(i).arrivalTime <= currentTime) {
            queue.add(processList.get(i));
            inQueue.add(processList.get(i));
            i++;
        }

        while (completed < n) {
            if (queue.isEmpty()) {
                // If queue is empty but not all done, jump to next arrival
                // Find next arriving process
                for(int k=0; k<n; k++){
                    if(processList.get(k).remainingTime > 0 && !inQueue.contains(processList.get(k))){
                        currentTime = processList.get(k).arrivalTime;
                        queue.add(processList.get(k));
                        inQueue.add(processList.get(k));
                        i = k + 1; // Sync index
                        break;
                    }
                }
                if(queue.isEmpty()) break; // Safety break
            }

            Process p = queue.poll();
            inQueue.remove(p);

            int start = currentTime;
            int execTime = Math.min(p.remainingTime, quantum);
            p.remainingTime -= execTime;
            currentTime += execTime;
            int end = currentTime;

            ganttLog.add(new GanttBlock(p.pid, start, end));

            // Check for new arrivals during this execution
            while(i < n && processList.get(i).arrivalTime <= currentTime) {
                if(processList.get(i).remainingTime > 0 && !inQueue.contains(processList.get(i))){
                    queue.add(processList.get(i));
                    inQueue.add(processList.get(i));
                }
                i++;
            }

            if (p.remainingTime > 0) {
                queue.add(p);
                inQueue.add(p);
            } else {
                completed++;
                p.completionTime = currentTime;
                p.turnAroundTime = p.completionTime - p.arrivalTime;
                p.waitingTime = p.turnAroundTime - p.burstTime;
            }
        }
    }

    private void updateTableResult() {
        tableModel.setRowCount(0);
        for (Process p : processList) {
            tableModel.addRow(new Object[]{
                    p.pid, p.arrivalTime, p.burstTime,
                    p.completionTime, p.turnAroundTime, p.waitingTime
            });
        }
    }

    // --- Styling Helpers ---
    private JLabel styleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_COLOR);
        return l;
    }

    private JTextField styleTextField(JTextField tf) {
        tf.setBackground(PANEL_COLOR);
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return tf;
    }

    private JButton styleButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT_COLOR);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        return b;
    }

    // --- Gantt Chart Panel ---
    private class GanttPanel extends JPanel {
        public GanttPanel() {
            setBackground(BG_COLOR);
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(TEXT_COLOR), "Gantt Chart", 0, 0, null, TEXT_COLOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (ganttLog.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            int startX = 20;
            int y = 50;
            int h = 40;

            // Calculate scale based on total time
            int totalTime = ganttLog.get(ganttLog.size()-1).endTime;
            int panelWidth = getWidth() - 50;
            double scale = (double) panelWidth / (totalTime + 2); // +2 padding

            for (GanttBlock b : ganttLog) {
                int width = (int) ((b.endTime - b.startTime) * scale);
                int x = startX + (int) (b.startTime * scale);

                // Draw Block
                g2.setColor(ACCENT_COLOR);
                g2.fillRect(x, y, width, h);
                g2.setColor(Color.WHITE);
                g2.drawRect(x, y, width, h);

                // Draw PID
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int textX = x + (width - fm.stringWidth(b.pid)) / 2;
                int textY = y + (h + fm.getAscent()) / 2 - 3;
                g2.drawString(b.pid, textX, textY);

                // Draw Time Markers
                g2.setColor(Color.GRAY);
                g2.drawString(String.valueOf(b.startTime), x, y + h + 15);
                // Draw end time only for the last block to avoid clutter,
                // or if it's the end of a block
                g2.drawString(String.valueOf(b.endTime), x + width, y + h + 15);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TaskScheduler().setVisible(true));
    }
}