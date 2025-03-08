import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author hatzp
 **/
public class Main {
    public static void main(String[] args) {

        Task task1 = Task.createTask("Learn Java","Study Java SE 21", null, LocalDateTime.of(2025,3,25,15,0),
                 false, "Education", "Records Lesson", BigDecimal.valueOf(5.5));

        Task task2 = Task.createTask("Write Code","Add features", null, LocalDateTime.of(2025,3,23,13,0),
                false, "Work", "Focus on indexing", BigDecimal.valueOf(3.75));

        System.out.println(task1);
        System.out.println(task2);

        System.out.println("Due Date comparison of task 1 to task 2: " + task1.compareTo(task2));//positive if task 1 is later/higher
        System.out.println("\n@@@@@@@@@@@@@@@@@@@@@@@@@@ PART 2 @@@@@@@@@@@@@@@@@@@@@@@@\n");

        TaskManager manager = new TaskManager();

        manager.addTask(task1);
        manager.addTask(task2);
        manager.addTask(Task.createTask("Debug", "Fix bugs", null, null, false,
                "Work", "", new BigDecimal("2.0") ));

        System.out.println("Unsorted:");
        manager.displayTasks();

        System.out.println("Sorted by Due Date:");
        manager.sortByDueDate();
        manager.displayTasks();

        System.out.println("Sorter by Effort:");
        manager.sortByEffort();
        manager.displayTasks();

        System.out.println("Tasks due before March 24th");
        List<Task> dueSoon = manager.getTasksDueBefore(LocalDateTime.of(2025, 3, 24, 0,0));
        dueSoon.forEach(System.out::println);

    }
}
