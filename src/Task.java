import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author hatzp
 **/
public record Task(
       int id,
       String title,
       String description,
       LocalDateTime createdAt,
       LocalDateTime dueDate,
       boolean isCompleted,
       String category,
       String notes,
       BigDecimal effort // in hours
) implements Comparable<Task>{
    private static int idCounter = 0;

    //factory method manages unique IDs and also prepares for thread safety
    public static synchronized Task createTask(
            String title,
            String description,
            LocalDateTime createdAt,
            LocalDateTime dueDate,
            boolean isCompleted,
            String category,
            String notes,
            BigDecimal effort
    ){
        if(title == null || title.isBlank()){
            throw new IllegalArgumentException("Title is null or blank");
        }
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
        if(effort == null || effort.compareTo(BigDecimal.ZERO)<0){
            effort = BigDecimal.ZERO;
        }

        @Deprecated
        int newId = generateUniqueId();

        return new Task(-1,title, description, createdAt, dueDate, isCompleted, category, notes, effort);
    }

    @Deprecated
    private static synchronized int generateUniqueId(){
        return ++idCounter;
    }


    //records must respect public canonical ctor
    public Task{
    }

    //Mark as completed
    public Task markCompleted(){
        return new Task(id, title, description, createdAt, dueDate, true, category, notes, effort);
    }

    public String getFormattedDueDate(){
        return dueDate != null ? dueDate.format(DateTimeFormatter.ISO_DATE_TIME) : "No due date";
    }

    //Natural ordring by due date (Task is Comparable)
    public int compareTo(Task other){
        if(this.dueDate == null && other.dueDate == null) return 0;
        if(this.dueDate == null) return 1; //nulls last
        if(other.dueDate == null) return -1;
        return this.dueDate.compareTo(other.dueDate);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", completed=" + isCompleted +
                ", due=" + getFormattedDueDate() +
                ", category='" + category + '\'' +
                ", effort=" + effort.stripTrailingZeros().toPlainString() + "h" +
                '}';
    }
}
