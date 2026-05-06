package Models;
import java.util.HashMap;
import java.util.Map;
import Simulation.EngagementSimulator;

public class Schedule {
    // Mapping: Post -> TimeSlot
    private Map<Post, TimeSlot> assignment;
    private double fitness; // engagement score

    public Schedule() {
        assignment = new HashMap<>();
        fitness = 0;
    }

    public void assign(Post post, TimeSlot slot) {
        assignment.put(post, slot);
    }

    public Map<Post, TimeSlot> getAssignments() {
        return assignment;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getFitness() {
        return fitness;
    }

    public void calculateFitness() {
        double total = 0;
        Map<TimeSlot, Integer> slotCounts = new HashMap<>();

        for (Post post : assignment.keySet()) {
            TimeSlot slot = assignment.get(post);
            total += EngagementSimulator.getEngagement(
                    post.getType(),
                    slot
            );
            slotCounts.put(slot, slotCounts.getOrDefault(slot, 0) + 1);
        }

        // Apply penalty for overloaded slots
        for (int count : slotCounts.values()) {
            if (count > 1) {
                total -= 10 * (count - 1);
            }
        }

        this.fitness = total;
    }

    public void printSchedule() {
        System.out.println("Schedule:");
        for (Post post : assignment.keySet()) {
            System.out.println(post + " -> " + assignment.get(post));
        }
        System.out.println("Fitness: " + fitness);
    }

    public String toStringSchedule() {
        StringBuilder sb = new StringBuilder();
        sb.append("Schedule:\n");
        for (Post post : assignment.keySet()) {
            sb.append(post).append(" -> ").append(assignment.get(post)).append("\n");
        }
        sb.append("Fitness: ").append(fitness);
        return sb.toString();
    }
}