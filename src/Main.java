import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    }
}
