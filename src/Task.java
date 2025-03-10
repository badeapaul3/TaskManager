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
       BigDecimal effort, // in hours
       Priority priority
) implements Comparable<Task>{

    public enum Priority{
        HIGH, MEDIUM, LOW;
        @Override
        public String toString(){
            return name().substring(0,1) + name().substring(1).toLowerCase();
        }
    }

    //factory method manages unique IDs and also prepares for thread safety
    public static synchronized Task createTask(
            String title,
            String description,
            LocalDateTime createdAt,
            LocalDateTime dueDate,
            boolean isCompleted,
            String category,
            String notes,
            BigDecimal effort,
            Priority priority
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
        if(priority == null){
            priority = Priority.MEDIUM;
        }


        return new Task(-1,title, description, createdAt, dueDate, isCompleted, category, notes, effort, priority);
    }


    //records must respect public canonical ctor
    public Task{
    }

    //Mark as completed
    public Task markCompleted(){
        return new Task(id, title, description, createdAt, dueDate, true, category, notes, effort, priority);
    }

    public String getFormattedDueDate(){
        return dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "No due date";
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
                ", priority=" + priority +
                '}';
    }
}
