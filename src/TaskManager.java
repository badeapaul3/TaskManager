
import javax.swing.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the collection of tasks, including CRUD operations, processing, and sorting.
 * Delegates file I/O to TaskFileHandler to keep responsibilities separate.
 */
public class TaskManager {

    private final List<Task> tasks;

    private static final String DB_URL = "jdbc:sqlite:taskmanager.sqlite";
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Runnable updateCallback; // Callback to refresh UI

    private final TaskFileHandler fileHandler;

    /**
     * Constructor: Initializes task list and file handler, loads tasks from database.
     */
    public TaskManager(){
        this.tasks = new ArrayList<>();
        this.fileHandler = new TaskFileHandler();
        initializeDatabase();
        loadTasksFromDatabase();
    }

    private void initializeDatabase(){
        try(Connection conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement()){
            // Tasks table (unchanged except ensuring it exists)
            stmt.execute("CREATE table if not exists tasks (" +
                                "id integer primary key autoincrement, " +
                                "title text not null, " +
                                "description text, " +
                                "created_at text, " +
                                "due_date text, " +
                                "is_completed integer, " +
                                "category text, " +
                                "notes text, " +
                                "effort text, " +
                                "priority text)");
            // Dependencies table: maps task_id to dependency_id
            stmt.execute("CREATE TABLE IF NOT EXISTS task_dependencies (" +
                    "task_id INTEGER, " +
                    "dependency_id INTEGER, " +
                    "FOREIGN KEY(task_id) REFERENCES tasks(id), " +
                    "FOREIGN KEY(dependency_id) REFERENCES tasks(id))");
        }catch (SQLException e){
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }



    /**
     * Sets a callback to notify the UI when tasks change.
     * @param callback Runnable to execute (typically updates UI)
     * Thought: Enables loose coupling with UI—TaskManager doesn’t know about Swing.
     */
    public void setUpdateCallback(Runnable callback){
        this.updateCallback = callback;
    }


    /**
     * Loads tasks and dependencies from the database into the tasks list.
     * Orchestrates the loading process by delegating to helper methods.
     */
    private void loadTasksFromDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Map<Integer, List<Integer>> dependencyMap = loadDependencies(conn);
            loadTasks(conn, dependencyMap);
        } catch (SQLException e) {
            System.err.println("Database load error: " + e.getMessage());
        }
    }


    /**
     * Loads task dependencies from the task_dependencies table into a map.
     * @param conn Database connection
     * @return Map of task ID to list of dependency IDs
     */
    private Map<Integer, List<Integer>> loadDependencies(Connection conn) throws SQLException {
        Map<Integer, List<Integer>> dependencyMap = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT task_id, dependency_id FROM task_dependencies")) {
            while (rs.next()) {
                int taskId = rs.getInt("task_id");
                int depId = rs.getInt("dependency_id");
                // Add dependency ID to the task's list, creating list if absent
                dependencyMap.computeIfAbsent(taskId, k -> new ArrayList<>()).add(depId);
            }
        }
        return dependencyMap;
    }

    /**
     * Loads tasks from the tasks table, linking them with their dependencies.
     * @param conn Database connection
     * @param dependencyMap Map of task ID to dependency IDs
     */
    private void loadTasks(Connection conn, Map<Integer, List<Integer>> dependencyMap) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks")) {
            tasks.clear(); // Reset list before loading
            while (rs.next()) {
                LocalDateTime createdAt = rs.getString("created_at") != null ?
                        LocalDateTime.parse(rs.getString("created_at"), DB_FORMATTER) : null;
                LocalDateTime dueDate = rs.getString("due_date") != null ?
                        LocalDateTime.parse(rs.getString("due_date"), DB_FORMATTER) : null;
                BigDecimal effort = rs.getString("effort") != null ?
                        new BigDecimal(rs.getString("effort")) : null;
                String priorityStr = rs.getString("priority");
                Task.Priority priority = priorityStr != null ? Task.Priority.valueOf(priorityStr) : Task.Priority.MEDIUM;
                List<Integer> dependencies = dependencyMap.getOrDefault(rs.getInt("id"), Collections.emptyList());
                tasks.add(new Task(
                        rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                        createdAt, dueDate, rs.getInt("is_completed") == 1,
                        rs.getString("category"), rs.getString("notes"), effort, priority, dependencies
                ));
            }
        }
    }

    /**
     * Adds a new task to the list and database.
     * @param task Task to add (ID is -1 initially)
     * Thought: Synchronized to prevent concurrent modifications; assigns new ID from DB.
     */
    public synchronized void addTask(Task task){
        int newId = saveTaskToDatabase(task);
        Task taskWithId = new Task(newId, task.title(), task.description(), task.createdAt(),
                task.dueDate(), task.isCompleted(), task.category(), task.notes(), task.effort(), task.priority(), task.dependencies());
        tasks.add(taskWithId);
        saveDependenciesToDatabase(newId, task.dependencies());
        if(updateCallback != null) SwingUtilities.invokeLater(updateCallback);
    }

    /**
     * Saves a task to the database and returns its generated ID.
     * @param task Task to save
     * @return Generated ID, or -1 if failed
     * Thought: Uses PreparedStatement for safety and efficiency; returns ID for in-memory sync.
     */
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
                if(rs.next()) return rs.getInt(1); // Return new ID
            }
        }catch (SQLException e){
            System.err.println("Database insert error: " + e.getMessage());
        }
        return -1; // Fallback ID if insert fails
    }

    private void saveDependenciesToDatabase(int taskId, List<Integer> dependencies){
        try(Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO task_dependencies (task_id, dependency_id) VALUES (?,?)")){
            for(int depId : dependencies){
                pstmt.setInt(1, taskId);
                pstmt.setInt(2, depId);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }catch (SQLException e){
            System.err.println("Database dependency insert error: " + e.getMessage());
        }
    }

    /**
     * Processes all incomplete tasks, respecting dependencies, in separate threads.
     */
    public void processTasks() {
        List<Task> tasksToProcess = new ArrayList<>(tasks);
        if (tasksToProcess.isEmpty()) {
            if (updateCallback != null) SwingUtilities.invokeLater(updateCallback);
            return;
        }
        processTaskBatch(tasksToProcess);
    }

    /**
     * Launches threads to process a batch of tasks.
     */
    private void processTaskBatch(List<Task> tasksToProcess) {
        AtomicInteger activeThreads = new AtomicInteger(tasksToProcess.size());
        for (Task task : tasksToProcess) {
            Thread thread = new Thread(() -> processSingleTask(task, activeThreads));
            thread.start();
        }
    }

    /**
     * Processes a single task if its dependencies are met.
     */
    private void processSingleTask(Task task, AtomicInteger activeThreads) {
        synchronized (this) {
            if (!task.isCompleted() && areDependenciesCompleted(task)) {
                System.out.println("Processing " + task.title() + " (Effort: " + task.effort() + "h)");
                try {
                    Thread.sleep(task.effort().multiply(BigDecimal.valueOf(1000)).longValue());
                    Task completedTask = task.markCompleted();
                    tasks.set(tasks.indexOf(task), completedTask);
                    updateTaskInDatabase(completedTask);
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

    private boolean areDependenciesCompleted(Task task){
        for (int depId : task.dependencies()){
            Task dep = tasks.stream().filter(t -> t.id() == depId).findFirst().orElse(null);
            if(dep == null || !dep.isCompleted()){
                System.out.println("Cannot process " + task.title() + "!\nDependency " +
                depId + " not completed");
                return false;
            }
        }
        return true;
    }

    /**
     * Reverts all completed tasks to incomplete.
     * Thought: Synchronized to ensure atomic update; rebuilds list to avoid concurrent issues.
     */
    public void revertTasks() {
        synchronized (this) { // Thread-safe
            List<Task> updatedTasks = new ArrayList<>();
            for (Task task : tasks) {
                if (task.isCompleted()) {
                    Task revertedTask = new Task(task.id(), task.title(), task.description(), task.createdAt(),
                            task.dueDate(), false, task.category(), task.notes(), task.effort(), task.priority(), task.dependencies());
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

    /**
     * Updates a task’s completion status in the database.
     * @param task Task to update
     * Thought: Minimal update to avoid overwriting other fields; used by processTasks and revertTasks.
     */
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
    /**
     * Gets tasks by category (null returns all tasks).
     * @param category Category to filter by
     * @return List of matching tasks
     * Thought: Flexible filter for UI display; null case simplifies general use.
     */
    public List<Task> getTasksByCategory(String category) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (category == null || task.category() != null && task.category().equalsIgnoreCase(category)) {
                result.add(task);
            }
        }
        return result;
    }
    /**
     * Gets tasks due before a deadline that aren’t completed.
     * @param deadline Cutoff date/time
     * @return List of matching tasks
     * Thought: Simple filter for future features (e.g., overdue alerts).
     */
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

    /**
     * Sorts tasks by due date using Task’s natural ordering.
     */
    public void sortByDueDate(){
        tasks.sort(null); //natural ordering from Comparable
    }

    /**
     * Sorts tasks by effort, with nulls last.
     */
    public void sortByEffort(){
        tasks.sort(Comparator.comparing(Task::effort, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * Sorts tasks by priority (High > Medium > Low).
     */
    public void sortByPriority(){
        tasks.sort(Comparator.comparing(Task::priority,Comparator.reverseOrder()));
    }

    public List<Task> getAllTasks(){
        return tasks;
    }

    /**
     * Displays tasks to console (for debugging).
     */
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

    /**
     * Exports tasks to a CSV file using TaskFileHandler.
     * @param filePath Path to export to
     * Thought: Delegates to fileHandler for SRP; synchronized to protect task list.
     */
    public synchronized void exportTasksToCsv(String filePath){
        fileHandler.exportToCsv(tasks,filePath);
    }

    public synchronized void importTasksFromCsv(String filePath){
        List<Task> importedTasks = fileHandler.importFromCsv(filePath); // Get tasks from file

        for(Task imported : importedTasks){
            tasks.removeIf(t -> t.id() == imported.id()); // Replace duplicates by ID
            tasks.add(imported);
            saveTaskToDatabase(imported); // Ensure DB consistency
        }
        if(updateCallback !=null){
            SwingUtilities.invokeLater(updateCallback); //Refresh UI
        }


    }







}
