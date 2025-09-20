package com.example.jobdispatcher.enums;

/**
 * Enum representing job priority levels.
 * Lower numeric values indicate higher priority.
 */
public enum JobPriority {
    
    CRITICAL(1, "Critical priority jobs that must be executed immediately"),
    HIGH(2, "High priority jobs with elevated execution priority"),
    NORMAL(3, "Normal priority jobs with standard execution priority"),
    LOW(4, "Low priority jobs with reduced execution priority"),
    BACKGROUND(5, "Background jobs with lowest execution priority");
    
    private final int level;
    private final String description;
    
    JobPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this priority is higher than the given priority.
     */
    public boolean isHigherThan(JobPriority other) {
        return this.level < other.level;
    }
    
    /**
     * Check if this priority is lower than the given priority.
     */
    public boolean isLowerThan(JobPriority other) {
        return this.level > other.level;
    }
    
    /**
     * Get priority by level.
     */
    public static JobPriority fromLevel(int level) {
        for (JobPriority priority : values()) {
            if (priority.level == level) {
                return priority;
            }
        }
        return NORMAL; // Default fallback
    }
    
    /**
     * Get priority by name (case insensitive).
     */
    public static JobPriority fromName(String name) {
        if (name == null) {
            return NORMAL;
        }
        
        try {
            return JobPriority.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL; // Default fallback
        }
    }
    
    @Override
    public String toString() {
        return name() + "(" + level + ")";
    }
}

