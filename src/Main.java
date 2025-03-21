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

    private static JButton addButton; // static for mode toggle
    private static JButton processButton; // Made field to access in callback

    private static Task editingTask = null; //task being edited

    public static void main(String[] args) throws InterruptedException {

        try { // Step 13: Wrap manager initialization
            manager = new TaskManager();
            if (manager == null) {
                throw new RuntimeException("TaskManager initialization returned null"); // Step 13
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to initialize Task Manager: " + e.getMessage()); // Step 13
            System.err.println("Failed to initialize Task Manager: " + e.getMessage());
            return;
        }
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI(){

        //Main window
        JFrame frame = new JFrame("Task Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1600,800);
        frame.setMinimumSize(new Dimension(1400,500));
        frame.setResizable(true);

        //Task list
        taskList = new JList<>(new DefaultListModel<>());
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        updateTaskDisplay(null);
        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        //Input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 1, 10,10));

        //add/edit task panel
        JPanel addTaskPanel = new JPanel();
        addTaskPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        addTaskPanel.setPreferredSize(new Dimension(1100,150));

        JTextField titleField = new JTextField(20);
        JTextField dueField = new JTextField(15); // Format: yyyy-MM-dd HH:mm
        JTextField effortField = new JTextField(5);
        JComboBox<Task.Priority> priorityCombo = new JComboBox<>(Task.Priority.values());
        JList<Task> dependencyList = new JList<>(new DefaultListModel<>());
        dependencyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        updateDependencyList(dependencyList);
        JScrollPane dependencyScroll = new JScrollPane(dependencyList);
        dependencyScroll.setPreferredSize(new Dimension(200,100));
        addButton = new JButton("Add Task");
        JComboBox<String> categoryCombo = new JComboBox<>();
        categoryCombo.setEditable(true);
        updateCategoryCombo(categoryCombo);


        addTaskPanel.add(new JLabel("Title:"));
        addTaskPanel.add(titleField);
        addTaskPanel.add(new JLabel("Due (yyyy-MM-dd HH:mm):"));
        addTaskPanel.add(dueField);
        addTaskPanel.add(new JLabel("Effort (h):"));
        addTaskPanel.add(effortField);
        addTaskPanel.add(new JLabel("Priority:"));
        addTaskPanel.add(priorityCombo);
        addTaskPanel.add(new JLabel("Category: "));
        addTaskPanel.add(categoryCombo);
        addTaskPanel.add(new JLabel("Dependencies:"));
        addTaskPanel.add(dependencyScroll);
        addTaskPanel.add(addButton);


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));

        JComboBox<String> filterCategoryCombo = new JComboBox<>();
        filterCategoryCombo.setEditable(false);
        filterCategoryCombo.setPreferredSize(new Dimension(150,25)); // ensure visible size


        JButton sortDueButton = new JButton("Sort by Due Date");
        JButton sortEffortButton = new JButton("Sort by Effort");
        JButton sortPriorityButton = new JButton("Sort by Priority");
        processButton = new JButton("Process Tasks"); //now it is field
        JButton revertButton = new JButton("Revert Tasks Completion");
        JButton exportButton = new JButton("Export to CSV");// new File export import features
        JButton importButton = new JButton("Import from CSV");
        JButton deleteButton = new JButton("Delete Task");
        JButton editButton = new JButton("Edit Task");


        buttonPanel.add(new JLabel("Filter by Category:"));
        buttonPanel.add(filterCategoryCombo);
        buttonPanel.add(sortDueButton);
        buttonPanel.add(sortEffortButton);
        buttonPanel.add(sortPriorityButton);
        buttonPanel.add(processButton);
        buttonPanel.add(revertButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(importButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(editButton);

        inputPanel.add(addTaskPanel);
        inputPanel.add(buttonPanel);

        updateFilterCategoryCombo(filterCategoryCombo);
        filterCategoryCombo.setSelectedItem("All Categories");

        //Event listeners
        manager.setUpdateCallback(
                () -> {
                    updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
                    updateDependencyList(dependencyList);
                    updateCategoryCombo(categoryCombo);
                    updateFilterCategoryCombo(filterCategoryCombo);
                    processButton.setEnabled(true);
                    processButton.repaint();
                    //reset to add mode after updates
                    resetInputFields(titleField, dueField, effortField, priorityCombo, categoryCombo, dependencyList, addTaskPanel);
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
                    String category = (String) categoryCombo.getSelectedItem();

                    List<Task> selectedDependencies = dependencyList.getSelectedValuesList();
                    List<Integer> dependencyIds = selectedDependencies.stream().map(Task::id).toList();

                    if(editingTask == null) {
                        Task newTask = Task.createTask(title, "", LocalDateTime.now(), dueDate, false,
                                category, "", effort, priority, dependencyIds);
                        manager.addTask(newTask);
                    } else {
                        Task updatedTask = new Task(editingTask.id(), title, editingTask.description(), editingTask.createdAt(),
                                dueDate, editingTask.isCompleted(), category, editingTask.notes(), effort, priority, dependencyIds);
                        manager.updateTask(updatedTask);
                    }
                    updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
                    updateDependencyList(dependencyList);
                    resetInputFields(titleField, dueField, effortField, priorityCombo, categoryCombo, dependencyList, addTaskPanel);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid effort value: " + ex.getMessage()); // Step 13
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid date format. Use yyyy-MM-dd HH:mm. Details: " + ex.getMessage());
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(frame, ex.getMessage()); // Step 13: Show validation errors
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error adding task: "+ ex.getMessage());
                }
            }
        );

        sortDueButton.addActionListener(e -> {
            try { // Step 13: Wrap sorting
                manager.sortByDueDate();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error sorting by due date: " + ex.getMessage()); // Step 13
            }
        });

        sortEffortButton.addActionListener(e -> {
            try { // Step 13: Wrap sorting
                manager.sortByEffort();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error sorting by effort: " + ex.getMessage()); // Step 13
            }
        });

        sortPriorityButton.addActionListener( e->{
            try { // Step 13: Wrap sorting
                manager.sortByPriority();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error sorting by priority: " + ex.getMessage()); // Step 13
            }
        });

        processButton.addActionListener(e -> {
            try { // Step 13: Wrap processing
                processButton.setEnabled(false);
                new Thread(() -> manager.processTasks()).start(); // Simplified - logic moved to TaskManager
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error starting task processing: " + ex.getMessage()); // Step 13
                processButton.setEnabled(true); // Step 13: Reset button on failure
            }
        });

        revertButton.addActionListener(e -> {
            try { // Step 13: Wrap revert
                manager.revertTasks();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error reverting tasks: " + ex.getMessage()); // Step 13
            }
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
                try { // Step 13: Wrap deletion
                    boolean deleted = manager.deleteTask(selectedTask.id());
                    if (deleted) {
                        updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
                        updateDependencyList(dependencyList);
                        JOptionPane.showMessageDialog(frame, "Task deleted: " + selectedTask.title() + " with id " + selectedTask.id());
                    } else {
                        JOptionPane.showMessageDialog(frame, "Cannot delete " + selectedTask.title() + " with id " + selectedTask.id()
                                + ".\nOther tasks depend on it.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error deleting task: " + ex.getMessage()); // Step 13
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a task to delete.");
            }
        });

        editButton.addActionListener(e -> {
            Task selectedTask = taskList.getSelectedValue();
            if (selectedTask != null) {
                try { // Step 13: Wrap edit setup
                    editingTask = selectedTask;
                    titleField.setText(selectedTask.title());
                    dueField.setText(selectedTask.dueDate() != null ? selectedTask.dueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                    effortField.setText(selectedTask.effort().toString());
                    priorityCombo.setSelectedItem(selectedTask.priority());

                    categoryCombo.setSelectedItem(selectedTask.category() != null ? selectedTask.category() : "");

                    DefaultListModel<Task> depModel = (DefaultListModel<Task>) dependencyList.getModel();
                    List<Integer> depIndices = new ArrayList<>();
                    for (int i = 0; i < depModel.size(); i++) {
                        if (selectedTask.dependencies().contains(depModel.get(i).id())) {
                            depIndices.add(i);
                        }
                    }

                    dependencyList.setSelectedIndices(depIndices.stream().mapToInt(Integer::intValue).toArray());
                    addButton.setText("Save Changes");
                    // Added to ensure button visibility
                    addButton.setVisible(true);
                    addTaskPanel.revalidate();
                    addTaskPanel.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error setting up task edit: " + ex.getMessage()); // Step 13
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a task to edit.");
            }
        });

        filterCategoryCombo.addActionListener(e -> {
            try { // Step 13: Wrap filter update
                String selectedCategory = (String) filterCategoryCombo.getSelectedItem();
                updateTaskDisplay(selectedCategory == null || selectedCategory.equals("All Categories") ? null : selectedCategory);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error filtering tasks: " + ex.getMessage()); // Step 13
            }
        });

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.pack(); // Adjusts size to fit components
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            updateFilterCategoryCombo(filterCategoryCombo);
            filterCategoryCombo.setSelectedItem("All Categories");
            filterCategoryCombo.revalidate();
            filterCategoryCombo.repaint();
            System.out.println("Post-render: Items: " + filterCategoryCombo.getItemCount() +
                    ", Selected: " + filterCategoryCombo.getSelectedItem());
        });

    }


    private static void updateDependencyList(JList<Task> dependencyList){
        DefaultListModel<Task> model = (DefaultListModel<Task>) dependencyList.getModel();
        model.clear();
        for (Task task : manager.getAllTasks()){
            model.addElement(task);
        }
    }

    private static void updateTaskDisplay(String category){
        DefaultListModel<Task> model = (DefaultListModel<Task>) taskList.getModel();
        model.clear();
        for(Task task : manager.getTasksByCategory(category)){
            model.addElement(task);
        }
    }

    private static void clearFields(JTextField... fields){
        for (JTextField field : fields){
            field.setText("");
        }
    }

    // Step 10: Added to refactor duplicated reset logic DRY principle - Don't repeat yourself
    private static void resetInputFields(JTextField titleField, JTextField dueField, JTextField effortField,
                                         JComboBox<Task.Priority> priorityCombo, JComboBox<String> categoryCombo,
                                         JList<Task> dependencyList, JPanel addTaskPanel) {
        updateDependencyList(dependencyList);
        clearFields(titleField, dueField, effortField);
        priorityCombo.setSelectedItem(Task.Priority.MEDIUM);
        categoryCombo.setSelectedItem(null);
        dependencyList.clearSelection();
        editingTask = null;
        addButton.setText("Add Task");
        addButton.setVisible(true);
        addTaskPanel.revalidate();
        addTaskPanel.repaint();
    }

    private static void updateCategoryCombo(JComboBox<String> categoryCombo){
        String selected = (String) categoryCombo.getSelectedItem();
        categoryCombo.removeAllItems();
        for(String category : manager.getCategories()){
            categoryCombo.addItem(category);
        }
        categoryCombo.setSelectedItem(selected);
    }

    private static void updateFilterCategoryCombo(JComboBox<String> filterCategoryCombo){
        String selected = (String) filterCategoryCombo.getSelectedItem();
        filterCategoryCombo.removeAllItems();
        filterCategoryCombo.addItem("All Categories");
        for(String category : manager.getCategories()){
            filterCategoryCombo.addItem(category);
        }
        filterCategoryCombo.setSelectedItem(selected != null && filterCategoryCombo.getItemCount() > 1 ? selected : "All Categories");
    }

}
