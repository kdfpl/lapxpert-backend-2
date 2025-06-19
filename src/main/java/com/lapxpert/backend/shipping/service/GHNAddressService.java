package com.lapxpert.backend.shipping.service;

import com.lapxpert.backend.shipping.config.GHNConfig;
import com.lapxpert.backend.shipping.dto.GHNDistrictResponse;
import com.lapxpert.backend.shipping.dto.GHNProvinceResponse;
import com.lapxpert.backend.shipping.dto.GHNWardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GHN Address Mapping Service
 * Converts between Vietnamese text-based addresses and GHN numeric IDs
 * Uses in-memory caching to avoid repeated API calls
 */
@Slf4j
@Service
public class GHNAddressService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    // In-memory caches for address data
    private final Map<String, GHNProvinceResponse.GHNProvince> provinceNameToProvince = new ConcurrentHashMap<>();
    private final Map<Integer, GHNProvinceResponse.GHNProvince> provinceIdToProvince = new ConcurrentHashMap<>();
    
    private final Map<String, GHNDistrictResponse.GHNDistrict> districtKeyToDistrict = new ConcurrentHashMap<>();
    private final Map<Integer, GHNDistrictResponse.GHNDistrict> districtIdToDistrict = new ConcurrentHashMap<>();
    
    private final Map<String, GHNWardResponse.GHNWard> wardKeyToWard = new ConcurrentHashMap<>();
    private final Map<String, GHNWardResponse.GHNWard> wardCodeToWard = new ConcurrentHashMap<>();
    
    // Cache status flags
    private volatile boolean provincesLoaded = false;
    private volatile boolean isLoadingProvinces = false;
    
    /**
     * Initialize service by loading provinces on startup
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing GHN Address Service");
        // Configuration validation is now handled by GHNConfig @PostConstruct
        // We'll check if configuration is valid before loading provinces
        try {
            // Try to access a config value to see if it's available
            if (GHNConfig.ghn_ApiToken != null && !GHNConfig.ghn_ApiToken.trim().isEmpty()) {
                loadProvinces();
            } else {
                log.warn("GHN configuration is not yet available, address mapping will be loaded on demand");
            }
        } catch (Exception e) {
            log.warn("GHN configuration check failed, address mapping will be loaded on demand: {}", e.getMessage());
        }
    }
    
    /**
     * Find province ID by Vietnamese province name
     * @param provinceName Vietnamese province name (e.g., "Hà Nội", "TP. Hồ Chí Minh")
     * @return Province ID or null if not found
     */
    public Integer findProvinceId(String provinceName) {
        if (provinceName == null || provinceName.trim().isEmpty()) {
            return null;
        }
        
        ensureProvincesLoaded();
        
        String normalizedName = normalizeVietnameseName(provinceName);
        GHNProvinceResponse.GHNProvince province = provinceNameToProvince.get(normalizedName);
        
        if (province != null) {
            log.debug("Found province ID {} for name '{}'", province.getProvinceId(), provinceName);
            return province.getProvinceId();
        }
        
        log.warn("Province not found for name: '{}'", provinceName);
        return null;
    }
    
    /**
     * Find district ID by Vietnamese district name and province ID
     * @param districtName Vietnamese district name
     * @param provinceId Province ID from findProvinceId
     * @return District ID or null if not found
     */
    public Integer findDistrictId(String districtName, Integer provinceId) {
        if (districtName == null || districtName.trim().isEmpty() || provinceId == null) {
            return null;
        }
        
        // Load districts for this province if not already loaded
        loadDistrictsForProvince(provinceId);
        
        String key = createDistrictKey(districtName, provinceId);
        GHNDistrictResponse.GHNDistrict district = districtKeyToDistrict.get(key);
        
        if (district != null) {
            log.debug("Found district ID {} for name '{}' in province {}", district.getDistrictId(), districtName, provinceId);
            return district.getDistrictId();
        }
        
        log.warn("District not found for name: '{}' in province {}", districtName, provinceId);
        return null;
    }
    
    /**
     * Find ward code by Vietnamese ward name and district ID
     * @param wardName Vietnamese ward name
     * @param districtId District ID from findDistrictId
     * @return Ward code or null if not found
     */
    public String findWardCode(String wardName, Integer districtId) {
        if (wardName == null || wardName.trim().isEmpty() || districtId == null) {
            return null;
        }
        
        // Load wards for this district if not already loaded
        loadWardsForDistrict(districtId);
        
        String key = createWardKey(wardName, districtId);
        GHNWardResponse.GHNWard ward = wardKeyToWard.get(key);
        
        if (ward != null) {
            log.debug("Found ward code {} for name '{}' in district {}", ward.getWardCode(), wardName, districtId);
            return ward.getWardCode();
        }
        
        log.warn("Ward not found for name: '{}' in district {}", wardName, districtId);
        return null;
    }
    
    /**
     * Reverse mapping: Get province name by ID
     */
    public String getProvinceName(Integer provinceId) {
        if (provinceId == null) {
            return null;
        }
        
        ensureProvincesLoaded();
        GHNProvinceResponse.GHNProvince province = provinceIdToProvince.get(provinceId);
        return province != null ? province.getProvinceName() : null;
    }
    
    /**
     * Reverse mapping: Get district name by ID
     */
    public String getDistrictName(Integer districtId) {
        if (districtId == null) {
            return null;
        }
        
        GHNDistrictResponse.GHNDistrict district = districtIdToDistrict.get(districtId);
        return district != null ? district.getDistrictName() : null;
    }
    
    /**
     * Reverse mapping: Get ward name by code
     */
    public String getWardName(String wardCode) {
        if (wardCode == null || wardCode.trim().isEmpty()) {
            return null;
        }
        
        GHNWardResponse.GHNWard ward = wardCodeToWard.get(wardCode);
        return ward != null ? ward.getWardName() : null;
    }
    
    /**
     * Load all provinces from GHN API
     */
    private void loadProvinces() {
        if (provincesLoaded || isLoadingProvinces) {
            return;
        }
        
        synchronized (this) {
            if (provincesLoaded || isLoadingProvinces) {
                return;
            }
            
            isLoadingProvinces = true;
            
            try {
                log.info("Loading provinces from GHN API");
                
                HttpHeaders headers = createHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<GHNProvinceResponse> response = restTemplate.exchange(
                    GHNConfig.ghn_BaseUrl + "/shiip/public-api/master-data/province",
                    HttpMethod.GET,
                    entity,
                    GHNProvinceResponse.class
                );
                
                GHNProvinceResponse provinceResponse = response.getBody();
                if (provinceResponse != null && provinceResponse.isSuccess() && provinceResponse.getData() != null) {
                    for (GHNProvinceResponse.GHNProvince province : provinceResponse.getData()) {
                        String normalizedName = normalizeVietnameseName(province.getProvinceName());
                        provinceNameToProvince.put(normalizedName, province);
                        provinceIdToProvince.put(province.getProvinceId(), province);
                    }
                    
                    provincesLoaded = true;
                    log.info("Successfully loaded {} provinces from GHN API", provinceResponse.getData().size());
                } else {
                    log.error("Failed to load provinces from GHN API: {}", 
                        provinceResponse != null ? provinceResponse.getMessage() : "null response");
                }
                
            } catch (Exception e) {
                log.error("Exception while loading provinces from GHN API: {}", e.getMessage(), e);
            } finally {
                isLoadingProvinces = false;
            }
        }
    }
    
    /**
     * Load districts for a specific province
     */
    private void loadDistrictsForProvince(Integer provinceId) {
        // Check if districts for this province are already loaded
        boolean hasDistrictsForProvince = districtKeyToDistrict.keySet().stream()
            .anyMatch(key -> key.endsWith(":" + provinceId));
        
        if (hasDistrictsForProvince) {
            return;
        }
        
        try {
            log.debug("Loading districts for province {} from GHN API", provinceId);
            
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("province_id", provinceId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<GHNDistrictResponse> response = restTemplate.exchange(
                GHNConfig.ghn_BaseUrl + "/shiip/public-api/master-data/district",
                HttpMethod.POST,
                entity,
                GHNDistrictResponse.class
            );
            
            GHNDistrictResponse districtResponse = response.getBody();
            if (districtResponse != null && districtResponse.isSuccess() && districtResponse.getData() != null) {
                for (GHNDistrictResponse.GHNDistrict district : districtResponse.getData()) {
                    String key = createDistrictKey(district.getDistrictName(), provinceId);
                    districtKeyToDistrict.put(key, district);
                    districtIdToDistrict.put(district.getDistrictId(), district);
                }
                
                log.debug("Successfully loaded {} districts for province {}", 
                    districtResponse.getData().size(), provinceId);
            } else {
                log.error("Failed to load districts for province {}: {}", 
                    provinceId, districtResponse != null ? districtResponse.getMessage() : "null response");
            }
            
        } catch (Exception e) {
            log.error("Exception while loading districts for province {}: {}", provinceId, e.getMessage(), e);
        }
    }

    /**
     * Load wards for a specific district
     */
    private void loadWardsForDistrict(Integer districtId) {
        // Check if wards for this district are already loaded
        boolean hasWardsForDistrict = wardKeyToWard.keySet().stream()
            .anyMatch(key -> key.endsWith(":" + districtId));

        if (hasWardsForDistrict) {
            return;
        }

        try {
            log.debug("Loading wards for district {} from GHN API", districtId);

            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("district_id", districtId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<GHNWardResponse> response = restTemplate.exchange(
                GHNConfig.ghn_BaseUrl + "/shiip/public-api/master-data/ward",
                HttpMethod.POST,
                entity,
                GHNWardResponse.class
            );

            GHNWardResponse wardResponse = response.getBody();
            if (wardResponse != null && wardResponse.isSuccess() && wardResponse.getData() != null) {
                for (GHNWardResponse.GHNWard ward : wardResponse.getData()) {
                    String key = createWardKey(ward.getWardName(), districtId);
                    wardKeyToWard.put(key, ward);
                    wardCodeToWard.put(ward.getWardCode(), ward);
                }

                log.debug("Successfully loaded {} wards for district {}",
                    wardResponse.getData().size(), districtId);
            } else {
                log.error("Failed to load wards for district {}: {}",
                    districtId, wardResponse != null ? wardResponse.getMessage() : "null response");
            }

        } catch (Exception e) {
            log.error("Exception while loading wards for district {}: {}", districtId, e.getMessage(), e);
        }
    }

    /**
     * Create HTTP headers for GHN API calls
     * Updated to match GHN API documentation format
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", GHNConfig.ghn_ApiToken);  // lowercase as per GHN docs
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Ensure provinces are loaded before use
     */
    private void ensureProvincesLoaded() {
        if (!provincesLoaded) {
            loadProvinces();
        }
    }

    /**
     * Normalize Vietnamese names for consistent matching
     * Removes diacritics, converts to lowercase, and trims whitespace
     */
    private String normalizeVietnameseName(String name) {
        if (name == null) {
            return null;
        }

        // Remove diacritics and convert to lowercase
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .toLowerCase()
            .trim();

        // Remove common prefixes and suffixes for better matching
        normalized = normalized
            .replaceAll("^(tinh|tp\\.|thanh pho|quan|huyen|thi xa|phuong|xa|thi tran)\\s+", "")
            .replaceAll("\\s+(tinh|tp|thanh pho|quan|huyen|thi xa|phuong|xa|thi tran)$", "");

        return normalized;
    }

    /**
     * Create cache key for district mapping
     */
    private String createDistrictKey(String districtName, Integer provinceId) {
        return normalizeVietnameseName(districtName) + ":" + provinceId;
    }

    /**
     * Create cache key for ward mapping
     */
    private String createWardKey(String wardName, Integer districtId) {
        return normalizeVietnameseName(wardName) + ":" + districtId;
    }

    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("provincesLoaded", provincesLoaded);
        stats.put("provinceCount", provinceNameToProvince.size());
        stats.put("districtCount", districtKeyToDistrict.size());
        stats.put("wardCount", wardKeyToWard.size());
        return stats;
    }

    /**
     * Clear all caches (for testing or refresh purposes)
     */
    public void clearCaches() {
        log.info("Clearing GHN address caches");
        provinceNameToProvince.clear();
        provinceIdToProvince.clear();
        districtKeyToDistrict.clear();
        districtIdToDistrict.clear();
        wardKeyToWard.clear();
        wardCodeToWard.clear();
        provincesLoaded = false;
    }

    /**
     * Get all provinces for frontend consumption
     * Returns provinces in frontend-compatible format
     */
    public List<Map<String, Object>> getAllProvinces() {
        ensureProvincesLoaded();

        List<Map<String, Object>> provinces = new ArrayList<>();
        for (GHNProvinceResponse.GHNProvince province : provinceIdToProvince.values()) {
            Map<String, Object> provinceMap = new HashMap<>();
            provinceMap.put("name", province.getProvinceName());
            provinceMap.put("code", province.getProvinceId().toString());
            provinces.add(provinceMap);
        }

        // Sort by name for better UX
        provinces.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));

        return provinces;
    }

    /**
     * Get districts for a specific province for frontend consumption
     * Returns districts in frontend-compatible format
     */
    public List<Map<String, Object>> getDistrictsForProvince(Integer provinceId) {
        if (provinceId == null) {
            return new ArrayList<>();
        }

        loadDistrictsForProvince(provinceId);

        List<Map<String, Object>> districts = new ArrayList<>();
        for (GHNDistrictResponse.GHNDistrict district : districtIdToDistrict.values()) {
            if (district.getProvinceId().equals(provinceId)) {
                Map<String, Object> districtMap = new HashMap<>();
                districtMap.put("name", district.getDistrictName());
                districtMap.put("code", district.getDistrictId().toString());
                districts.add(districtMap);
            }
        }

        // Sort by name for better UX
        districts.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));

        return districts;
    }

    /**
     * Get wards for a specific district for frontend consumption
     * Returns wards in frontend-compatible format
     */
    public List<Map<String, Object>> getWardsForDistrict(Integer districtId) {
        if (districtId == null) {
            return new ArrayList<>();
        }

        loadWardsForDistrict(districtId);

        List<Map<String, Object>> wards = new ArrayList<>();
        for (GHNWardResponse.GHNWard ward : wardCodeToWard.values()) {
            if (ward.getDistrictId().equals(districtId)) {
                Map<String, Object> wardMap = new HashMap<>();
                wardMap.put("name", ward.getWardName());
                wardMap.put("code", ward.getWardCode());
                wards.add(wardMap);
            }
        }

        // Sort by name for better UX
        wards.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));

        return wards;
    }
}
