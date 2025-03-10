
import javax.swing.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hatzp
 **/
public class TaskManager {

    private final List<Task> tasks;

    private static final String DB_URL = "jdbc:sqlite:taskmanager.sqlite";
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Runnable updateCallback; // Callback to refresh UI

    public TaskManager(){
        this.tasks = new ArrayList<>();
        loadTasksFromDatabase();
    }


    public void setUpdateCallback(Runnable callback){
        this.updateCallback = callback;
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

                String priorityStr = rs.getString("priority");
                Task.Priority priority = priorityStr != null ? Task.Priority.valueOf(priorityStr) : Task.Priority.MEDIUM;

                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        createdAt,
                        dueDate,
                        rs.getInt("is_completed") == 1,
                        rs.getString("category"),
                        rs.getString("notes"),
                        effort,
                        priority
                ));
            }
        }catch (SQLException e){
            System.err.println("Database load error: " + e.getMessage());
        }
    }


    public synchronized void addTask(Task task){
        int newId = saveTaskToDatabase(task);
        Task taskWithId = new Task(newId, task.title(), task.description(), task.createdAt(),
                task.dueDate(), task.isCompleted(), task.category(), task.notes(), task.effort(), task.priority());
        tasks.add(taskWithId);
    }

    private int saveTaskToDatabase(Task task){
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "insert into tasks (title, description, created_at, due_date, is_completed, category, notes, effort, priority) "
                     + "values (?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS
             )){
            pstmt.setString(1,task.title());
            pstmt.setString(2, task.description());
            pstmt.setString(3, task.createdAt() != null ? task.createdAt().format(DB_FORMATTER) : null);
            pstmt.setString(4, task.dueDate() != null ? task.dueDate().format(DB_FORMATTER) : null);
            pstmt.setInt(5, task.isCompleted() ? 1 : 0);
            pstmt.setString(6, task.category());
            pstmt.setString(7, task.notes());
            pstmt.setString(8, task.effort() !=null ? task.effort().toString() : null);
            pstmt.setString(9, task.priority().name());

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

    public void processTasks(){
        List<Task> tasksToProcess = new ArrayList<>(tasks);
        if(tasksToProcess.isEmpty()) return;

        AtomicInteger activeThreads = new AtomicInteger(tasksToProcess.size());

        for(Task task : new ArrayList<>(tasks)) { //Copy to avoid concurrentModificationException
            Thread thread = new Thread(
                () -> {
                    synchronized (this){
                        if(!task.isCompleted()){
                            System.out.println("Processing " + task.title() + " (Effort: "+ task.effort() + "h) on " + Thread.currentThread().getName());
                            try{
                                Thread.sleep(task.effort().multiply(BigDecimal.valueOf(1000)).longValue());
                                Task completedTask = task.markCompleted();
                                tasks.set(tasks.indexOf(task), completedTask);
                                updateTaskInDatabase(completedTask);
                                System.out.println("Completed " + task.title());
                                //update UI after each task
                                if(updateCallback != null){
                                    SwingUtilities.invokeLater(updateCallback);
                                }
                            }catch (InterruptedException e){
                                Thread.currentThread().interrupt();
                                System.err.println("Thread interrupted: " + e.getMessage());
                            }
                        }
                    }
                    if(activeThreads.decrementAndGet() == 0 && updateCallback != null){
                        SwingUtilities.invokeLater(updateCallback); // Final update
                    }
                }
            );
            thread.start();
        }
    }

    // New method to revert tasks to incomplete
    public void revertTasks() {
        synchronized (this) { // Thread-safe
            List<Task> updatedTasks = new ArrayList<>();
            for (Task task : tasks) {
                if (task.isCompleted()) {
                    Task revertedTask = new Task(task.id(), task.title(), task.description(), task.createdAt(),
                            task.dueDate(), false, task.category(), task.notes(), task.effort(), task.priority());
                    updatedTasks.add(revertedTask);
                    updateTaskInDatabase(revertedTask);
                    System.out.println("Reverted " + task.title() + " to incomplete");
                } else {
                    updatedTasks.add(task); // Keep unchanged
                }
            }
            tasks.clear();
            tasks.addAll(updatedTasks); // Replace list atomically
        }
    }

    protected void updateTaskInDatabase(Task task){
        try(Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement pstmt = conn.prepareStatement("update tasks set is_completed = ? where id = ?")
        ){
            pstmt.setInt(1, task.isCompleted() ? 1 : 0);
            pstmt.setInt(2, task.id());
            pstmt.executeUpdate();

        }catch (SQLException e){
            System.err.println("Database update error: " + e.getMessage());
        }
    }

    public List<Task> getTasksByCategory(String category) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (category == null || task.category() != null && task.category().equalsIgnoreCase(category)) {
                result.add(task);
            }
        }
        return result;
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

    public void sortByPriority(){
        tasks.sort(Comparator.comparing(Task::priority,Comparator.reverseOrder()));
    }

    public List<Task> getAllTasks(){
        return tasks;
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
