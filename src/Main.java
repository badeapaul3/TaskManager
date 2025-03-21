import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class Main {
    private static TaskManager manager;
    private static JList<Task> taskList;
    private static JButton addButton;
    private static JButton processButton;
    private static Task editingTask = null;

    public static void main(String[] args) throws InterruptedException {
        try {
            manager = new TaskManager();
            if (manager == null) {
                throw new RuntimeException("TaskManager initialization returned null"); // Step 13
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to initialize Task Manager: " + e.getMessage()); // Step 13
            return;
        }
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Task Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1600, 800);
        frame.setMinimumSize(new Dimension(1400, 500));
        frame.setResizable(true);

        taskList = new JList<>(new DefaultListModel<>());
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        updateTaskDisplay(null);
        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 1, 10, 10));

        JPanel addTaskPanel = new JPanel();
        addTaskPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        addTaskPanel.setPreferredSize(new Dimension(1100, 150));

        JTextField titleField = new JTextField(20);
        JTextField dueField = new JTextField(15);
        JTextField effortField = new JTextField(5);
        JComboBox<Task.Priority> priorityCombo = new JComboBox<>(Task.Priority.values());
        JList<Task> dependencyList = new JList<>(new DefaultListModel<>());
        dependencyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        updateDependencyList(dependencyList);
        JScrollPane dependencyScroll = new JScrollPane(dependencyList);
        dependencyScroll.setPreferredSize(new Dimension(200, 100));
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
        filterCategoryCombo.setPreferredSize(new Dimension(150, 25));

        JButton sortDueButton = new JButton("Sort by Due Date");
        JButton sortEffortButton = new JButton("Sort by Effort");
        JButton sortPriorityButton = new JButton("Sort by Priority");
        processButton = new JButton("Process Tasks");
        JButton revertButton = new JButton("Revert Tasks Completion");
        JButton exportButton = new JButton("Export to CSV");
        JButton importButton = new JButton("Import from CSV");
        JButton deleteButton = new JButton("Delete Task");
        JButton editButton = new JButton("Edit Task");
        JButton reloadButton = new JButton("Reload from DB"); // Step 13: Added

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
        buttonPanel.add(reloadButton);

        inputPanel.add(addTaskPanel);
        inputPanel.add(buttonPanel);

        updateFilterCategoryCombo(filterCategoryCombo);
        filterCategoryCombo.setSelectedItem("All Categories");

        manager.setUpdateCallback(() -> {
            updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            updateDependencyList(dependencyList);
            updateCategoryCombo(categoryCombo);
            updateFilterCategoryCombo(filterCategoryCombo);
            processButton.setEnabled(true);
            processButton.repaint();
            resetInputFields(titleField, dueField, effortField, priorityCombo, categoryCombo, dependencyList, addTaskPanel);
        });

        addButton.addActionListener(e -> {
            try {
                String title = titleField.getText();
                if (title.isBlank()) throw new IllegalArgumentException("Title is required");
                LocalDateTime dueDate = dueField.getText().isBlank() ? null : LocalDateTime.parse(dueField.getText(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                BigDecimal effort = effortField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(effortField.getText());
                if (effort.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Effort cannot be negative"); // Step 13
                Task.Priority priority = (Task.Priority) priorityCombo.getSelectedItem();
                if (priority == null) throw new IllegalArgumentException("Priority must be selected"); // Step 13
                String category = (String) categoryCombo.getSelectedItem();

                List<Task> selectedDependencies = dependencyList.getSelectedValuesList();
                List<Integer> dependencyIds = selectedDependencies.stream().map(Task::id).toList();

                if (editingTask == null) {
                    Task newTask = Task.createTask(title, "", LocalDateTime.now(), dueDate, false,
                            category, "", effort, priority, dependencyIds);
                    manager.addTask(newTask);
                } else {
                    Task updatedTask = new Task(editingTask.id(), title, editingTask.description(), editingTask.createdAt(),
                            dueDate, editingTask.isCompleted(), category, editingTask.notes(), effort, priority, dependencyIds);
                    manager.updateTask(updatedTask);
                    editingTask = null;
                    addButton.setText("Add Task");
                }
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
                updateDependencyList(dependencyList);
                resetInputFields(titleField, dueField, effortField, priorityCombo, categoryCombo, dependencyList, addTaskPanel);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid effort value: " + ex.getMessage()); // Step 13
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid date format. Use yyyy-MM-dd HH:mm. Details: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage()); // Step 13
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error adding/updating task: " + ex.getMessage()); // Step 13
            }
        });

        sortDueButton.addActionListener(e -> {
            try {
                manager.sortByDueDate();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error sorting by due date: " + ex.getMessage()); // Step 13
            }
        });

        sortEffortButton.addActionListener(e -> {
            try {
                manager.sortByEffort();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error sorting by effort: " + ex.getMessage()); // Step 13
            }
        });

        sortPriorityButton.addActionListener(e -> {
            try {
                manager.sortByPriority();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error sorting by priority: " + ex.getMessage()); // Step 13
            }
        });

        processButton.addActionListener(e -> {
            try {
                processButton.setEnabled(false);
                new Thread(() -> manager.processTasks()).start();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error starting task processing: " + ex.getMessage()); // Step 13
                processButton.setEnabled(true);
            }
        });

        revertButton.addActionListener(e -> {
            try {
                manager.revertTasks();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error reverting tasks: " + ex.getMessage()); // Step 13
            }
        });

        exportButton.addActionListener(e -> {
            try {
                manager.exportTasksToCsv("tasks.csv");
                JOptionPane.showMessageDialog(frame, "Tasks exported to tasks.csv");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage()); // Step 13
            }
        });

        importButton.addActionListener(e -> {
            try {
                manager.importTasksFromCsv("tasks.csv");
                JOptionPane.showMessageDialog(frame, "Tasks imported from tasks.csv");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Import failed: " + ex.getMessage()); // Step 13
            }
        });

        deleteButton.addActionListener(e -> {
            try {
                Task selectedTask = taskList.getSelectedValue();
                if (selectedTask != null && manager.deleteTask(selectedTask.id())) {
                    updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
                    updateDependencyList(dependencyList);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error deleting task: " + ex.getMessage()); // Step 13
            }
        });

        editButton.addActionListener(e -> {
            try {
                Task selectedTask = taskList.getSelectedValue();
                if (selectedTask != null) {
                    editingTask = selectedTask;
                    titleField.setText(selectedTask.title());
                    dueField.setText(selectedTask.dueDate() != null ?
                            selectedTask.dueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                    effortField.setText(selectedTask.effort() != null ? selectedTask.effort().toString() : "");
                    priorityCombo.setSelectedItem(selectedTask.priority());
                    categoryCombo.setSelectedItem(selectedTask.category());
                    addButton.setText("Update Task");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error setting task for edit: " + ex.getMessage()); // Step 13
            }
        });

        reloadButton.addActionListener(e -> {
            try {
                manager.reloadTasks();
                updateTaskDisplay((String) filterCategoryCombo.getSelectedItem());
                updateDependencyList(dependencyList);
                JOptionPane.showMessageDialog(frame, "Tasks reloaded from database");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error reloading tasks: " + ex.getMessage()); // Step 13
            }
        });

        filterCategoryCombo.addActionListener(e -> updateTaskDisplay((String) filterCategoryCombo.getSelectedItem()));

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void resetInputFields(JTextField titleField, JTextField dueField, JTextField effortField,
                                         JComboBox<Task.Priority> priorityCombo, JComboBox<String> categoryCombo,
                                         JList<Task> dependencyList, JPanel addTaskPanel) {
        titleField.setText("");
        dueField.setText("");
        effortField.setText("");
        priorityCombo.setSelectedItem(Task.Priority.MEDIUM);
        categoryCombo.setSelectedItem("");
        dependencyList.clearSelection();
        addButton.setText("Add Task");
        editingTask = null;
        addTaskPanel.revalidate();
        addTaskPanel.repaint();
    }

    private static void updateTaskDisplay(String category) {
        DefaultListModel<Task> model = (DefaultListModel<Task>) taskList.getModel();
        model.clear();
        List<Task> tasksToDisplay = "All Categories".equals(category) || category == null ?
                manager.getAllTasks() : manager.getTasksByCategory(category);
        for (Task task : tasksToDisplay) {
            model.addElement(task);
        }
    }

    private static void updateDependencyList(JList<Task> dependencyList) {
        DefaultListModel<Task> model = (DefaultListModel<Task>) dependencyList.getModel();
        model.clear();
        for (Task task : manager.getAllTasks()) {
            model.addElement(task);
        }
    }

    private static void updateCategoryCombo(JComboBox<String> categoryCombo) {
        String selected = (String) categoryCombo.getSelectedItem();
        categoryCombo.removeAllItems();
        manager.getCategories().forEach(categoryCombo::addItem);
        categoryCombo.setSelectedItem(selected != null && manager.getCategories().contains(selected) ? selected : "");
    }

    private static void updateFilterCategoryCombo(JComboBox<String> filterCategoryCombo) {
        String selected = (String) filterCategoryCombo.getSelectedItem();
        filterCategoryCombo.removeAllItems();
        filterCategoryCombo.addItem("All Categories");
        manager.getCategories().forEach(filterCategoryCombo::addItem);
        filterCategoryCombo.setSelectedItem(selected != null && filterCategoryCombo.getItemCount() > 1 ? selected : "All Categories");
    }
}