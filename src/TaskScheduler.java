import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * TaskScheduler.java
 * * A Java Swing GUI application that simulates CPU Scheduling Algorithms:
 * 1. First Come First Serve (FCFS)
 * 2. Round Robin (RR)
 * * Features:
 * - Input for Process ID, Arrival Time, Burst Time.
 * - Dynamic JTable for process data.
 * - Calculation of Turnaround Time, Waiting Time, and Throughput.
 * - Visual Gantt Chart.
 * - Custom Color Palette: #0F2854, #1C4D8D, #4988C4, #BDE8F5.
 */
public class TaskScheduler extends JFrame {

    // --- Color Palette ---
    private static final Color COL_DARK_NAVY = Color.decode("#0F2854"); // Backgrounds / Text
    private static final Color COL_MEDIUM_BLUE = Color.decode("#1C4D8D"); // Buttons / Headers
    private static final Color COL_LIGHT_BLUE = Color.decode("#4988C4"); // Accents
    private static final Color COL_PALE_BLUE = Color.decode("#BDE8F5"); // Fields / Table BG

    // --- Components ---
    private JTextField tfProcessId, tfArrivalTime, tfBurstTime;
    private JComboBox<String> cbAlgorithm;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblAvgTurnaround, lblAvgWaiting, lblThroughput;
    private GanttPanel ganttPanel;

    // --- Data ---
    private ArrayList<Process> processList = new ArrayList<>();

    public static void main(String[] args) {
        // Ensure GUI is created on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel for better integration
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TaskScheduler().setVisible(true);
        });
    }

    public TaskScheduler() {
        setTitle("CPU Task Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);

        // -- Initialize UI Parts --
        JPanel topPanel = createTopPanel();
        JPanel centerPanel = createCenterPanel();
        JPanel bottomPanel = createGanttSection();

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates the top section containing Inputs and Algorithm Selection.
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBorder(new EmptyBorder(15, 15, 5, 15));
        panel.setBackground(Color.WHITE);

        // 1. Left Side: Inputs
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COL_LIGHT_BLUE), "Add Process", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), COL_DARK_NAVY));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Headers
        gbc.gridy = 0; gbc.gridx = 0; inputPanel.add(new JLabel("Process ID"), gbc);
        gbc.gridx = 1; inputPanel.add(new JLabel("Arrival Time"), gbc);
        gbc.gridx = 2; inputPanel.add(new JLabel("Burst Time"), gbc);

        // Fields
        gbc.gridy = 1;
        gbc.gridx = 0; tfProcessId = createStyledTextField(); inputPanel.add(tfProcessId, gbc);
        gbc.gridx = 1; tfArrivalTime = createStyledTextField(); inputPanel.add(tfArrivalTime, gbc);
        gbc.gridx = 2; tfBurstTime = createStyledTextField(); inputPanel.add(tfBurstTime, gbc);

        // Add Button
        JButton btnAdd = createStyledButton("Add Process");
        btnAdd.addActionListener(e -> addProcess());
        gbc.gridy = 1; gbc.gridx = 3;
        inputPanel.add(btnAdd, gbc);

        // 2. Right Side: Algorithm Select
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(BorderFactory.createLineBorder(COL_LIGHT_BLUE));

        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.insets = new Insets(10, 10, 10, 10);
        cGbc.fill = GridBagConstraints.HORIZONTAL;
        cGbc.weightx = 1.0;

        JLabel lblSelect = new JLabel("Select Scheduling Method");
        lblSelect.setForeground(COL_DARK_NAVY);
        cGbc.gridx = 0; cGbc.gridy = 0;
        controlPanel.add(lblSelect, cGbc);

        cbAlgorithm = new JComboBox<>(new String[]{"First Come First Served", "Round Robin"});
        cbAlgorithm.setBackground(Color.WHITE);
        cGbc.gridy = 1;
        controlPanel.add(cbAlgorithm, cGbc);

        JButton btnCalculate = createStyledButton("Calculate");
        btnCalculate.addActionListener(e -> calculateSchedule());
        cGbc.gridy = 2;
        cGbc.fill = GridBagConstraints.NONE;
        cGbc.anchor = GridBagConstraints.WEST;
        controlPanel.add(btnCalculate, cGbc);

        panel.add(inputPanel);
        panel.add(controlPanel);

        return panel;
    }

    /**
     * Creates the center section containing the Table and Stats.
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(5, 15, 5, 15));
        panel.setBackground(Color.WHITE);

        // 1. Table
        String[] columns = {"Process ID", "Arrival Time", "Burst Time", "Completed Time", "Waiting Time", "Turnaround Time"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);

        // Table Styling
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        table.setGridColor(COL_LIGHT_BLUE);
        table.getTableHeader().setBackground(COL_DARK_NAVY); // Header BG
        table.getTableHeader().setForeground(Color.WHITE);   // Header Text
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setSelectionBackground(COL_BDE8F5());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(COL_LIGHT_BLUE));

        // 2. Stats Panel (Right side)
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createLineBorder(COL_LIGHT_BLUE));
        statsPanel.setPreferredSize(new Dimension(250, 0));

        statsPanel.add(Box.createVerticalStrut(10));
        statsPanel.add(createStatBlock("Average Turnaround Time"));
        lblAvgTurnaround = createStatValueLabel();
        statsPanel.add(wrapInPanel(lblAvgTurnaround));

        statsPanel.add(Box.createVerticalStrut(10));
        statsPanel.add(createStatBlock("Average Waiting Time"));
        lblAvgWaiting = createStatValueLabel();
        statsPanel.add(wrapInPanel(lblAvgWaiting));

        statsPanel.add(Box.createVerticalStrut(10));
        statsPanel.add(createStatBlock("Throughput"));
        lblThroughput = createStatValueLabel();
        statsPanel.add(wrapInPanel(lblThroughput));

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statsPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createGanttSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Gantt Chart Visualization");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(COL_DARK_NAVY);
        title.setBorder(new EmptyBorder(0, 0, 5, 0));

        ganttPanel = new GanttPanel();
        ganttPanel.setPreferredSize(new Dimension(0, 100));
        ganttPanel.setBorder(BorderFactory.createLineBorder(COL_DARK_NAVY, 1));

        panel.add(title, BorderLayout.NORTH);
        panel.add(ganttPanel, BorderLayout.CENTER);
        return panel;
    }

    // --- Helpers for Styling ---

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField(8);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return tf;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(COL_MEDIUM_BLUE); // 1C4D8D
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        return btn;
    }

    // Helper for pale blue color since it's used in method
    private Color COL_BDE8F5() { return COL_PALE_BLUE; }

    private JLabel createStatBlock(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setForeground(COL_DARK_NAVY);
        lbl.setBorder(new EmptyBorder(0, 10, 5, 0));
        return lbl;
    }

    private JLabel createStatValueLabel() {
        JLabel lbl = new JLabel("0");
        lbl.setOpaque(true);
        lbl.setBackground(COL_PALE_BLUE); // BDE8F5
        lbl.setBorder(new EmptyBorder(10, 10, 10, 10));
        lbl.setPreferredSize(new Dimension(200, 35));
        return lbl;
    }

    private JPanel wrapInPanel(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(Color.WHITE);
        p.add(c);
        return p;
    }

    // --- Logic Implementation ---

    private void addProcess() {
        try {
            String pid = tfProcessId.getText();
            int arrival = Integer.parseInt(tfArrivalTime.getText());
            int burst = Integer.parseInt(tfBurstTime.getText());

            if(pid.isEmpty()) throw new Exception("Empty ID");

            Process p = new Process(pid, arrival, burst);
            processList.add(p);

            // Add raw data to table
            tableModel.addRow(new Object[]{pid, arrival, burst, "-", "-", "-"});

            // Clear inputs
            tfProcessId.setText("");
            tfArrivalTime.setText("");
            tfBurstTime.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Input: Please check fields.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateSchedule() {
        if (processList.isEmpty()) return;

        // Reset previous calculations
        for (Process p : processList) {
            p.reset();
        }

        List<GanttBlock> ganttData = new ArrayList<>();
        String algo = (String) cbAlgorithm.getSelectedItem();

        if ("First Come First Served".equals(algo)) {
            runFCFS(ganttData);
        } else {
            // Ask for Quantum
            String qStr = JOptionPane.showInputDialog(this, "Enter Time Quantum for Round Robin:");
            if (qStr == null || qStr.isEmpty()) return;
            try {
                int quantum = Integer.parseInt(qStr);
                runRoundRobin(quantum, ganttData);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid Quantum.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        updateUIResults(ganttData);
    }

    // --- Algorithm: FCFS ---
    private void runFCFS(List<GanttBlock> ganttData) {
        // Sort by Arrival Time
        processList.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        for (Process p : processList) {
            // CPU Idle check
            if (currentTime < p.arrivalTime) {
                currentTime = p.arrivalTime;
            }

            int start = currentTime;
            currentTime += p.burstTime;
            int end = currentTime;

            p.completionTime = end;
            p.turnaroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;

            ganttData.add(new GanttBlock(p.pid, start, end));
        }
    }

    // --- Algorithm: Round Robin ---
    private void runRoundRobin(int quantum, List<GanttBlock> ganttData) {
        // Sort by Arrival Time first
        processList.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        int completed = 0;
        int n = processList.size();
        Queue<Process> queue = new LinkedList<>();

        // Track which processes have arrived
        int index = 0;

        // Add initial process(es)
        if (!processList.isEmpty()) {
            // Handle initial idle time if any
            if(processList.get(0).arrivalTime > 0) currentTime = processList.get(0).arrivalTime;

            while(index < n && processList.get(index).arrivalTime <= currentTime) {
                queue.add(processList.get(index));
                index++;
            }
        }

        while (completed < n) {
            if (queue.isEmpty()) {
                // If queue is empty but processes remain, jump time
                if (index < n) {
                    currentTime = processList.get(index).arrivalTime;
                    while(index < n && processList.get(index).arrivalTime <= currentTime) {
                        queue.add(processList.get(index));
                        index++;
                    }
                } else {
                    break;
                }
            }

            Process p = queue.poll();
            int start = currentTime;
            int timeSlice = Math.min(p.remainingTime, quantum);

            p.remainingTime -= timeSlice;
            currentTime += timeSlice;
            int end = currentTime;

            // Add to Gantt
            ganttData.add(new GanttBlock(p.pid, start, end));

            // Check for new arrivals during this time slice
            while(index < n && processList.get(index).arrivalTime <= currentTime) {
                queue.add(processList.get(index));
                index++;
            }

            if (p.remainingTime > 0) {
                queue.add(p);
            } else {
                // Process Finished
                completed++;
                p.completionTime = currentTime;
                p.turnaroundTime = p.completionTime - p.arrivalTime;
                p.waitingTime = p.turnaroundTime - p.burstTime;
            }
        }
    }

    private void updateUIResults(List<GanttBlock> ganttData) {
        // Update Table
        tableModel.setRowCount(0);
        double totalTurnaround = 0;
        double totalWaiting = 0;
        int maxCompletion = 0;

        for (Process p : processList) {
            tableModel.addRow(new Object[]{
                    p.pid, p.arrivalTime, p.burstTime, p.completionTime, p.waitingTime, p.turnaroundTime
            });
            totalTurnaround += p.turnaroundTime;
            totalWaiting += p.waitingTime;
            maxCompletion = Math.max(maxCompletion, p.completionTime);
        }

        // Update Stats
        double avgTurn = totalTurnaround / processList.size();
        double avgWait = totalWaiting / processList.size();
        double throughput = (double) processList.size() / maxCompletion;

        lblAvgTurnaround.setText(String.format("%.2f", avgTurn));
        lblAvgWaiting.setText(String.format("%.2f", avgWait));
        lblThroughput.setText(String.format("%.4f", throughput));

        // Update Gantt
        ganttPanel.setData(ganttData, maxCompletion);
    }

    // --- Inner Classes ---

    // Data Structure for Process
    class Process {
        String pid;
        int arrivalTime;
        int burstTime;
        int remainingTime; // For RR
        int completionTime;
        int waitingTime;
        int turnaroundTime;

        public Process(String pid, int arrivalTime, int burstTime) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            reset();
        }

        public void reset() {
            this.remainingTime = burstTime;
            this.completionTime = 0;
            this.waitingTime = 0;
            this.turnaroundTime = 0;
        }
    }

    // Data Structure for Gantt Chart Blocks
    class GanttBlock {
        String pid;
        int start;
        int end;

        public GanttBlock(String pid, int start, int end) {
            this.pid = pid;
            this.start = start;
            this.end = end;
        }
    }

    // Custom Component for Drawing Gantt Chart
    class GanttPanel extends JPanel {
        private List<GanttBlock> blocks;
        private int totalTime;

        public void setData(List<GanttBlock> blocks, int totalTime) {
            this.blocks = blocks;
            this.totalTime = totalTime;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (blocks == null || blocks.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth() - 40;
            int height = getHeight() - 40;
            int startX = 20;
            int startY = 20;
            int barHeight = 40;

            // Scale factor
            double scale = (double) width / totalTime;

            for (GanttBlock block : blocks) {
                int blockWidth = (int) ((block.end - block.start) * scale);
                int xPos = startX + (int) (block.start * scale);

                // Draw Block
                g2.setColor(COL_LIGHT_BLUE);
                g2.fillRect(xPos, startY, blockWidth, barHeight);
                g2.setColor(COL_DARK_NAVY);
                g2.drawRect(xPos, startY, blockWidth, barHeight);

                // Draw Text
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int textX = xPos + (blockWidth - fm.stringWidth(block.pid)) / 2;
                int textY = startY + (barHeight + fm.getAscent()) / 2 - 2;
                g2.drawString(block.pid, textX, textY);

                // Draw Time Labels (Start/End)
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(block.start), xPos, startY + barHeight + 15);
                // Only draw end time if it's the very last one or there is a gap, 
                // usually simple scheduling just needs start time of next block, 
                // but let's draw end time for the last block specifically.
                if (block == blocks.get(blocks.size()-1)) {
                    g2.drawString(String.valueOf(block.end), xPos + blockWidth, startY + barHeight + 15);
                }
            }
        }
    }
}