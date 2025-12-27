import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.List;

/**
 * TaskScheduler.java
 *
 * Simulates CPU Scheduling (FCFS & Round Robin) using Java Swing.
 *
 * Updates:
 * - Dark Theme using specific palette: #0F2854, #1C4D8D, #4988C4, #BDE8F5
 * - Fixed window size (not resizable)
 * - Increased Font Sizes
 * - Integrated Quantum Input Field
 */
public class TaskScheduler extends JFrame {

    // --- Color Palette ---
    private static final Color COL_BG_DARK    = Color.decode("#0F2854"); // Main Background
    private static final Color COL_PANEL_BG   = Color.decode("#1C4D8D"); // Secondary Panels
    private static final Color COL_ACCENT     = Color.decode("#4988C4"); // Borders / Buttons
    private static final Color COL_TEXT_FIELD = Color.decode("#BDE8F5"); // Input Fields / Text Color
    private static final Color COL_TEXT_WHITE = Color.WHITE;

    // --- Fonts ---
    private static final Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);

    // --- Components ---
    private JTextField tfProcessId, tfArrivalTime, tfBurstTime, tfQuantum;
    private JComboBox<String> cbAlgorithm;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblAvgTurnaround, lblAvgWaiting, lblThroughput;
    private GanttPanel ganttPanel;

    // --- Data ---
    private ArrayList<Process> processList = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set cross-platform look and feel to ensure colors render correctly
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TaskScheduler().setVisible(true);
        });
    }

    public TaskScheduler() {
        setTitle("CPU Task Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setResizable(false); // Requirement: Not resizable
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        // Main Container Background
        getContentPane().setBackground(COL_BG_DARK);

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
        panel.setBorder(new EmptyBorder(20, 20, 10, 20));
        panel.setBackground(COL_BG_DARK);

        // 1. Left Side: Inputs
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(COL_BG_DARK);

        // Titled Border Styling
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                new LineBorder(COL_ACCENT, 2), " Add Process ", TitledBorder.LEFT, TitledBorder.TOP,
                FONT_HEADER, COL_TEXT_FIELD);
        inputPanel.setBorder(titledBorder);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Headers
        gbc.gridy = 0;
        gbc.gridx = 0; inputPanel.add(createLabel("Process ID"), gbc);
        gbc.gridx = 1; inputPanel.add(createLabel("Arrival Time"), gbc);
        gbc.gridx = 2; inputPanel.add(createLabel("Burst Time"), gbc);

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
        controlPanel.setBackground(COL_PANEL_BG); // Lighter Blue for contrast
        controlPanel.setBorder(new LineBorder(COL_ACCENT, 2));

        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.insets = new Insets(10, 15, 10, 15);
        cGbc.fill = GridBagConstraints.HORIZONTAL;
        cGbc.weightx = 1.0;

        // Label
        cGbc.gridx = 0; cGbc.gridy = 0; cGbc.gridwidth = 2;
        controlPanel.add(createLabel("Select Scheduling Method"), cGbc);

        // Combo Box
        cbAlgorithm = new JComboBox<>(new String[]{"First Come First Served", "Round Robin"});
        cbAlgorithm.setFont(FONT_MAIN);
        cbAlgorithm.setBackground(COL_TEXT_FIELD);

        // Listener to enable/disable Quantum field
        cbAlgorithm.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean isRR = "Round Robin".equals(e.getItem());
                tfQuantum.setEnabled(isRR);
                tfQuantum.setBackground(isRR ? COL_TEXT_FIELD : Color.GRAY);
            }
        });
        cGbc.gridy = 1;
        controlPanel.add(cbAlgorithm, cGbc);

        // Quantum Input (Requirement: Integrated in frame)
        JPanel quantumPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        quantumPanel.setBackground(COL_PANEL_BG);

        JLabel lblQ = createLabel("Quantum: ");
        lblQ.setBorder(new EmptyBorder(0,0,0,10));
        quantumPanel.add(lblQ);

        tfQuantum = createStyledTextField();
        tfQuantum.setText("2");
        tfQuantum.setColumns(5);
        tfQuantum.setEnabled(false); // Disabled by default for FCFS
        tfQuantum.setBackground(Color.GRAY);
        quantumPanel.add(tfQuantum);

        cGbc.gridy = 2;
        controlPanel.add(quantumPanel, cGbc);

        // Calculate Button
        JButton btnCalculate = createStyledButton("Calculate");
        btnCalculate.addActionListener(e -> calculateSchedule());
        cGbc.gridy = 3;
        cGbc.fill = GridBagConstraints.NONE;
        cGbc.anchor = GridBagConstraints.CENTER;
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
        panel.setBorder(new EmptyBorder(5, 20, 5, 20));
        panel.setBackground(COL_BG_DARK);

        // 1. Table
        String[] columns = {"Process ID", "Arrival Time", "Burst Time", "Completed Time", "Waiting Time", "Turnaround Time"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);

        // Table Styling
        table.setFont(FONT_MAIN);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30); // Taller rows
        table.setBackground(COL_TEXT_FIELD); // Pale Blue background
        table.setForeground(COL_BG_DARK); // Dark text
        table.setGridColor(COL_ACCENT);

        JTableHeader header = table.getTableHeader();
        header.setBackground(COL_PANEL_BG);
        header.setForeground(COL_TEXT_WHITE);
        header.setFont(FONT_BOLD);

        // Center align table cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<table.getColumnCount(); i++){
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(COL_ACCENT, 2));
        scrollPane.getViewport().setBackground(COL_BG_DARK);

        // 2. Stats Panel (Right side)
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(COL_BG_DARK);
        statsPanel.setBorder(new LineBorder(COL_ACCENT, 1));
        statsPanel.setPreferredSize(new Dimension(280, 0));

        statsPanel.add(Box.createVerticalStrut(20));

        statsPanel.add(createStatBlock("Average Turnaround Time"));
        lblAvgTurnaround = createStatValueLabel();
        statsPanel.add(wrapInPanel(lblAvgTurnaround));

        statsPanel.add(Box.createVerticalStrut(20));

        statsPanel.add(createStatBlock("Average Waiting Time"));
        lblAvgWaiting = createStatValueLabel();
        statsPanel.add(wrapInPanel(lblAvgWaiting));

        statsPanel.add(Box.createVerticalStrut(20));

        statsPanel.add(createStatBlock("Throughput"));
        lblThroughput = createStatValueLabel();
        statsPanel.add(wrapInPanel(lblThroughput));

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statsPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createGanttSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 20, 20, 20));
        panel.setBackground(COL_BG_DARK);

        JLabel title = new JLabel("Gantt Chart Visualization");
        title.setFont(FONT_HEADER);
        title.setForeground(COL_TEXT_FIELD);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));

        ganttPanel = new GanttPanel();
        ganttPanel.setPreferredSize(new Dimension(0, 120));
        ganttPanel.setBorder(new LineBorder(COL_ACCENT, 2));
        ganttPanel.setBackground(COL_PANEL_BG); // Darker background for chart area

        panel.add(title, BorderLayout.NORTH);
        panel.add(ganttPanel, BorderLayout.CENTER);
        return panel;
    }

    // --- Helpers for Styling ---

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(COL_TEXT_WHITE);
        lbl.setFont(FONT_BOLD);
        return lbl;
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField(6);
        tf.setBackground(COL_TEXT_FIELD); // Pale Blue
        tf.setForeground(COL_BG_DARK); // Dark Text
        tf.setFont(FONT_MAIN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COL_ACCENT),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return tf;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(COL_ACCENT); // 4988C4
        btn.setForeground(COL_BG_DARK);
        btn.setFocusPainted(false);
        btn.setFont(FONT_BOLD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COL_TEXT_WHITE, 1),
                new EmptyBorder(8, 15, 8, 15)
        ));
        return btn;
    }

    private JLabel createStatBlock(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setForeground(COL_TEXT_FIELD);
        lbl.setFont(FONT_BOLD);
        lbl.setBorder(new EmptyBorder(0, 10, 5, 0));
        return lbl;
    }

    private JLabel createStatValueLabel() {
        JLabel lbl = new JLabel("0");
        lbl.setOpaque(true);
        lbl.setBackground(COL_TEXT_FIELD);
        lbl.setForeground(COL_BG_DARK);
        lbl.setFont(FONT_MAIN);
        lbl.setBorder(new EmptyBorder(10, 10, 10, 10));
        lbl.setPreferredSize(new Dimension(230, 40));
        return lbl;
    }

    private JPanel wrapInPanel(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(COL_BG_DARK);
        p.add(c);
        return p;
    }

    // --- Logic Implementation ---

    private void addProcess() {
        try {
            String pid = tfProcessId.getText();
            String arrText = tfArrivalTime.getText();
            String burstText = tfBurstTime.getText();

            if(pid.isEmpty() || arrText.isEmpty() || burstText.isEmpty()) {
                throw new Exception("Empty Fields");
            }

            int arrival = Integer.parseInt(arrText);
            int burst = Integer.parseInt(burstText);

            Process p = new Process(pid, arrival, burst);
            processList.add(p);

            // Add raw data to table
            tableModel.addRow(new Object[]{pid, arrival, burst, "-", "-", "-"});

            // Clear inputs
            tfProcessId.setText("");
            tfArrivalTime.setText("");
            tfBurstTime.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Input: Check fields.", "Error", JOptionPane.ERROR_MESSAGE);
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
            // Get Quantum from integrated text field
            try {
                int quantum = Integer.parseInt(tfQuantum.getText());
                if (quantum <= 0) throw new NumberFormatException();
                runRoundRobin(quantum, ganttData);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid Time Quantum. Must be > 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        updateUIResults(ganttData);
    }

    // --- Algorithm: FCFS ---
    private void runFCFS(List<GanttBlock> ganttData) {
        processList.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        for (Process p : processList) {
            if (currentTime < p.arrivalTime) {
                // Gap in gantt
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
        processList.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        int completed = 0;
        int n = processList.size();
        Queue<Process> queue = new LinkedList<>();
        int index = 0;

        // Initial load
        if (!processList.isEmpty()) {
            if(processList.get(0).arrivalTime > 0) currentTime = processList.get(0).arrivalTime;
            while(index < n && processList.get(index).arrivalTime <= currentTime) {
                queue.add(processList.get(index));
                index++;
            }
        }

        while (completed < n) {
            if (queue.isEmpty()) {
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

            ganttData.add(new GanttBlock(p.pid, start, end));

            while(index < n && processList.get(index).arrivalTime <= currentTime) {
                queue.add(processList.get(index));
                index++;
            }

            if (p.remainingTime > 0) {
                queue.add(p);
            } else {
                completed++;
                p.completionTime = currentTime;
                p.turnaroundTime = p.completionTime - p.arrivalTime;
                p.waitingTime = p.turnaroundTime - p.burstTime;
            }
        }
    }

    private void updateUIResults(List<GanttBlock> ganttData) {
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

        double avgTurn = totalTurnaround / processList.size();
        double avgWait = totalWaiting / processList.size();
        double throughput = (double) processList.size() / maxCompletion;

        lblAvgTurnaround.setText(String.format("%.2f", avgTurn));
        lblAvgWaiting.setText(String.format("%.2f", avgWait));
        lblThroughput.setText(String.format("%.4f", throughput));

        ganttPanel.setData(ganttData, maxCompletion);
    }

    // --- Inner Classes ---

    class Process {
        String pid;
        int arrivalTime;
        int burstTime;
        int remainingTime;
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
            // Anti-aliasing for smoother text/shapes
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 40;
            int startX = 20;
            int startY = 30;
            int barHeight = 50;

            // Simple scaling
            double scale = (double) width / totalTime;

            for (GanttBlock block : blocks) {
                int blockWidth = (int) ((block.end - block.start) * scale);
                int xPos = startX + (int) (block.start * scale);

                // Draw Bar
                g2.setColor(COL_ACCENT); // Light Blue Bars
                g2.fillRect(xPos, startY, blockWidth, barHeight);
                g2.setColor(COL_TEXT_WHITE);
                g2.drawRect(xPos, startY, blockWidth, barHeight);

                // Draw Process ID inside
                g2.setColor(COL_BG_DARK);
                g2.setFont(FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int textX = xPos + (blockWidth - fm.stringWidth(block.pid)) / 2;
                int textY = startY + (barHeight + fm.getAscent()) / 2 - 5;
                g2.drawString(block.pid, textX, textY);

                // Draw Time Markers
                g2.setColor(COL_TEXT_WHITE); // White text for timeline
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.drawString(String.valueOf(block.start), xPos, startY + barHeight + 15);

                if (block == blocks.get(blocks.size()-1)) {
                    g2.drawString(String.valueOf(block.end), xPos + blockWidth, startY + barHeight + 15);
                }
            }
        }
    }
}