import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TaskScheduler extends JFrame {

    // Components
    private JTextField txtName, txtArrival, txtBurst, txtQuantum;
    private JComboBox<String> cmbAlgorithm;
    private JTable table;
    private DefaultTableModel tableModel;
    private GanttChartPanel ganttPanel;
    private JButton btnSimulate;

    // Data
    private List<Task> taskList = new ArrayList<>();

    public TaskScheduler() {
        setTitle("Task Scheduler: FCFS & Round Robin");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- Top Panel: Inputs ---
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        inputPanel.add(new JLabel("Task Name:"));
        txtName = new JTextField("T1", 5);
        inputPanel.add(txtName);

        inputPanel.add(new JLabel("Arrival:"));
        txtArrival = new JTextField("0", 3);
        inputPanel.add(txtArrival);

        inputPanel.add(new JLabel("Burst:"));
        txtBurst = new JTextField("5", 3);
        inputPanel.add(txtBurst);

        JButton btnAdd = new JButton("Add Task");
        inputPanel.add(btnAdd);

        inputPanel.add(new JSeparator(SwingConstants.VERTICAL));

        inputPanel.add(new JLabel("Algorithm:"));
        cmbAlgorithm = new JComboBox<>(new String[]{"FCFS", "Round Robin"});
        inputPanel.add(cmbAlgorithm);

        inputPanel.add(new JLabel("Quantum:"));
        txtQuantum = new JTextField("2", 3);
        txtQuantum.setEnabled(false); // Disabled by default for FCFS
        inputPanel.add(txtQuantum);

        btnSimulate = new JButton("Simulate");
        btnSimulate.setBackground(new Color(60, 179, 113));
        btnSimulate.setForeground(Color.WHITE);
        inputPanel.add(btnSimulate);

        JButton btnClear = new JButton("Reset");
        inputPanel.add(btnClear);

        add(inputPanel, BorderLayout.NORTH);

        // --- Center Panel: Table ---
        String[] columns = {"ID", "Arrival", "Burst", "Waiting Time", "Turnaround Time"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Bottom Panel: Gantt Chart ---
        ganttPanel = new GanttChartPanel();
        ganttPanel.setPreferredSize(new Dimension(800, 150));
        ganttPanel.setBorder(BorderFactory.createTitledBorder("Gantt Chart Visualization"));
        add(ganttPanel, BorderLayout.SOUTH);

        // --- Event Listeners ---

        // 1. Enable/Disable Quantum based on Algorithm selection
        cmbAlgorithm.addActionListener(e -> {
            String selected = (String) cmbAlgorithm.getSelectedItem();
            txtQuantum.setEnabled("Round Robin".equals(selected));
        });

        // 2. Add Task Button
        btnAdd.addActionListener(e -> addTask());

        // 3. Simulate Button
        btnSimulate.addActionListener(e -> simulate());

        // 4. Clear Button
        btnClear.addActionListener(e -> reset());
    }

    private void addTask() {
        try {
            String name = txtName.getText();
            int arrival = Integer.parseInt(txtArrival.getText());
            int burst = Integer.parseInt(txtBurst.getText());

            taskList.add(new Task(name, arrival, burst));
            tableModel.addRow(new Object[]{name, arrival, burst, "-", "-"});

            // Auto-increment name for convenience
            int nextId = taskList.size() + 1;
            txtName.setText("T" + nextId);
            txtArrival.setText("");
            txtBurst.setText("");
            txtName.requestFocus();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid integers for Time.");
        }
    }

    private void reset() {
        taskList.clear();
        tableModel.setRowCount(0);
        ganttPanel.setSchedule(new ArrayList<>());
        ganttPanel.repaint();
        txtName.setText("T1");
        txtArrival.setText("0");
        txtBurst.setText("5");
    }

    private void simulate() {
        if (taskList.isEmpty()) return;

        // Clone list to avoid modifying original input data during calculation
        List<Task> simulationList = new ArrayList<>();
        for (Task t : taskList) {
            simulationList.add(new Task(t.id, t.arrivalTime, t.burstTime));
        }

        String algo = (String) cmbAlgorithm.getSelectedItem();
        List<ExecutionBlock> scheduleLog = new ArrayList<>();

        if ("FCFS".equals(algo)) {
            runFCFS(simulationList, scheduleLog);
        } else {
            try {
                int quantum = Integer.parseInt(txtQuantum.getText());
                runRoundRobin(simulationList, quantum, scheduleLog);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Time Quantum.");
                return;
            }
        }

        // Update Table with results
        updateTable(simulationList);

        // Update Gantt Chart
        ganttPanel.setSchedule(scheduleLog);
        ganttPanel.repaint();
    }

    private void updateTable(List<Task> executedTasks) {
        tableModel.setRowCount(0);
        // Map results back to table rows
        for (Task t : executedTasks) {
            tableModel.addRow(new Object[]{
                    t.id, t.arrivalTime, t.burstTime, t.waitingTime, t.turnaroundTime
            });
        }
    }

    // --- Algorithms ---

    private void runFCFS(List<Task> tasks, List<ExecutionBlock> log) {
        // Sort by Arrival Time
        tasks.sort(Comparator.comparingInt(t -> t.arrivalTime));

        int currentTime = 0;

        for (Task t : tasks) {
            // CPU is idle if current time < arrival time
            if (currentTime < t.arrivalTime) {
                currentTime = t.arrivalTime;
            }

            int start = currentTime;
            currentTime += t.burstTime;
            int end = currentTime;

            t.completionTime = end;
            t.turnaroundTime = t.completionTime - t.arrivalTime;
            t.waitingTime = t.turnaroundTime - t.burstTime;

            log.add(new ExecutionBlock(t.id, start, end));
        }
    }

    private void runRoundRobin(List<Task> tasks, int quantum, List<ExecutionBlock> log) {
        // Sort initially by Arrival Time
        tasks.sort(Comparator.comparingInt(t -> t.arrivalTime));

        int currentTime = 0;
        int completed = 0;
        Queue<Task> queue = new LinkedList<>();

        // Track remaining burst times
        for(Task t : tasks) t.remainingTime = t.burstTime;

        // Push initial tasks arriving at time 0
        int i = 0;
        while(i < tasks.size() && tasks.get(i).arrivalTime <= currentTime) {
            queue.add(tasks.get(i));
            i++;
        }

        while(completed < tasks.size()) {
            if (queue.isEmpty()) {
                // If queue is empty but tasks remain, jump time
                if(i < tasks.size()) {
                    currentTime = tasks.get(i).arrivalTime;
                    queue.add(tasks.get(i));
                    i++;
                } else {
                    currentTime++; // Idle time
                }
                continue;
            }

            Task current = queue.poll();
            int start = currentTime;
            int executeTime = Math.min(current.remainingTime, quantum);

            current.remainingTime -= executeTime;
            currentTime += executeTime;

            log.add(new ExecutionBlock(current.id, start, currentTime));

            // Check for new arrivals during this execution
            while(i < tasks.size() && tasks.get(i).arrivalTime <= currentTime) {
                queue.add(tasks.get(i));
                i++;
            }

            if (current.remainingTime > 0) {
                queue.add(current);
            } else {
                current.completionTime = currentTime;
                current.turnaroundTime = current.completionTime - current.arrivalTime;
                current.waitingTime = current.turnaroundTime - current.burstTime;
                completed++;
            }
        }
    }

    // --- Helper Classes ---

    static class Task {
        String id;
        int arrivalTime;
        int burstTime;
        int remainingTime;
        int completionTime;
        int waitingTime;
        int turnaroundTime;

        public Task(String id, int arrival, int burst) {
            this.id = id;
            this.arrivalTime = arrival;
            this.burstTime = burst;
        }
    }

    static class ExecutionBlock {
        String taskId;
        int start;
        int end;

        public ExecutionBlock(String tid, int s, int e) {
            this.taskId = tid;
            this.start = s;
            this.end = e;
        }
    }

    // Custom JPanel for Drawing Gantt Chart
    static class GanttChartPanel extends JPanel {
        private List<ExecutionBlock> schedule = new ArrayList<>();

        public void setSchedule(List<ExecutionBlock> schedule) {
            this.schedule = schedule;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (schedule == null || schedule.isEmpty()) return;

            int height = 50;
            int startY = 50;
            int scale = 20; // Pixels per time unit

            // Dynamic scaling if simulation is long
            int totalTime = schedule.get(schedule.size() - 1).end;
            if (totalTime * scale > getWidth()) {
                scale = Math.max(2, getWidth() / (totalTime + 2));
            }

            for (ExecutionBlock block : schedule) {
                int x = block.start * scale + 10;
                int width = (block.end - block.start) * scale;

                // Draw Bar
                g.setColor(new Color(100, 149, 237)); // Cornflower Blue
                g.fillRect(x, startY, width, height);
                g.setColor(Color.BLACK);
                g.drawRect(x, startY, width, height);

                // Draw Task Label inside
                g.setColor(Color.WHITE);
                g.drawString(block.taskId, x + 5, startY + 25);

                // Draw Time Labels
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(block.start), x, startY + height + 15);
                // Draw end time
                g.drawString(String.valueOf(block.end), x + width, startY + height + 15);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new TaskScheduler().setVisible(true));
    }
}