import javax.swing.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskProcessor {
    private final List<Task> tasks;
    private final TaskDatabase db;
    private final TaskProcessingStrategy strategy;
    private Map<Integer, Set<Integer>> dependencyGraph; // Step 12.2: Cached graph
    private Runnable updateCallback;

    public TaskProcessor(List<Task> tasks, TaskDatabase db, TaskProcessingStrategy strategy) {
        this.tasks = tasks;
        this.db = db;
        this.strategy = strategy;
        this.dependencyGraph = buildDependencyGraph(); // Step 12.2: Pre-compute
    }

    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
    }

    public void processTasks() {
        strategy.processTasks(tasks, dependencyGraph, this::processTaskBatch, updateCallback);
    }

    // Step 12.2: Build and cache dependency graph
    private Map<Integer, Set<Integer>> buildDependencyGraph() {
        Map<Integer, Set<Integer>> graph = new HashMap<>();
        for (Task task : tasks) {
            graph.putIfAbsent(task.id(), new HashSet<>(task.dependencies()));
            for (int depId : task.dependencies()) {
                graph.putIfAbsent(depId, new HashSet<>());
            }
        }
        return graph;
    }

    // Step 12.2: Update graph on task addition/update
    public void updateGraph(Task task) {
        dependencyGraph.put(task.id(), new HashSet<>(task.dependencies()));
        for (int depId : task.dependencies()) {
            dependencyGraph.putIfAbsent(depId, new HashSet<>());
        }
    }

    // Step 12.2: Update graph on task deletion
    public void updateGraphAfterDelete(int taskId) {
        dependencyGraph.remove(taskId);
        for (Set<Integer> deps : dependencyGraph.values()) {
            deps.remove(taskId);
        }
    }

    // Step 12.2: Update graph after revert
    public void updateGraphAfterRevert() {
        dependencyGraph = buildDependencyGraph();
    }

    void processTaskBatch(List<Task> orderedTasks) {
        AtomicInteger activeThreads = new AtomicInteger(orderedTasks.size());
        for (Task task : orderedTasks) {
            Thread thread = new Thread(() -> processSingleTask(task, activeThreads));
            thread.start();
        }
    }

    private void processSingleTask(Task task, AtomicInteger activeThreads) {
        synchronized (this) {
            if (!task.isCompleted() && areDependenciesCompleted(task)) {
                System.out.println("Processing " + task.title() + " (Effort: " + task.effort() + "h)");
                try {
                    Thread.sleep(task.effort().multiply(BigDecimal.valueOf(1000)).longValue());
                    Task completedTask = task.markCompleted();
                    tasks.set(tasks.indexOf(task), completedTask);
                    db.updateTask(completedTask);
                    System.out.println("Completed " + task.title());
                    if (updateCallback != null) {
                        SwingUtilities.invokeLater(updateCallback);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread interrupted: " + e.getMessage());
                }
            }
        }
        int remaining = activeThreads.decrementAndGet();
        if (remaining == 0 && updateCallback != null) {
            SwingUtilities.invokeLater(updateCallback);
        }
    }

    private boolean areDependenciesCompleted(Task task) {
        for (int depId : task.dependencies()) {
            Task dep = tasks.stream().filter(t -> t.id() == depId).findFirst().orElse(null);
            if (dep == null || !dep.isCompleted()) {
                return false;
            }
        }
        return true;
    }
}