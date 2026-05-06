import Models.*;
import ACO.ACOEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    public static void main(String[] args) {

        // Create Posts
        List<Post> posts = new ArrayList<>();
        posts.add(new Post(1, ContentType.REEL));
        posts.add(new Post(2, ContentType.MEME));
        posts.add(new Post(3, ContentType.EDUCATIONAL));

        // Create Time Slots
        List<TimeSlot> slots = new ArrayList<>();
        slots.add(new TimeSlot(1, "Morning (9 AM)"));
        slots.add(new TimeSlot(2, "Afternoon (1 PM)"));
        slots.add(new TimeSlot(3, "Evening (6 PM)"));
        slots.add(new TimeSlot(4, "Night (9 PM)"));

        // Baseline: Random Scheduling
        System.out.println("\n--- BASELINE (RANDOM SCHEDULING) ---");
        Schedule randomSchedule = new Schedule();
        Random rand = new Random();
        for (Post post : posts) {
            randomSchedule.assign(post, slots.get(rand.nextInt(slots.size())));
        }
        
        // Calculate and Print Baseline Fitness
        randomSchedule.calculateFitness();
        randomSchedule.printSchedule();

        System.out.println("\n--- Running ACO ---");
        ACOEngine engine = new ACOEngine(posts, slots, 10, 1.0, 2.0, 100);
        Schedule best = engine.run();

        System.out.println("\nFinal Best Schedule:");
        best.printSchedule();
    }
}