package com.smse.rubik_solver.user;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.smse.rubik_solver.dto.CubeSolveRequest;
import com.smse.rubik_solver.dto.SolveStage;
import com.smse.rubik_solver.model.Cube;
import com.smse.rubik_solver.service.CubeService;
import com.smse.rubik_solver.service.ValidationService;

@RestController
public class PremiumController {

    private final CubeService cubeService;
    private final ValidationService validationService;
    private final UserRepository users;

    public PremiumController(CubeService cubeService, ValidationService validationService, UserRepository users) {
        this.cubeService = cubeService;
        this.validationService = validationService;
        this.users = users;
    }

    /**
     * Rozwiazanie "krok po kroku" (etapami). Endpoint pod /api/** => wymaga
     * zalogowania; dodatkowo sprawdzamy status premium (403 jesli brak).
     */
    @PostMapping("/api/solve-stages")
    public ResponseEntity<?> solveStages(@RequestBody CubeSolveRequest request,
            @AuthenticationPrincipal OidcUser principal) {

        AppUser user = principal == null ? null : users.findByEmail(principal.getEmail()).orElse(null);
        if (user == null || !"active".equals(user.getSubscriptionStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "premium_required"));
        }

        Cube cube = cubeService.initializeCube(request.getCube());
        if (!validationService.isCubeValid(cube)) {
            return ResponseEntity.badRequest().build();
        }

        List<SolveStage> stages = cubeService.solveStaged(cube);
        return ResponseEntity.ok(stages);
    }
}
