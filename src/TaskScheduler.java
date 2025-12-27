import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
 * A CPU Scheduling Simulator (FCFS & Round Robin).
 *
 * Design Updates:
 * - Monochrome & Minimalist Aesthetic (Black/White/Gray).
 * - Dynamic UI: Quantum input completely disappears for FCFS.
 * - Enhanced Gantt Chart: Detailed timeline markers for all transitions.
 */
public class TaskScheduler extends JFrame {

    // --- Monochrome Palette ---
    private static final Color COL_BG = Color.WHITE;
    private static final Color COL_FG = Color.BLACK;
    private static final Color COL_ACCENT = new Color(80, 80, 80); // Dark Gray for highlights
    private static final Color COL_LIGHT_GRAY = new Color(240, 240, 240); // Table headers/fields
    private static final Color COL_BORDER = Color.LIGHT_GRAY;

    // --- Fonts ---
    private static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);

    // --- Components ---
    private JTextField tfProcessId, tfArrivalTime, tfBurstTime, tfQuantum;
    private JComboBox<String> cbAlgorithm;
    private JPanel quantumPanel; // Container to hide/show quantum input
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblAvgTurnaround, lblAvgWaiting, lblThroughput;
    private GanttPanel ganttPanel;

    // --- Data ---
    private ArrayList<Process> processList = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // Force basic colors for a consistent monochrome look across platforms
                UIManager.put("Panel.background", Color.WHITE);
                UIManager.put("OptionPane.background", Color.WHITE);
                UIManager.put("OptionPane.messageForeground", Color.BLACK);
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TaskScheduler().setVisible(true);
        });
    }

    public TaskScheduler() {
        setTitle("CPU Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(COL_BG);

        // -- Layout Structure --
        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 30, 0));
        mainPanel.setBackground(COL_BG);
        mainPanel.setBorder(new EmptyBorder(25, 25, 10, 25));

        // --- Left: Input Section ---
        JPanel inputSection = new JPanel(new BorderLayout(0, 10));
        inputSection.setBackground(COL_BG);

        JLabel inputTitle = new JLabel("Add Process");
        inputTitle.setFont(FONT_TITLE);
        inputSection.add(inputTitle, BorderLayout.NORTH);

        JPanel inputFields = new JPanel(new GridLayout(2, 3, 10, 10));
        inputFields.setBackground(COL_BG);

        // Labels
        inputFields.add(new JLabel("Process ID"));
        inputFields.add(new JLabel("Arrival Time"));
        inputFields.add(new JLabel("Burst Time"));

        // Fields
        tfProcessId = createFlatField();
        tfArrivalTime = createFlatField();
        tfBurstTime = createFlatField();

        inputFields.add(tfProcessId);
        inputFields.add(tfArrivalTime);
        inputFields.add(tfBurstTime);
        inputSection.add(inputFields, BorderLayout.CENTER);

        JButton btnAdd = createDarkButton("Add Process");
        btnAdd.addActionListener(e -> addProcess());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        btnPanel.setBackground(COL_BG);
        btnPanel.add(btnAdd);
        inputSection.add(btnPanel, BorderLayout.SOUTH);

        // --- Right: Control Section ---
        JPanel controlSection = new JPanel(new GridBagLayout());
        controlSection.setBackground(COL_BG);
        controlSection.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COL_BORDER),
                new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.gridx = 0; gbc.gridy = 0;

        controlSection.add(new JLabel("Scheduling Method:"), gbc);

        cbAlgorithm = new JComboBox<>(new String[]{"First Come First Served", "Round Robin"});
        cbAlgorithm.setFont(FONT_UI);
        cbAlgorithm.setFocusable(false);
        cbAlgorithm.setBackground(Color.WHITE);

        gbc.gridy = 1;
        controlSection.add(cbAlgorithm, gbc);

        // Quantum Field (Hidden by default)
        quantumPanel = new JPanel(new BorderLayout(5, 0));
        quantumPanel.setBackground(COL_BG);
        quantumPanel.setVisible(false); // Initially hidden for FCFS

        JLabel lblQ = new JLabel("Time Quantum:");
        tfQuantum = createFlatField();
        tfQuantum.setText("2");

        quantumPanel.add(lblQ, BorderLayout.WEST);
        quantumPanel.add(tfQuantum, BorderLayout.CENTER);

        gbc.gridy = 2;
        controlSection.add(quantumPanel, gbc);

        // Toggle Logic
        cbAlgorithm.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean isRR = "Round Robin".equals(e.getItem());
                quantumPanel.setVisible(isRR);
                controlSection.revalidate(); // Refresh layout to hide/show gap
                controlSection.repaint();
            }
        });

        JButton btnCalculate = createDarkButton("Calculate Schedule");
        btnCalculate.addActionListener(e -> calculateSchedule());

        gbc.gridy = 3;
        gbc.weighty = 1.0; // Push button to bottom
        gbc.anchor = GridBagConstraints.SOUTH;
        controlSection.add(btnCalculate, gbc);

        mainPanel.add(inputSection);
        mainPanel.add(controlSection);
        return mainPanel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBackground(COL_BG);
        panel.setBorder(new EmptyBorder(10, 25, 10, 25));

        // --- Table ---
        String[] cols = {"ID", "Arrival", "Burst", "Completed", "Waiting", "Turnaround"};
        tableModel = new DefaultTableModel(cols, 0);
        table = new JTable(tableModel) {
            public boolean isCellEditable(int row, int column) { return false; }
        };

        // Minimalist Table Styling
        table.setFont(FONT_UI);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setGridColor(COL_BORDER);
        table.setSelectionBackground(Color.BLACK);
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(COL_BG);
        header.setForeground(COL_FG);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.BLACK));

        // Center text in table
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(COL_BG);
        scroll.setBorder(new LineBorder(COL_BORDER));

        // --- Stats Sidebar ---
        JPanel statsContainer = new JPanel();
        statsContainer.setLayout(new BoxLayout(statsContainer, BoxLayout.Y_AXIS));
        statsContainer.setBackground(COL_BG);
        statsContainer.setPreferredSize(new Dimension(200, 0));
        statsContainer.setBorder(new EmptyBorder(0, 0, 0, 0));

        lblAvgTurnaround = addStatBox(statsContainer, "Avg Turnaround");
        statsContainer.add(Box.createVerticalStrut(15));
        lblAvgWaiting = addStatBox(statsContainer, "Avg Waiting");
        statsContainer.add(Box.createVerticalStrut(15));
        lblThroughput = addStatBox(statsContainer, "Throughput");

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(statsContainer, BorderLayout.EAST);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COL_BG);
        panel.setBorder(new EmptyBorder(10, 25, 25, 25));

        JLabel title = new JLabel("Gantt Chart Visualization");
        title.setFont(FONT_BOLD);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));

        ganttPanel = new GanttPanel();
        ganttPanel.setPreferredSize(new Dimension(0, 100));
        ganttPanel.setBackground(COL_BG);
        ganttPanel.setBorder(new LineBorder(COL_FG, 1)); // Simple black border

        panel.add(title, BorderLayout.NORTH);
        panel.add(ganttPanel, BorderLayout.CENTER);

        return panel;
    }

    // --- Components Factory ---

    private JTextField createFlatField() {
        JTextField tf = new JTextField();
        tf.setFont(FONT_UI);
        tf.setBackground(COL_LIGHT_GRAY);
        tf.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8)); // No border, just bg
        return tf;
    }

    private JButton createDarkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel addStatBox(JPanel container, String title) {
        JLabel header = new JLabel(title);
        header.setFont(FONT_UI);
        header.setForeground(Color.GRAY);

        JLabel value = new JLabel("0.00");
        value.setFont(new Font("Segoe UI", Font.BOLD, 22));
        value.setForeground(COL_FG);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(COL_BG);
        p.add(header, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(200, 60));

        container.add(p);
        return value;
    }

    // --- Logic & Algorithms ---

    private void addProcess() {
        try {
            String pid = tfProcessId.getText();
            int arr = Integer.parseInt(tfArrivalTime.getText());
            int burst = Integer.parseInt(tfBurstTime.getText());

            if (pid.isEmpty()) throw new Exception();

            processList.add(new Process(pid, arr, burst));
            tableModel.addRow(new Object[]{pid, arr, burst, "-", "-", "-"});

            tfProcessId.setText("");
            tfArrivalTime.setText("");
            tfBurstTime.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Input", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateSchedule() {
        if (processList.isEmpty()) return;

        // Reset
        for(Process p : processList) p.reset();
        List<GanttBlock> blocks = new ArrayList<>();

        String algo = (String) cbAlgorithm.getSelectedItem();

        if ("First Come First Served".equals(algo)) {
            runFCFS(blocks);
        } else {
            try {
                int q = Integer.parseInt(tfQuantum.getText());
                if (q < 1) throw new Exception();
                runRR(q, blocks);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid Quantum", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        updateResults(blocks);
    }

    private void runFCFS(List<GanttBlock> blocks) {
        processList.sort(Comparator.comparingInt(p -> p.arrivalTime));
        int time = 0;

        for (Process p : processList) {
            if (time < p.arrivalTime) time = p.arrivalTime; // Idle handling

            int start = time;
            time += p.burstTime;
            int end = time;

            p.completionTime = end;
            p.turnaroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;

            blocks.add(new GanttBlock(p.pid, start, end));
        }
    }

    private void runRR(int quantum, List<GanttBlock> blocks) {
        processList.sort(Comparator.comparingInt(p -> p.arrivalTime));
        Queue<Process> queue = new LinkedList<>();
        int time = 0;
        int completed = 0;
        int n = processList.size();
        int index = 0;

        // Push initial processes
        if(n > 0) {
            // Jump to first arrival if needed
            if(processList.get(0).arrivalTime > time) time = processList.get(0).arrivalTime;
            while(index < n && processList.get(index).arrivalTime <= time) {
                queue.add(processList.get(index));
                index++;
            }
        }

        while(completed < n) {
            if(queue.isEmpty()) {
                if(index < n) {
                    time = processList.get(index).arrivalTime;
                    while(index < n && processList.get(index).arrivalTime <= time) {
                        queue.add(processList.get(index));
                        index++;
                    }
                } else break;
            }

            Process p = queue.poll();
            int start = time;
            int slice = Math.min(p.remainingTime, quantum);

            p.remainingTime -= slice;
            time += slice;

            blocks.add(new GanttBlock(p.pid, start, time));

            // Add newly arrived processes
            while(index < n && processList.get(index).arrivalTime <= time) {
                queue.add(processList.get(index));
                index++;
            }

            if(p.remainingTime > 0) {
                queue.add(p);
            } else {
                completed++;
                p.completionTime = time;
                p.turnaroundTime = p.completionTime - p.arrivalTime;
                p.waitingTime = p.turnaroundTime - p.burstTime;
            }
        }
    }

    private void updateResults(List<GanttBlock> blocks) {
        tableModel.setRowCount(0);
        double totalWait = 0, totalTurn = 0;
        int maxTime = 0;

        for(Process p : processList) {
            tableModel.addRow(new Object[]{p.pid, p.arrivalTime, p.burstTime, p.completionTime, p.waitingTime, p.turnaroundTime});
            totalWait += p.waitingTime;
            totalTurn += p.turnaroundTime;
            maxTime = Math.max(maxTime, p.completionTime);
        }

        lblAvgTurnaround.setText(String.format("%.2f", totalTurn / processList.size()));
        lblAvgWaiting.setText(String.format("%.2f", totalWait / processList.size()));
        lblThroughput.setText(String.format("%.4f", (double)processList.size() / maxTime));

        ganttPanel.updateData(blocks, maxTime);
    }

    // --- Custom Gantt Panel ---
    class GanttPanel extends JPanel {
        private List<GanttBlock> blocks;
        private int totalTime;

        public void updateData(List<GanttBlock> blocks, int totalTime) {
            this.blocks = blocks;
            this.totalTime = totalTime;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (blocks == null || blocks.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 50;
            int h = getHeight();
            int startX = 25;
            int barY = 30;
            int barH = 40;

            // Scale calculations
            double unitWidth = (double) w / totalTime;

            for (GanttBlock b : blocks) {
                int x = startX + (int)(b.start * unitWidth);
                int bw = (int)((b.end - b.start) * unitWidth);

                // Draw Block
                g2.setColor(Color.WHITE);
                g2.fillRect(x, barY, bw, barH);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, barY, bw, barH);

                // Center PID Text
                String label = b.pid;
                FontMetrics fm = g2.getFontMetrics();
                int textX = x + (bw - fm.stringWidth(label)) / 2;
                int textY = barY + (barH + fm.getAscent()) / 2 - 3;

                // Clip text if block is too small
                if(bw > fm.stringWidth(label)) {
                    g2.drawString(label, textX, textY);
                }

                // Draw Time Markers (Detailed)
                // Draw start time at bottom left of block
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString(String.valueOf(b.start), x - 2, barY + barH + 15);
            }

            // Draw final end time
            int finalX = startX + (int)(totalTime * unitWidth);
            g2.drawString(String.valueOf(totalTime), finalX - 3, barY + barH + 15);
        }
    }

    // --- Data Classes ---
    class Process {
        String pid;
        int arrivalTime, burstTime, remainingTime;
        int completionTime, waitingTime, turnaroundTime;

        Process(String id, int arr, int burst) {
            this.pid = id; this.arrivalTime = arr; this.burstTime = burst;
            reset();
        }
        void reset() { remainingTime = burstTime; completionTime=0; waitingTime=0; turnaroundTime=0; }
    }

    class GanttBlock {
        String pid; int start, end;
        GanttBlock(String p, int s, int e) { pid = p; start = s; end = e; }
    }
}