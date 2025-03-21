import javax.swing.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TaskManager {
    private final List<Task> tasks;
    private final TaskDatabase db;
    private final TaskProcessor processor;
    private final TaskFileHandler fileHandler;
    private Runnable updateCallback;

    public TaskManager() {
        tasks = new ArrayList<>();
        db = new TaskDatabase();
        processor = new TaskProcessor(tasks, db, new TopoSortStrategy());
        fileHandler = new TaskFileHandler();
        db.loadTasks(tasks);
    }

    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
        processor.setUpdateCallback(callback);
    }

    public void addTask(Task task) {
        if (task == null || task.title() == null || task.title().trim().isEmpty()) { // Step 13: Input validation
            JOptionPane.showMessageDialog(null, "Task title cannot be empty"); // Step 13
            return;
        }

        int newId = db.saveTask(task);
        if (newId == -1) return; // Step 13: Check save failure

        Task taskWithId = new Task(newId, task.title(), task.description(), task.createdAt(),
                task.dueDate(), task.isCompleted(), task.category(), task.notes(), task.effort(), task.priority(), task.dependencies());
        synchronized (this) {
            tasks.add(taskWithId);
            processor.updateGraph(taskWithId); // Step 12.2: Update cached graph
        }
        db.saveDependencies(newId, task.dependencies());
        if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
    }

    public boolean deleteTask(int taskId) {
        synchronized (this) {
            Task task = tasks.stream().filter(t -> t.id() == taskId).findFirst().orElse(null);
            if (task != null && !isDependency(taskId)) {
                tasks.remove(task);
                db.deleteTask(taskId);
                processor.updateGraphAfterDelete(taskId); // Step 12.2: Update graph after deletion
                if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
                return true;
            }
            return false;
        }
    }

    public void updateTask(Task updatedTask) {
        synchronized (this) {
            int index = tasks.indexOf(tasks.stream().filter(t -> t.id() == updatedTask.id()).findFirst().orElse(null));
            if (index != -1) {
                tasks.set(index, updatedTask);
                db.updateTask(updatedTask);
                db.saveDependencies(updatedTask.id(), updatedTask.dependencies());
                processor.updateGraph(updatedTask); // Step 12.2: Update graph on task change
                if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
            }
        }
    }

    public void processTasks() {
        processor.processTasks();
    }

    public void revertTasks() {
        synchronized (this) {
            List<Task> updatedTasks = new ArrayList<>();
            for (Task task : tasks) {
                if (task.isCompleted()) {
                    Task revertedTask = new Task(task.id(), task.title(), task.description(), task.createdAt(),
                            task.dueDate(), false, task.category(), task.notes(), task.effort(), task.priority(), task.dependencies());
                    updatedTasks.add(revertedTask);
                    db.updateTask(revertedTask);
                } else {
                    updatedTasks.add(task);
                }
            }
            tasks.clear();
            tasks.addAll(updatedTasks);
            processor.updateGraphAfterRevert(); // Step 12.2: Rebuild graph after revert
            if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
        }
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getTasksByCategory(String category) {
        return category == null || category.isEmpty() ?
                new ArrayList<>(tasks) :
                tasks.stream().filter(t -> category.equalsIgnoreCase(t.category())).toList();
    }

    public List<Task> getTasksDueBefore(LocalDateTime date) {
        return tasks.stream()
                .filter(t -> t.dueDate() != null && t.dueDate().isBefore(date))
                .collect(Collectors.toList());
    }

    public void sortByDueDate() {
        synchronized (this) {
            tasks.sort(Comparator.comparing(Task::dueDate, Comparator.nullsLast(Comparator.naturalOrder())));
            if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
        }
    }

    public void sortByEffort() {
        synchronized (this) {
            tasks.sort(Comparator.comparing(Task::effort, Comparator.nullsLast(Comparator.naturalOrder())));
            if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
        }
    }

    public void sortByPriority() {
        synchronized (this) {
            tasks.sort(Comparator.comparing(Task::priority, Comparator.reverseOrder()));
            if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
        }
    }

    public void displayTasks() {
        synchronized (this) {
            tasks.forEach(System.out::println);
        }
    }

    public void exportTasksToCsv(String filename) {
        try { // Step 13: Wrap file operation
            fileHandler.exportToCsv(tasks, filename);
            processor.updateGraphAfterRevert();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to export tasks: " + e.getMessage()); // Step 13
        }
    }

    public void importTasksFromCsv(String filename) {
        try { // Step 13: Wrap file operation
            List<Task> importedTasks = fileHandler.importFromCsv(filename);
            integrateImportedTasks(importedTasks);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to import tasks: " + e.getMessage()); // Step 13
        }
    }

    private void integrateImportedTasks(List<Task> importedTasks) {
        synchronized (this) {
            for (Task task : importedTasks) {
                if (task == null) continue; // Step 13: Skip invalid imports
                int newId = db.saveTask(task);
                if (newId == -1) continue; // Step 13: Skip failed saves
                Task taskWithId = new Task(newId, task.title(), task.description(), task.createdAt(),
                        task.dueDate(), task.isCompleted(), task.category(), task.notes(), task.effort(), task.priority(), task.dependencies());
                tasks.add(taskWithId);
                db.saveDependencies(newId, task.dependencies());
                processor.updateGraph(taskWithId); // Step 12.2: Update graph for imported tasks
            }
            if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
        }
    }

    public Set<String> getCategories() {
        return tasks.stream()
                .map(Task::category)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean isDependency(int taskId) {
        return tasks.stream().anyMatch(t -> t.dependencies().contains(taskId));
    }
}