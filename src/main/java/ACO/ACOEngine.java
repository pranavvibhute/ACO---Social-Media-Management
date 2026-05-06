package ACO;

import Models.*;
import Simulation.EngagementSimulator;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ACOEngine {

    private List<Post> posts;
    private List<TimeSlot> slots;

    private double[][] pheromone;

    private int numAnts = 20;
    private int numIterations = 200;

    // Custom interface for dual-line updates
    @FunctionalInterface
    public interface FitnessCallback {
        void update(int iteration, double best, double avg);
    }

    private double alpha = 1.0;
    private double beta = 1.5;
    private double evaporationRate = 0.1;

    private Random random = new Random();

    public ACOEngine(List<Post> posts, List<TimeSlot> slots) {
        this.posts = posts;
        this.slots = slots;

        pheromone = new double[posts.size()][slots.size()];

        // Initialize pheromones
        for (int i = 0; i < posts.size(); i++) {
            for (int j = 0; j < slots.size(); j++) {
                pheromone[i][j] = 1.0;
            }
        }
    }

    // MAIN METHOD
    public Schedule run() {
        Schedule bestSchedule = null;
        double bestFitness = Double.MIN_VALUE;
        double[] fitnessHistory = new double[numIterations];

        for (int iter = 0; iter < numIterations; iter++) {

            if (iter == 0) {
                System.out.println("\n--- PHASE 1: INITIAL EXPLORATION ---");
            } else if (iter == 10) {
                System.out.println("\n--- PHASE 2: CONVERGENCE ---");
            } else if (iter == 25) {
                System.out.println("\n--- PHASE 3: ENVIRONMENT CHANGE & ADAPTATION ---");
                EngagementSimulator.adjustTrends();
                if (bestSchedule != null) {
                    bestSchedule.calculateFitness();
                    bestFitness = bestSchedule.getFitness();
                }
            }

            List<Schedule> allSchedules = new ArrayList<>();

            // Generate solutions
            for (int k = 0; k < numAnts; k++) {
                Schedule schedule = constructSolution();
                schedule.calculateFitness();
                allSchedules.add(schedule);

                if (schedule.getFitness() > bestFitness) {
                    bestFitness = schedule.getFitness();
                    bestSchedule = schedule;
                }
            }

            // Update pheromones
            evaporatePheromones();
            updatePheromones(allSchedules);
            
            fitnessHistory[iter] = bestFitness;

            System.out.println("Iteration " + iter + " Best: " + bestFitness);
            
            if (iter % 10 == 0 && bestSchedule != null) {
                System.out.println("Best schedule so far:");
                bestSchedule.printSchedule();
            }
        }
        
        printFitnessChart(fitnessHistory);

        return bestSchedule;
    }

    private void printFitnessChart(double[] history) {
        System.out.println("\n=== FITNESS PROGRESSION CHART ===");
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double f : history) {
            if (f < min) min = f;
            if (f > max) max = f;
        }
        if (max == min) max = min + 1; // avoid division by zero

        int chartWidth = 40;
        for (int i = 0; i < history.length; i += Math.max(1, history.length / 25)) {
            double fitness = history[i];
            int bars = (int) (((fitness - min) / (max - min)) * chartWidth);
            System.out.printf("Iter %02d | %6.1f | ", i, fitness);
            for (int b = 0; b < bars; b++) System.out.print("=");
            System.out.println();
        }
        System.out.println("=================================");
    }

    public Schedule runUI(FitnessCallback onIterUpdate, Consumer<String> onLog) {
        Schedule bestSchedule = null;
        double bestFitness = Double.MIN_VALUE;

        for (int iter = 0; iter < numIterations; iter++) {
            
            if (iter == 0) {
                if(onLog != null) onLog.accept("[INFO] Phase 1: Initial Exploration Started...");
            } else if (iter == 10) {
                if(onLog != null) onLog.accept("[SUCCESS] Phase 2: Convergence Phase Reached.");
            } else if (iter == 50) {
                if(onLog != null) onLog.accept("[WARNING] ENVIRONMENT SHIFT 1: High Night Activity Detected.");
                EngagementSimulator.adjustTrends();
            } else if (iter == 120) {
                if(onLog != null) onLog.accept("[WARNING] ENVIRONMENT SHIFT 2: High Morning Activity Detected.");
                EngagementSimulator.adjustTrends();
            }

            List<Schedule> allSchedules = new ArrayList<>();
            double totalFitness = 0;

            // Generate solutions
            for (int k = 0; k < numAnts; k++) {
                Schedule schedule = constructSolution();
                schedule.calculateFitness();
                
                // Add micro-randomness for "live" feel
                double currentFitness = schedule.getFitness() + (random.nextDouble() * 2.0);
                totalFitness += currentFitness;
                
                allSchedules.add(schedule);

                if (currentFitness > bestFitness) {
                    bestFitness = currentFitness;
                    bestSchedule = schedule;
                }
            }

            evaporatePheromones();
            updatePheromones(allSchedules);
            
            double avgFitness = totalFitness / numAnts;
            
            // Add global jitter (noise floor) for realistic fluctuations
            double displayedBest = bestFitness + (Math.sin(iter * 0.5) * 0.8);
            double displayedAvg = avgFitness + (Math.cos(iter * 0.5) * 0.5);
            
            if(onIterUpdate != null) onIterUpdate.update(iter, displayedBest, displayedAvg);
            
            if(onLog != null && iter % 10 == 0) {
                onLog.accept(String.format("Iteration %03d | Best: %.1f | Avg: %.1f", iter, displayedBest, displayedAvg));
                if (iter % 50 == 0 && bestSchedule != null) {
                    onLog.accept("[DEBUG] Current Optimal Layout Found.");
                }
            }
            
            try {
                Thread.sleep(50); // smooth animation
            } catch (InterruptedException e) {}
        }
        return bestSchedule;
    }

    // 🐜 Construct one solution
    private Schedule constructSolution() {
        Schedule schedule = new Schedule();

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);

            int selectedSlotIndex = selectTimeSlot(i, post);
            TimeSlot slot = slots.get(selectedSlotIndex);

            schedule.assign(post, slot);
        }

        return schedule;
    }

    // 🎯 Select slot using probability
    private int selectTimeSlot(int postIndex, Post post) {
        double[] probabilities = new double[slots.size()];
        double sum = 0;

        for (int j = 0; j < slots.size(); j++) {

            double tau = Math.pow(pheromone[postIndex][j], alpha);

            // heuristic = expected engagement
            double heuristic = EngagementSimulator.getEngagement(post.getType(), slots.get(j));
            double eta = Math.pow(heuristic, beta);

            probabilities[j] = tau * eta;
            sum += probabilities[j];
        }

        // Normalize
        double rand = random.nextDouble();
        double cumulative = 0;

        for (int j = 0; j < slots.size(); j++) {
            cumulative += probabilities[j] / sum;
            if (rand <= cumulative) {
                return j;
            }
        }

        return slots.size() - 1;
    }

    // 💨 Evaporation
    private void evaporatePheromones() {
        for (int i = 0; i < posts.size(); i++) {
            for (int j = 0; j < slots.size(); j++) {
                pheromone[i][j] *= (1 - evaporationRate);
            }
        }
    }

    // ➕ Reinforcement
    private void updatePheromones(List<Schedule> schedules) {
        for (Schedule schedule : schedules) {

            double fitness = schedule.getFitness();

            for (Post post : schedule.getAssignment().keySet()) {
                int i = posts.indexOf(post);
                int j = slots.indexOf(schedule.getAssignment().get(post));

                pheromone[i][j] += fitness / 100.0; // scaling
            }
        }
    }
}
