package com.evoting.evoting_backend.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MonitoringService {
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> gauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    
    // Timer fields
    private final Timer voteTimer;
    private final Timer kycTimer;
    
    // Track initialized metrics to prevent duplicates
    private final ConcurrentHashMap<String, Boolean> initializedMetrics = new ConcurrentHashMap<>();
    
    @Autowired
    public MonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize timers first
        this.voteTimer = Timer.builder("performance.vote_processing_time")
            .description("Time taken to process a vote")
            .register(meterRegistry);
            
        this.kycTimer = Timer.builder("performance.kyc_processing_time")
            .description("Time taken for KYC verification")
            .register(meterRegistry);
            
        initializeCoreMetrics();
    }
    
    private void initializeCoreMetrics() {
        // ✅ FIXED: Check if metrics are already initialized
        if (initializedMetrics.containsKey("core_metrics")) {
            return; // Already initialized
        }
        
        try {
            // Create gauge values first
            AtomicLong activeElections = new AtomicLong(0);
            AtomicLong totalVotesCast = new AtomicLong(0);
            AtomicLong kycVerifications = new AtomicLong(0);
            AtomicLong tokensIssued = new AtomicLong(0);
            AtomicLong hsmOperational = new AtomicLong(1);
            AtomicLong zkpVerifications = new AtomicLong(0);
            
            // Store them in the gauges map
            gauges.put("election.active_elections", activeElections);
            gauges.put("election.total_votes_cast", totalVotesCast);
            gauges.put("election.kyc_verifications", kycVerifications);
            gauges.put("election.tokens_issued", tokensIssued);
            gauges.put("security.hsm_operational", hsmOperational);
            gauges.put("security.zkp_verifications", zkpVerifications);
            
            // ✅ FIXED: Register gauges only once with try-catch
            try {
                Gauge.builder("election.active_elections", activeElections, AtomicLong::get)
                    .description("Number of active elections")
                    .register(meterRegistry);
            } catch (Exception e) {
                System.err.println("Metric 'election.active_elections' already registered: " + e.getMessage());
            }
            
            try {
                Gauge.builder("election.total_votes_cast", totalVotesCast, AtomicLong::get)
                    .description("Total votes cast across all elections")
                    .register(meterRegistry);
            } catch (Exception e) {
                System.err.println("Metric 'election.total_votes_cast' already registered: " + e.getMessage());
            }
            
            try {
                Gauge.builder("election.kyc_verifications", kycVerifications, AtomicLong::get)
                    .description("Total KYC verifications performed")
                    .register(meterRegistry);
            } catch (Exception e) {
                System.err.println("Metric 'election.kyc_verifications' already registered: " + e.getMessage());
            }
            
            try {
                Gauge.builder("election.tokens_issued", tokensIssued, AtomicLong::get)
                    .description("Total anonymous tokens issued")
                    .register(meterRegistry);
            } catch (Exception e) {
                System.err.println("Metric 'election.tokens_issued' already registered: " + e.getMessage());
            }
            
            try {
                Gauge.builder("security.hsm_operational", hsmOperational, AtomicLong::get)
                    .description("HSM operational status (1=up, 0=down)")
                    .register(meterRegistry);
            } catch (Exception e) {
                System.err.println("Metric 'security.hsm_operational' already registered: " + e.getMessage());
            }
            
            try {
                Gauge.builder("security.zkp_verifications", zkpVerifications, AtomicLong::get)
                    .description("Total ZKP verifications performed")
                    .register(meterRegistry);
            } catch (Exception e) {
                System.err.println("Metric 'security.zkp_verifications' already registered: " + e.getMessage());
            }
            
            // Mark as initialized
            initializedMetrics.put("core_metrics", true);
            System.out.println("✅ Core metrics initialized successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing core metrics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Timer getter methods
    public Timer getVoteTimer() {
        return voteTimer;
    }
    
    public Timer getKycTimer() {
        return kycTimer;
    }
    
    // ✅ FIXED: Safe counter increment with duplicate protection
    public void incrementCounter(String metricName) {
        incrementCounter(metricName, 1);
    }
    
    public void incrementCounter(String metricName, double amount) {
        try {
            Counter counter = counters.computeIfAbsent(metricName, 
                name -> {
                    try {
                        return Counter.builder(name).register(meterRegistry);
                    } catch (Exception e) {
                        System.err.println("Counter '" + name + "' already exists: " + e.getMessage());
                        // Return existing counter if possible
                        return meterRegistry.counter(name);
                    }
                });
            counter.increment(amount);
        } catch (Exception e) {
            System.err.println("Error incrementing counter '" + metricName + "': " + e.getMessage());
        }
    }
    
    // ✅ FIXED: Safe gauge setting with duplicate protection
    public void setGauge(String metricName, long value) {
        try {
            AtomicLong gauge = gauges.computeIfAbsent(metricName,
                name -> {
                    try {
                        AtomicLong g = new AtomicLong(0);
                        Gauge.builder(metricName, g, AtomicLong::get)
                            .description("Custom gauge for " + metricName)
                            .register(meterRegistry);
                        return g;
                    } catch (Exception e) {
                        System.err.println("Gauge '" + metricName + "' already registered: " + e.getMessage());
                        return new AtomicLong(value); // Return with initial value
                    }
                });
            gauge.set(value);
        } catch (Exception e) {
            System.err.println("Error setting gauge '" + metricName + "': " + e.getMessage());
        }
    }
    
    public void recordTimer(String metricName, long duration, TimeUnit unit) {
        try {
            Timer timer = timers.computeIfAbsent(metricName,
                name -> Timer.builder(name).register(meterRegistry));
            timer.record(duration, unit);
        } catch (Exception e) {
            System.err.println("Error recording timer '" + metricName + "': " + e.getMessage());
        }
    }
    
    // Election-specific monitoring methods
    public void recordVoteCast(Long electionId, long processingTimeMs) {
        incrementCounter("election.votes.cast");
        incrementCounter("election.votes.cast.election_" + electionId);
        setGauge("election.last_vote_timestamp", System.currentTimeMillis());
        recordTimer("performance.vote_processing_time", processingTimeMs, TimeUnit.MILLISECONDS);
        
        // Update the total votes cast gauge
        AtomicLong totalVotes = gauges.get("election.total_votes_cast");
        if (totalVotes != null) {
            totalVotes.incrementAndGet();
        }
    }
    
    public void recordKYCVerification(boolean success, long processingTimeMs) {
        if (success) {
            incrementCounter("election.kyc.successful");
            // Update the KYC verifications gauge
            AtomicLong kycVerifications = gauges.get("election.kyc_verifications");
            if (kycVerifications != null) {
                kycVerifications.incrementAndGet();
            }
        } else {
            incrementCounter("election.kyc.failed");
        }
        recordTimer("performance.kyc_processing_time", processingTimeMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordTokenIssuance() {
        incrementCounter("election.tokens.issued");
        // Update the tokens issued gauge
        AtomicLong tokensIssued = gauges.get("election.tokens_issued");
        if (tokensIssued != null) {
            tokensIssued.incrementAndGet();
        }
    }
    
    public void recordZKPVerification(boolean success) {
        if (success) {
            incrementCounter("election.zkp.successful");
            // Update the ZKP verifications gauge
            AtomicLong zkpVerifications = gauges.get("security.zkp_verifications");
            if (zkpVerifications != null) {
                zkpVerifications.incrementAndGet();
            }
        } else {
            incrementCounter("election.zkp.failed");
        }
    }
    
    public void recordHSMStatus(boolean operational) {
        setGauge("security.hsm_operational", operational ? 1 : 0);
    }
    
    public void recordSecurityEvent(String eventType) {
        incrementCounter("security.events." + eventType);
    }
    
    // Performance monitoring
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void stopTimer(Timer.Sample sample, String timerName) {
        try {
            sample.stop(Timer.builder(timerName).register(meterRegistry));
        } catch (Exception e) {
            System.err.println("Error stopping timer '" + timerName + "': " + e.getMessage());
        }
    }
    
    // Health check metrics
    public void recordDatabaseHealth(boolean healthy) {
        setGauge("health.database", healthy ? 1 : 0);
    }
    
    public void recordDeathRegistryHealth(boolean healthy) {
        setGauge("health.death_registry", healthy ? 1 : 0);
    }
    
    public void recordZKPServiceHealth(boolean healthy) {
        setGauge("health.zkp_service", healthy ? 1 : 0);
    }
    
    // Get current metrics snapshot
    public java.util.Map<String, Object> getMetricsSnapshot() {
        java.util.Map<String, Object> snapshot = new java.util.HashMap<>();
        
        // Add custom metrics
        gauges.forEach((name, gauge) -> snapshot.put(name, gauge.get()));
        
        // Add counter values
        java.util.Map<String, Double> counterValues = new java.util.HashMap<>();
        counters.forEach((name, counter) -> counterValues.put(name, counter.count()));
        snapshot.put("counters", counterValues);
        
        snapshot.put("timestamp", System.currentTimeMillis());
        
        return snapshot;
    }
    
    // ✅ NEW: Method to check if metric exists
    public boolean isMetricRegistered(String metricName) {
        try {
            meterRegistry.get(metricName).counter();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ✅ NEW: Method to clear all metrics (for testing)
    public void clearAllMetrics() {
        counters.clear();
        gauges.clear();
        timers.clear();
        initializedMetrics.clear();
        System.out.println("✅ All metrics cleared");
    }
}