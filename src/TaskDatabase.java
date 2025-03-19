import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TaskDatabase {
    private static final String DB_URL = "jdbc:sqlite:tasks.db";
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TaskDatabase() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "created_at TEXT, " +
                    "due_date TEXT, " +
                    "is_completed INTEGER, " +
                    "category TEXT, " +
                    "notes TEXT, " +
                    "effort TEXT, " +
                    "priority TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS task_dependencies (" +
                    "task_id INTEGER, " +
                    "dependency_id INTEGER, " +
                    "FOREIGN KEY(task_id) REFERENCES tasks(id), " +
                    "FOREIGN KEY(dependency_id) REFERENCES tasks(id))");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    public void loadTasks(List<Task> tasks) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Map<Integer, List<Integer>> dependencyMap = loadDependencies(conn);
            loadTasks(conn, dependencyMap, tasks);
        } catch (SQLException e) {
            System.err.println("Database load error: " + e.getMessage());
        }
    }

    private Map<Integer, List<Integer>> loadDependencies(Connection conn) throws SQLException {
        Map<Integer, List<Integer>> dependencyMap = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT task_id, dependency_id FROM task_dependencies")) {
            while (rs.next()) {
                int taskId = rs.getInt("task_id");
                int depId = rs.getInt("dependency_id");
                dependencyMap.computeIfAbsent(taskId, k -> new ArrayList<>()).add(depId);
            }
        }
        return dependencyMap;
    }

    private void loadTasks(Connection conn, Map<Integer, List<Integer>> dependencyMap, List<Task> tasks) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks")) {
            tasks.clear();
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

    public int saveTask(Task task) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO tasks (title, description, created_at, due_date, is_completed, category, notes, effort, priority) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, task.title());
            pstmt.setString(2, task.description());
            pstmt.setString(3, task.createdAt() != null ? task.createdAt().format(DB_FORMATTER) : null);
            pstmt.setString(4, task.dueDate() != null ? task.dueDate().format(DB_FORMATTER) : null);
            pstmt.setInt(5, task.isCompleted() ? 1 : 0);
            pstmt.setString(6, task.category());
            pstmt.setString(7, task.notes());
            pstmt.setString(8, task.effort() != null ? task.effort().toString() : null);
            pstmt.setString(9, task.priority().name());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Database insert error: " + e.getMessage());
        }
        return -1;
    }

    public void saveDependencies(int taskId, List<Integer> dependencies) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO task_dependencies (task_id, dependency_id) VALUES (?, ?)")) {
            for (int depId : dependencies) {
                pstmt.setInt(1, taskId);
                pstmt.setInt(2, depId);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("Database dependency insert error: " + e.getMessage());
        }
    }

    public void deleteTask(int taskId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM task_dependencies WHERE task_id = ?")) {
                pstmt.setInt(1, taskId);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
                pstmt.setInt(1, taskId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Database delete error: " + e.getMessage());
        }
    }

    public void updateTask(Task task) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE tasks SET title = ?, description = ?, created_at = ?, due_date = ?, is_completed = ?, " +
                             "category = ?, notes = ?, effort = ?, priority = ? WHERE id = ?")) {
            pstmt.setString(1, task.title());
            pstmt.setString(2, task.description());
            pstmt.setString(3, task.createdAt() != null ? task.createdAt().format(DB_FORMATTER) : null);
            pstmt.setString(4, task.dueDate() != null ? task.dueDate().format(DB_FORMATTER) : null);
            pstmt.setInt(5, task.isCompleted() ? 1 : 0);
            pstmt.setString(6, task.category());
            pstmt.setString(7, task.notes());
            pstmt.setString(8, task.effort() != null ? task.effort().toString() : null);
            pstmt.setString(9, task.priority().name());
            pstmt.setInt(10, task.id());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database update error: " + e.getMessage());
        }
    }
}