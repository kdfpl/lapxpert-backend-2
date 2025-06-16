package com.lapxpert.backend.shipping.domain.service;

import com.lapxpert.backend.ghn.domain.service.GHNService;
import com.lapxpert.backend.shipping.domain.config.ShippingComparisonConfig;
import com.lapxpert.backend.shipping.domain.dto.ProviderComparisonResult;
import com.lapxpert.backend.shipping.domain.dto.ShippingFeeResponse;
import com.lapxpert.backend.shipping.domain.dto.ShippingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Shipping provider comparison service
 * Evaluates multiple shipping providers and selects the best option
 * based on cost, reliability, and delivery time
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingProviderComparator {
    
    private final GHTKShippingService ghtkService;
    private final GHNService ghnService;
    private final ShippingComparisonConfig comparisonConfig;
    private final ProviderPerformanceTracker performanceTracker;
    
    /**
     * Compare all available shipping providers and select the best option
     * Uses caching to avoid repeated API calls for the same request
     */
    @Cacheable(value = "providerComparisons", key = "#request.hashCode()")
    public ProviderComparisonResult compareProviders(ShippingRequest request) {
        return compareProviders(request, comparisonConfig.getDefaultCriteria());
    }
    
    /**
     * Compare providers with custom criteria
     */
    public ProviderComparisonResult compareProviders(ShippingRequest request, 
                                                   ProviderComparisonResult.ComparisonCriteria criteria) {
        log.info("Comparing shipping providers for request: {} -> {}", 
                request.getPickProvince(), request.getProvince());
        
        List<ProviderComparisonResult.ProviderResult> providerResults = new ArrayList<>();
        
        // Call all providers concurrently for better performance
        CompletableFuture<ProviderComparisonResult.ProviderResult> ghtkFuture = 
            CompletableFuture.supplyAsync(() -> evaluateProvider(ghtkService, request, criteria));
        
        CompletableFuture<ProviderComparisonResult.ProviderResult> ghnFuture = 
            CompletableFuture.supplyAsync(() -> evaluateProvider(ghnService, request, criteria));
        
        try {
            // Wait for all providers to respond (with timeout)
            ProviderComparisonResult.ProviderResult ghtkResult = 
                ghtkFuture.get(criteria.getMaxResponseTimeMs(), TimeUnit.MILLISECONDS);
            ProviderComparisonResult.ProviderResult ghnResult = 
                ghnFuture.get(criteria.getMaxResponseTimeMs(), TimeUnit.MILLISECONDS);
            
            providerResults.add(ghtkResult);
            providerResults.add(ghnResult);
            
        } catch (Exception e) {
            log.error("Error during provider comparison: {}", e.getMessage(), e);
            
            // Add any completed results
            if (ghtkFuture.isDone() && !ghtkFuture.isCompletedExceptionally()) {
                try {
                    providerResults.add(ghtkFuture.get());
                } catch (Exception ignored) {}
            }
            
            if (ghnFuture.isDone() && !ghnFuture.isCompletedExceptionally()) {
                try {
                    providerResults.add(ghnFuture.get());
                } catch (Exception ignored) {}
            }
        }
        
        // Select the best provider based on results
        return selectBestProvider(providerResults, criteria);
    }
    
    /**
     * Evaluate a single provider
     */
    private ProviderComparisonResult.ProviderResult evaluateProvider(ShippingCalculatorService provider, 
                                                                    ShippingRequest request,
                                                                    ProviderComparisonResult.ComparisonCriteria criteria) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Check if provider is available
            if (!provider.isAvailable()) {
                return ProviderComparisonResult.ProviderResult.builder()
                    .providerName(provider.getProviderName())
                    .isAvailable(false)
                    .isSuccessful(false)
                    .failureReason("Provider not available or not configured")
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .totalScore(BigDecimal.ZERO)
                    .build();
            }
            
            // Calculate shipping fee
            ShippingFeeResponse response = provider.calculateShippingFee(request);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (!response.isSuccess()) {
                // Record failure for performance tracking
                performanceTracker.recordFailure(provider.getProviderName(), responseTime, response.getErrorMessage());

                return ProviderComparisonResult.ProviderResult.builder()
                    .providerName(provider.getProviderName())
                    .response(response)
                    .isAvailable(true)
                    .isSuccessful(false)
                    .failureReason(response.getErrorMessage())
                    .responseTimeMs(responseTime)
                    .totalScore(BigDecimal.ZERO)
                    .build();
            }
            
            // Calculate scores
            BigDecimal costScore = calculateCostScore(response.getTotalFee(), criteria);
            BigDecimal reliabilityScore = calculateReliabilityScore(provider.getProviderName(), responseTime, criteria);
            BigDecimal speedScore = calculateSpeedScore(provider.getProviderName());
            
            // Calculate total weighted score
            BigDecimal totalScore = costScore.multiply(criteria.getCostWeight())
                .add(reliabilityScore.multiply(criteria.getReliabilityWeight()))
                .add(speedScore.multiply(criteria.getSpeedWeight()));

            // Record success for performance tracking
            performanceTracker.recordSuccess(provider.getProviderName(), responseTime, response.getTotalFee());

            return ProviderComparisonResult.ProviderResult.builder()
                .providerName(provider.getProviderName())
                .response(response)
                .isAvailable(true)
                .isSuccessful(true)
                .responseTimeMs(responseTime)
                .costScore(costScore)
                .reliabilityScore(reliabilityScore)
                .speedScore(speedScore)
                .totalScore(totalScore)
                .build();
            
        } catch (Exception e) {
            log.error("Error evaluating provider {}: {}", provider.getProviderName(), e.getMessage(), e);

            long responseTime = System.currentTimeMillis() - startTime;
            // Record failure for performance tracking
            performanceTracker.recordFailure(provider.getProviderName(), responseTime, "Exception: " + e.getMessage());

            return ProviderComparisonResult.ProviderResult.builder()
                .providerName(provider.getProviderName())
                .isAvailable(true)
                .isSuccessful(false)
                .failureReason("Provider evaluation failed: " + e.getMessage())
                .responseTimeMs(responseTime)
                .totalScore(BigDecimal.ZERO)
                .build();
        }
    }

    /**
     * Select the best provider from comparison results
     */
    private ProviderComparisonResult selectBestProvider(List<ProviderComparisonResult.ProviderResult> results,
                                                       ProviderComparisonResult.ComparisonCriteria criteria) {

        // Filter successful results
        List<ProviderComparisonResult.ProviderResult> successfulResults = results.stream()
            .filter(ProviderComparisonResult.ProviderResult::isSuccessful)
            .filter(result -> result.getResponse().getTotalFee().compareTo(criteria.getMaxAcceptableFee()) <= 0)
            .filter(result -> result.getReliabilityScore().compareTo(criteria.getMinReliabilityScore()) >= 0)
            .toList();

        if (successfulResults.isEmpty()) {
            log.warn("No successful provider results found");
            return ProviderComparisonResult.failed("No providers returned valid shipping fees");
        }

        // Sort by total score (highest first)
        successfulResults.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));

        // Check for preferred provider first
        ProviderComparisonResult.ProviderResult preferredResult = successfulResults.stream()
            .filter(result -> criteria.getPreferredProvider().equals(result.getProviderName()))
            .findFirst()
            .orElse(null);

        ProviderComparisonResult.ProviderResult selectedResult;
        String selectionReason;

        if (preferredResult != null) {
            // Use preferred provider if available and reasonable
            ProviderComparisonResult.ProviderResult bestResult = successfulResults.get(0);
            BigDecimal priceDifference = preferredResult.getResponse().getTotalFee()
                .subtract(bestResult.getResponse().getTotalFee());

            // Use preferred provider if price difference is less than 20% or 50k VND
            BigDecimal maxDifference = bestResult.getResponse().getTotalFee()
                .multiply(new BigDecimal("0.2"))
                .max(new BigDecimal("50000"));

            if (priceDifference.compareTo(maxDifference) <= 0) {
                selectedResult = preferredResult;
                selectionReason = String.format("Selected preferred provider %s (price difference: %s VND)",
                    preferredResult.getProviderName(), priceDifference);
            } else {
                selectedResult = bestResult;
                selectionReason = String.format("Selected best scoring provider %s (score: %s)",
                    bestResult.getProviderName(), bestResult.getTotalScore());
            }
        } else {
            // Use best scoring provider
            selectedResult = successfulResults.get(0);
            selectionReason = String.format("Selected best scoring provider %s (score: %s)",
                selectedResult.getProviderName(), selectedResult.getTotalScore());
        }

        log.info("Provider comparison completed. Selected: {} - {}",
            selectedResult.getProviderName(), selectionReason);

        return ProviderComparisonResult.success(
            selectedResult.getResponse(),
            results,
            selectionReason,
            criteria
        );
    }

    /**
     * Calculate cost score (lower cost = higher score)
     */
    private BigDecimal calculateCostScore(BigDecimal totalFee, ProviderComparisonResult.ComparisonCriteria criteria) {
        if (totalFee == null || totalFee.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Normalize cost score: 1.0 for fees <= 50k, decreasing for higher fees
        BigDecimal baseFee = new BigDecimal("50000"); // 50k VND baseline
        BigDecimal maxFee = criteria.getMaxAcceptableFee();

        if (totalFee.compareTo(baseFee) <= 0) {
            return BigDecimal.ONE;
        }

        if (totalFee.compareTo(maxFee) >= 0) {
            return new BigDecimal("0.1"); // Minimum score for max acceptable fee
        }

        // Linear interpolation between base and max fee
        BigDecimal feeRange = maxFee.subtract(baseFee);
        BigDecimal feeAboveBase = totalFee.subtract(baseFee);
        BigDecimal scoreReduction = feeAboveBase.divide(feeRange, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("0.9")); // 0.9 score reduction range

        return BigDecimal.ONE.subtract(scoreReduction);
    }

    /**
     * Calculate reliability score based on provider history and response time
     */
    private BigDecimal calculateReliabilityScore(String providerName, long responseTimeMs,
                                                ProviderComparisonResult.ComparisonCriteria criteria) {
        // Get historical reliability from performance tracker
        BigDecimal historicalReliability = performanceTracker.getReliabilityScore(providerName);

        // Adjust for current response time
        BigDecimal responseTimePenalty = BigDecimal.ZERO;
        if (responseTimeMs > 5000) { // 5 seconds threshold
            responseTimePenalty = new BigDecimal(responseTimeMs - 5000)
                .divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP) // 10 second range
                .multiply(new BigDecimal("0.2")); // Max 20% penalty
        }

        return historicalReliability.subtract(responseTimePenalty).max(new BigDecimal("0.1"));
    }

    /**
     * Calculate speed score based on estimated delivery time
     */
    private BigDecimal calculateSpeedScore(String providerName) {
        // Estimated delivery speed scores (could be dynamic based on route)
        return switch (providerName) {
            case "GHN" -> new BigDecimal("0.90"); // Generally faster
            case "GHTK" -> new BigDecimal("0.85"); // Slightly slower
            default -> new BigDecimal("0.70"); // Default speed
        };
    }
}
