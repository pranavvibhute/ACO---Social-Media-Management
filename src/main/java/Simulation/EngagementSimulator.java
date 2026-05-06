package Simulation;
import Models.*;

import java.util.Random;

public class EngagementSimulator {

    private static Random random = new Random();
    private static int currentTrend = 0; // 0: Normal, 1: Night-focused, 2: Morning-focused

    public static void adjustTrends() {
        currentTrend = (currentTrend + 1) % 3;
    }

    public static void resetTrends() {
        currentTrend = 0;
    }

    // Base score depending on content type
    public static int baseScore(ContentType type) {
        switch (type) {
            case REEL:
                return 60;
            case MEME:
                return 50;
            case EDUCATIONAL:
                return 40;
            default:
                return 0;
        }
    }

    // Time slot effect
    public static int timeWeight(TimeSlot slot, ContentType type) {
        String time = slot.getName().toLowerCase();

        // Content-time relationship
        if (type == ContentType.REEL) {
            if (currentTrend == 0) {
                if (time.contains("night")) return 25;
                if (time.contains("evening")) return 20;
            } else if (currentTrend == 1) {
                if (time.contains("evening")) return 30;
                if (time.contains("night")) return 15;
            } else if (currentTrend == 2) {
                if (time.contains("morning")) return 30;
                if (time.contains("afternoon")) return 10;
            }
        }

        if (type == ContentType.MEME) {
            if (time.contains("evening")) return 35;
            if (time.contains("afternoon")) return 25;
        }

        if (type == ContentType.EDUCATIONAL) {
            if (time.contains("morning")) return 25;
            if (time.contains("afternoon")) return 15;
        }

        return 10; // default low engagement
    }

    // Random noise (VERY IMPORTANT)
    public static int randomNoise() {
        return random.nextInt(60); // 0 to 59
    }

    // Final engagement calculation
    public static int getEngagement(ContentType type, TimeSlot slot) {
        int score = baseScore(type);
        score += timeWeight(slot, type);
        score += randomNoise();

        return score;
    }
}