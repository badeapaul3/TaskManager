import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Handles file I/O operations for tasks, specifically CSV export and import.
 * Separated from TaskManager to follow Single Responsibility Principle (SRP)—TaskManager manages tasks,
 * this class manages file persistence.
 */

public class TaskFileHandler {

    // Formatter for date/time fields in CSV, matches database format for consistency
    private static final DateTimeFormatter CSV_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Exports a list of tasks to a CSV file.
     * @param tasks List of tasks to export
     * @param filePath Path to the CSV file (e.g., "tasks.csv")
     * Thought: We use CSV for simplicity and readability; BufferedWriter ensures efficient writing.
     * Throws RuntimeException to signal failure to callers (e.g., UI) without complex error handling here.
     */
    public void exportToCsv(List<Task> tasks, String filePath){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))){
            // Write header row to define CSV structure—matches Task record fields
            writer.write("id,title,description,created_at,due_date,is_completed,category,notes,effort,priority,dependencies\n");

            // Iterate through tasks and convert each to a CSV row
            for(Task task : tasks){
                String dependenciesStr = String.join(";",task.dependencies().stream().map(String::valueOf).toList());
                // Use String.format for structured output; escape quotes and commas to handle special characters
                String line = String.format("%d, \"%s\",\"%s\",%s,%s,%d,\"%s\",\"%s\",%s,%s,\"%s\"",
                        task.id(),
                        escapeCsv(task.title()), // Title, quoted to handle commas, same for others escaped
                        escapeCsv(task.description()),
                        task.createdAt() != null ? "\"" + task.createdAt().format(CSV_FORMATTER) + "\"" : "",
                        task.dueDate() != null ? "\"" + task.dueDate().format(CSV_FORMATTER) + "\"" : "",
                        task.isCompleted() ? 1:0,
                        escapeCsv(task.category()),
                        escapeCsv(task.notes()),
                        task.effort() != null ? task.effort().toString() : "",
                        task.priority().name(),
                        escapeCsv(dependenciesStr)
                );
                writer.write(line + "\n");
            }
            System.out.println("Tasks exported to " + filePath);
        }catch (IOException e){
            System.err.println("Error exporting to CSV: " + e.getMessage());
            throw new RuntimeException("Failed to export tasks", e);
        }
    }

    /**
     * Imports tasks from a CSV file and returns them as a list.
     * @param filePath Path to the CSV file (e.g., "tasks.csv")
     * @return List of imported Task objects
     * Thought: Reads CSV line-by-line, skips header, and parses fields into Task objects.
     * Returns partial results if parsing fails mid-file to avoid total loss of data.
     */
    public List<Task> importFromCsv(String filePath) {
        List<Task> importedTasks = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            String line;
            boolean firstLine = true; // skip header
            while ((line = reader.readLine())!=null){
                if(firstLine){
                    firstLine = false; // skip header row
                    continue;
                }
                String[] fields = parseCsvLine(line); // Custom parser for CSV fields

                if (fields.length != 11) continue; // Skip malformed rows

                // Parse each field into appropriate type; handle nulls or empty strings
                int id = Integer.parseInt(fields[0]); // ID as integer
                String title = fields[1]; // Title, unescaped by parser
                String description = fields[2];
                LocalDateTime createdAt = fields[3].isEmpty() ? null : LocalDateTime.parse(fields[3],CSV_FORMATTER);
                LocalDateTime dueDate = fields[4].isEmpty() ? null : LocalDateTime.parse(fields[4],CSV_FORMATTER);
                boolean isCompleted = Integer.parseInt(fields[5]) == 1; // Boolean from 0/1 values;
                String category = fields[6];
                String notes = fields[7];
                BigDecimal effort = fields[8].isEmpty() ? null : new BigDecimal(fields[8]);
                Task.Priority priority = Task.Priority.valueOf(fields[9]); // Priority from enum name
                List<Integer> dependencies = fields[10].isEmpty() ? Collections.emptyList() : Arrays.stream(fields[10].split(";")).map(Integer::parseInt).toList();

                Task task = new Task(id, title, description, createdAt, dueDate, isCompleted,
                        category, notes, effort, priority, dependencies);

                importedTasks.add(task);
            }
            System.out.println("Imported " + importedTasks.size() + " tasks from " + filePath);
            return  importedTasks;
        }catch (IOException e){
            System.err.println("Error importing from CSV: " + e.getMessage());
            throw new RuntimeException("Failed to import tasks", e);
        }catch (Exception e){
            System.err.println("Error parsing CSV: " + e.getMessage());
            // Return what we’ve parsed so far instead of failing completely
            return importedTasks;
        }
    }




    /**
     * Escapes a string for CSV by adding quotes if it contains commas or quotes.
     * @param value String to escape
     * @return Escaped string
     * Thought: Prevents CSV parsing errors by quoting fields with special characters.
     */
    private String escapeCsv(String value){
        if(value == null) return "";
        return value.contains(",") || value.contains("\"") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }


    /**
     * Parses a CSV line into an array of fields, handling quoted values.
     * @param line CSV line to parse
     * @return Array of field values
     * Thought: Simple parser for our needs—handles quotes but assumes well-formed input.
     * Could be replaced with a library (e.g., OpenCSV) for robustness later.
     */
    private String[] parseCsvLine(String line){
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        for(int i = 0; i < line.length(); i++){
            char c = line.charAt(i);
            if(c == '"' && !inQuotes){
                inQuotes = true; //Start of quoted field
            } else if (c == '"' && inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"'){
                field.append('"'); // Escaped quote within field
                i++; //Skip next quote
            } else if (c == '"' && inQuotes){
                inQuotes = false; // End of quoted field
            } else if (c == ',' && !inQuotes){
                fields.add(field.toString()); // Field complete
                field = new StringBuilder();
            } else {
                field.append(c); // Add character to current field
            }
        }
        fields.add(field.toString()); // Add last field
        return fields.toArray(new String[0]);
    }
}
