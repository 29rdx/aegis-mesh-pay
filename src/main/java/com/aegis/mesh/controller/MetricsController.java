package com.aegis.mesh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/telemetry")
public class MetricsController {
    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        return Map.of(
            "status", "HEALTHY",
            "protocol", "AEGIS-MESH-V1",
            "revocationEngine", "BLOOM-FILTER",
            "consensus", "MERKLE-AUDIT"
        );
    }
}
