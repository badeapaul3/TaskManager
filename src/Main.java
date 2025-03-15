import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hatzp
 **/
public class Main {

    private static TaskManager manager;
    private static JList<Task> taskList;

    private static JButton processButton; // Made field to access in callback

    public static void main(String[] args) throws InterruptedException {

        manager = new TaskManager();
        if (manager == null){
            System.err.println("Failed to initialize Task Manager");
            return;
        }
        SwingUtilities.invokeLater(Main::createAndShowGUI);
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

        //Task list
        taskList = new JList<>(new DefaultListModel<>());
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        updateTaskDisplay();
        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        //Input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 1, 10,10));

        JPanel addTaskPanel = new JPanel();
        addTaskPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JTextField titleField = new JTextField(20);
        JTextField dueField = new JTextField(15); // Format: yyyy-MM-dd HH:mm
        JTextField effortField = new JTextField(5);
        JComboBox<Task.Priority> priorityCombo = new JComboBox<>(Task.Priority.values());
        JList<Task> dependencyList = new JList<>(new DefaultListModel<>());
        dependencyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        updateDependencyList(dependencyList);
        JScrollPane dependencyScroll = new JScrollPane(dependencyList);
        dependencyScroll.setPreferredSize(new Dimension(200,100));
        JButton addButton = new JButton("Add Task");

        addTaskPanel.add(new JLabel("Title:"));
        addTaskPanel.add(titleField);
        addTaskPanel.add(new JLabel("Due (yyyy-MM-dd HH:mm):"));
        addTaskPanel.add(dueField);
        addTaskPanel.add(new JLabel("Effort (h):"));
        addTaskPanel.add(effortField);
        addTaskPanel.add(new JLabel("Priority:"));
        addTaskPanel.add(priorityCombo);
        addTaskPanel.add(new JLabel("Dependencies:"));
        addTaskPanel.add(dependencyScroll);
        addTaskPanel.add(addButton);


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton sortDueButton = new JButton("Sort by Due Date");
        JButton sortEffortButton = new JButton("Sort by Effort");
        JButton sortPriorityButton = new JButton("Sort by Priority");
        processButton = new JButton("Process Tasks"); //now it is field
        JButton revertButton = new JButton("Revert Tasks Completion");
        JButton exportButton = new JButton("Export to CSV");// new File export import features
        JButton importButton = new JButton("Import from CSV");
        JButton deleteButton = new JButton("Delete Task");

        buttonPanel.add(sortDueButton);
        buttonPanel.add(sortEffortButton);
        buttonPanel.add(sortPriorityButton);
        buttonPanel.add(processButton);
        buttonPanel.add(revertButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(importButton);
        buttonPanel.add(deleteButton);

        inputPanel.add(addTaskPanel);
        inputPanel.add(buttonPanel);

        //Event listeners
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
                    Task.Priority priority = (Task.Priority) priorityCombo.getSelectedItem();

                    List<Task> selectedDependencies = dependencyList.getSelectedValuesList();
                    List<Integer> dependencyIds = selectedDependencies.stream().map(Task::id).toList();


                    Task newTask = Task.createTask(title, "", LocalDateTime.now(), dueDate, false,
                            "General", "", effort, priority, dependencyIds);
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

        sortPriorityButton.addActionListener( e->{
            manager.sortByPriority();
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

        exportButton.addActionListener(e -> {
            try{
                manager.exportTasksToCsv("tasks.csv"); //Export to project root
                JOptionPane.showMessageDialog(frame, "Tasks exported to tasks.csv");
            }catch (Exception ex){
                JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage());
            }
        });

        importButton.addActionListener(e -> {
            try {
                manager.importTasksFromCsv("tasks.csv");    //Import from project root
                JOptionPane.showMessageDialog(frame, "Tasks imported from tasks.csv");
            }catch (Exception ex){
                JOptionPane.showMessageDialog(frame, "Import failed: " + ex.getMessage());
            }
        });

        deleteButton.addActionListener(e -> {
            Task selectedTask = taskList.getSelectedValue();
            if(selectedTask != null){
                boolean deleted = manager.deleteTask(selectedTask.id());
                if(deleted){
                    updateTaskDisplay();
                    updateDependencyList(dependencyList);
                    JOptionPane.showMessageDialog(frame, "Task deleted: " + selectedTask.title() + " with id " + selectedTask.id());
                }else{
                    JOptionPane.showMessageDialog(frame, "Cannot delete " + selectedTask.title() + " with id " + selectedTask.id()
                            + ".\nOther tasks depend on it.");
                }
            }else{
                JOptionPane.showMessageDialog(frame, "Please select a task to delete.");
            }
        });

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.pack(); // Adjusts size to fit components
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);

    }

    private static void updateDependencyList(JList<Task> dependencyList){
        DefaultListModel<Task> model = (DefaultListModel<Task>) dependencyList.getModel();
        model.clear();
        for (Task task : manager.getAllTasks()){
            model.addElement(task);
        }
    }

    private static void updateTaskDisplay(){
        DefaultListModel<Task> model = (DefaultListModel<Task>) taskList.getModel();
        model.clear();
        for(Task task : manager.getTasksByCategory(null)){
            model.addElement(task);
        }
    }

    private static void clearFields(JTextField... fields){
        for (JTextField field : fields){
            field.setText("");
        }
    }

}
