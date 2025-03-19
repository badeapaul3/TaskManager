import java.util.*;
import java.util.function.Consumer;


public interface TaskProcessingStrategy {
    void processTasks(List<Task> tasks, Map<Integer, Set<Integer>> dependencyGraph, Consumer<List<Task>> batchProcessor, Runnable updateCallback);
}
