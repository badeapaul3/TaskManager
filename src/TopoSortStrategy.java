import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

// Step 12.2: Topological sort with priority and due date
public class TopoSortStrategy implements TaskProcessingStrategy {
    @Override
    public void processTasks(List<Task> tasks, Map<Integer, Set<Integer>> dependencyGraph,
                             Consumer<List<Task>> batchProcessor, Runnable updateCallback) {
        if (tasks.isEmpty()) {
            if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
            return;
        }
        if (hasCycle(dependencyGraph)) {
            System.out.println("Cannot process tasks: Dependency cycle detected.");
            JOptionPane.showMessageDialog(null, "Dependency cycle detected. Please resolve circular dependencies.");
            return;
        }
        List<Task> orderedTasks = topologicalSort(tasks, dependencyGraph);
        batchProcessor.accept(orderedTasks);
    }

    private boolean hasCycle(Map<Integer, Set<Integer>> graph) {
        Set<Integer> visited = new HashSet<>();
        Set<Integer> recursionStack = new HashSet<>();
        for (Integer taskId : graph.keySet()) {
            if (hasCycleDFS(taskId, graph, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleDFS(Integer taskId, Map<Integer, Set<Integer>> graph,
                                Set<Integer> visited, Set<Integer> recursionStack) {
        if (recursionStack.contains(taskId)) return true;
        if (visited.contains(taskId)) return false;
        visited.add(taskId);
        recursionStack.add(taskId);
        for (Integer depId : graph.getOrDefault(taskId, Collections.emptySet())) {
            if (hasCycleDFS(depId, graph, visited, recursionStack)) {
                return true;
            }
        }
        recursionStack.remove(taskId);
        return false;
    }

    private List<Task> topologicalSort(List<Task> tasks, Map<Integer, Set<Integer>> graph) {
        Set<Integer> visited = new HashSet<>();
        LinkedList<Task> sortedTasks = new LinkedList<>();
        PriorityQueue<Task> taskQueue = new PriorityQueue<>(
                Comparator.comparing(Task::priority, Comparator.reverseOrder())
                        .thenComparing(Task::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
        );
        taskQueue.addAll(tasks);

        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            if (!visited.contains(task.id()) && canProcessTask(task.id(), graph, visited)) {
                visited.add(task.id());
                sortedTasks.addFirst(task);
                PriorityQueue<Task> tempQueue = new PriorityQueue<>(taskQueue.comparator());
                tempQueue.addAll(taskQueue);
                taskQueue = tempQueue;
            }
        }
        return sortedTasks;
    }

    private boolean canProcessTask(Integer taskId, Map<Integer, Set<Integer>> graph, Set<Integer> visited) {
        for (Integer depId : graph.getOrDefault(taskId, Collections.emptySet())) {
            if (!visited.contains(depId)) {
                return false;
            }
        }
        return true;
    }
}