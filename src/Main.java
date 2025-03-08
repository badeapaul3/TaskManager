import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author hatzp
 **/
public class Main {
    public static void main(String[] args) throws InterruptedException {

        TaskManager manager = new TaskManager();

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

        manager.processTasks();

        Thread.sleep(6500);

        System.out.println("After processing");
        manager.displayTasks();

    }
}
