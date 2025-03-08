import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.math.BigDecimal;

/**
 * @author hatzp
 **/
public class TaskManager {

    private final List<Task> tasks;

    private static final String DB_URL = "jdbc:sqlite:taskmanager.sqlite";
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TaskManager(){
        this.tasks = new ArrayList<>();
        loadTasksFromDatabase();
    }

    private void loadTasksFromDatabase(){
        try(Connection conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM tasks"))
        {
            tasks.clear();
            while (rs.next()){
                LocalDateTime createdAt = rs.getString("created_at") != null ?
                        LocalDateTime.parse(rs.getString("created_at"),DB_FORMATTER) : null;

                LocalDateTime dueDate = rs.getString("due_date") != null ?
                        LocalDateTime.parse(rs.getString("due_date"),DB_FORMATTER) : null;

                BigDecimal effort = rs.getString("effort") != null ?
                        new BigDecimal(rs.getString("effort")) : null;

                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        createdAt,
                        dueDate,
                        rs.getInt("is_completed") == 1,
                        rs.getString("category"),
                        rs.getString("notes"),
                        effort
                ));
            }
        }catch (SQLException e){
            System.err.println("Database load error: " + e.getMessage());
        }
    }


    public void addTask(Task task){
        int newId = saveTaskToDatabase(task);
        Task taskWithId = new Task(newId, task.title(), task.description(), task.createdAt(),
                task.dueDate(), task.isCompleted(), task.category(), task.notes(), task.effort());
        tasks.add(task);
    }

    private int saveTaskToDatabase(Task task){
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "insert into tasks (title, description, created_at, due_date, is_completed, category, notes, effort) "
                     + "values (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS
             )){
            pstmt.setString(1,task.title());
            pstmt.setString(2, task.description());
            pstmt.setString(3, task.createdAt() != null ? task.createdAt().format(DB_FORMATTER) : null);
            pstmt.setString(4, task.dueDate() != null ? task.dueDate().format(DB_FORMATTER) : null);
            pstmt.setInt(5, task.isCompleted() ? 1 : 0);
            pstmt.setString(6, task.category());
            pstmt.setString(7, task.notes());
            pstmt.setString(8, task.effort() !=null ? task.effort().toString() : null);

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()){
                if(rs.next()) return rs.getInt(1);
            }
        }catch (SQLException e){
            System.err.println("Database insert error: " + e.getMessage());
        }
        return -1; //fallback
    }

    public void addTasks(List<Task> newTasks){
        for(Task task : newTasks){
            addTask(task);
        }
    }

    public List<Task> getTasksDueBefore(LocalDateTime deadline){
        List<Task> result = new ArrayList<>();
        for(Task task : tasks){
            if (task.dueDate() != null &&
                    task.dueDate().isBefore(deadline) &&
                    !task.isCompleted())
                result.add(task);
        }
        return result;
    }

    public void sortByDueDate(){
        tasks.sort(null); //natural ordering from Comparable
    }

    public void sortByEffort(){
        tasks.sort(Comparator.comparing(Task::effort, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public void displayTasks(){
        if (tasks.isEmpty()){
            System.out.println("No tasks available.");
        }else {
            for (Task task : tasks){
                System.out.println(task);
            }
            System.out.println("\n");
        }
    }




}
