package com.smse.rubik_solver.controller;

import org.springframework.web.bind.annotation.*;
import com.smse.rubik_solver.dto.CubeMoveRequest;
import com.smse.rubik_solver.dto.CubeScrambleRequest;
import com.smse.rubik_solver.dto.CubeSolveRequest;
import com.smse.rubik_solver.dto.InitResponseDto;
import com.smse.rubik_solver.dto.InitSolveResponse;
import com.smse.rubik_solver.model.Cube;
import com.smse.rubik_solver.model.UserSession;
import com.smse.rubik_solver.service.CubeService;
import com.smse.rubik_solver.service.SessionManager;
import com.smse.rubik_solver.service.ValidationService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/cube")
@RequiredArgsConstructor
public class CubeController {

    private final CubeService cubeService;
    private final SessionManager sessionManager;
    private final ValidationService validationService;

    private String getOrCreateSessionId(String sessionId) {
        if (sessionId != null && sessionManager.getSession(sessionId) != null) {
            return sessionId;
        } else {
            return sessionManager.createSession();
        }
    }

    private ResponseEntity<InitResponseDto> buildResponse(Cube cube, String sessionId) {
        InitResponseDto response = InitResponseDto.builder()
                .sessionId(sessionId)
                .cube(cube)
                .build();
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<InitResponseDto> saveAndRespond(Cube cube, String sessionId) {
        sessionId = getOrCreateSessionId(sessionId);
        UserSession session = sessionManager.getSession(sessionId);
        session.setCube(cube);
        return buildResponse(cube, sessionId);
    }

    @PostMapping("/init")
    public ResponseEntity<InitResponseDto> initCube(
            @RequestBody Cube c,
            @RequestParam(required = false) String sessionId) {
        try {
            Cube cube = cubeService.initializeCube(c);
            if (!validationService.isCubeValid(cube)) {
                return ResponseEntity.badRequest().build();
            }

            return saveAndRespond(cube, sessionId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Boolean> test(
            @RequestBody int count,
            @RequestParam(required = false) String sessionId) {
        try {
            boolean bool = cubeService.testSolutionIntegrity(count);
            return ResponseEntity.ok(bool);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/move")
    public ResponseEntity<InitResponseDto> applyMoves(
            @RequestBody CubeMoveRequest request,
            @RequestParam(required = false) String sessionId) {
        try {
            Cube cube = cubeService.initializeCube(request.getCube());
            if (!validationService.isCubeValid(cube)) {
                return ResponseEntity.badRequest().build();
            }

            cubeService.applyMoves(cube, request.getMoves());

            if (!validationService.isCubeValid(cube)) {
                return ResponseEntity.unprocessableEntity().build();
            }

            return saveAndRespond(cube, sessionId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/scramble")
    public ResponseEntity<InitResponseDto> scramble(
            @RequestBody CubeScrambleRequest request,
            @RequestParam(required = false) String sessionId) {
        try {
            Cube cube = cubeService.initializeCube(request.getCube());
            if (!validationService.isCubeValid(cube)) {
                return ResponseEntity.badRequest().build();
            }

            cubeService.getRandomScramble(cube, request.getN());

            if (!validationService.isCubeValid(cube)) {
                return ResponseEntity.unprocessableEntity().build();
            }

            return saveAndRespond(cube, sessionId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/random")
    public ResponseEntity<InitResponseDto> random(@RequestParam(required = false) String sessionId) {
        try {
            Cube cube = cubeService.createSolvedCube();
            cubeService.getRandomScramble(cube, 20);

            if (!validationService.isCubeValid(cube)) {
                return ResponseEntity.unprocessableEntity().build();
            }

            return saveAndRespond(cube, sessionId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/solve")
    public ResponseEntity<InitSolveResponse> solve(
            @RequestBody CubeSolveRequest request,
            @RequestParam(required = false) String sessionId) {
        try {
            Cube cube = cubeService.initializeCube(request.getCube());

            if (!validationService.isCubeValid(cube)) {
                System.out.println("Invalid cube");
                return ResponseEntity.badRequest().build();
            }

            List<String> solutionMoves = cubeService.solve(cube);
            /*
             * if (solutionMoves == null || solutionMoves.isEmpty()) {
             * return ResponseEntity.unprocessableEntity().build();
             * }
             */
            sessionId = getOrCreateSessionId(sessionId);
            UserSession session = sessionManager.getSession(sessionId);
            session.setCube(cube);

            InitSolveResponse response = InitSolveResponse.builder()
                    .sessionID(sessionId)
                    .moves(solutionMoves)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
