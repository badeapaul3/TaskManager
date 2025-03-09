import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * @author hatzp
 **/
public class Main {

    private static TaskManager manager;
    private static JTextArea taskArea;

    private static JButton processButton; // Made field to access in callback

    public static void main(String[] args) throws InterruptedException {

        manager = new TaskManager();
        if (manager == null){
            System.err.println("Failed to initialize Task Manager");
            return;
        }

        SwingUtilities.invokeLater(() -> createAndShowGUI());

    }

    private static void createAndShowGUI(){

        if (manager == null) {
            JOptionPane.showMessageDialog(null, "TaskManager not initialized!");
            return;
        }

        //Main window
        JFrame frame = new JFrame("Task Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000,600);
        frame.setMinimumSize(new Dimension(800,400));
        frame.setResizable(true);

        //Task display area
        taskArea = new JTextArea(20,80);
        taskArea.setEditable(false);
        taskArea.setLineWrap(true);
        taskArea.setWrapStyleWord(true);
        updateTaskDisplay();
        JScrollPane scrollPane = new JScrollPane(taskArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        //Input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JTextField titleField = new JTextField(20);
        JTextField dueField = new JTextField(15); // Format: yyyy-MM-dd HH:mm
        JTextField effortField = new JTextField(5);
        JButton addButton = new JButton("Add Task");
        JButton sortDueButton = new JButton("Sort by Due Date");
        JButton sortEffortButton = new JButton("Sort by Effort");
        processButton = new JButton("Process Tasks"); //now it is field
        JButton revertButton = new JButton("Revert Tasks Completion");


        manager.setUpdateCallback(
                () -> {
                    updateTaskDisplay();
                    if(manager.getTasksDueBefore(LocalDateTime.now().plusYears(10)).isEmpty()){
                        processButton.setEnabled(true);
                    }
                }
        );

        //Add Task action
        addButton.addActionListener(e -> {
                try {
                    String title = titleField.getText();
                    if (title.isBlank()) throw new IllegalArgumentException("Title is required");
                    LocalDateTime dueDate = dueField.getText().isBlank() ? null : LocalDateTime.parse(dueField.getText(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    BigDecimal effort = effortField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(effortField.getText());

                    Task newTask = Task.createTask(title, "", LocalDateTime.now(), dueDate, false, "General", "", effort);
                    manager.addTask(newTask);
                    updateTaskDisplay();
                    clearFields(titleField, dueField, effortField);


                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid date format. Use yyyy-MM-dd HH:mm");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
                }
            }
        );

        sortDueButton.addActionListener(e -> {
            manager.sortByDueDate();
            updateTaskDisplay();
        });

        sortEffortButton.addActionListener(e -> {
            manager.sortByEffort();
            updateTaskDisplay();
        });

        processButton.addActionListener(e -> {
            processButton.setEnabled(false);
            new Thread(
                    () -> manager.processTasks()).start(); // Simplified - logic moved to TaskManager
        });

        revertButton.addActionListener(e -> {
            manager.revertTasks();
            updateTaskDisplay();
        });

        inputPanel.add(new JLabel("Title:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Due (yyyy-MM-dd HH:mm):"));
        inputPanel.add(dueField);
        inputPanel.add(new JLabel("Effort (h):"));
        inputPanel.add(effortField);
        inputPanel.add(addButton);
        inputPanel.add(sortDueButton);
        inputPanel.add(sortEffortButton);
        inputPanel.add(processButton);
        inputPanel.add(revertButton);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.pack(); // Adjusts size to fit components
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);

    }

    private static void updateTaskDisplay(){
        taskArea.setText("");
        for(Task task : manager.getTasksByCategory(null)){
            taskArea.append(task.toString() + "\n");
        }
    }

    private static void clearFields(JTextField... fields){
        for (JTextField field : fields){
            field.setText("");
        }
    }

}
