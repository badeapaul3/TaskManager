import java.time.LocalDateTime;
import java.util.*;

/**
 * @author hatzp
 **/
public class TaskManager {

    private final List<Task> tasks;

    public TaskManager(){
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task){
        tasks.add(task);
    }

    public void addTasks(List<Task> newTasks){
        tasks.addAll(newTasks);
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
