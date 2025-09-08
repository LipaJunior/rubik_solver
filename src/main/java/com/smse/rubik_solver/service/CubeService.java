package com.smse.rubik_solver.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smse.rubik_solver.model.Color;
import com.smse.rubik_solver.model.Cube;
import com.smse.rubik_solver.model.CubeSolver;

@Service
public class CubeService {

    private final ValidationService validationService = new ValidationService();

    public Cube initializeCube(Cube cube) {
        cube.initArrays();
        return cube;
    }

    public List<String> solve(Cube cube) {
        CubeSolver solver = new CubeSolver();

        return solver.solveCube(cube);
    }

    public Cube applyMoves(Cube cube, List<String> moves) {
        cube.makeMovesFromList(moves);
        cube.syncToLists();
        return cube;
    }

    public Cube getRandomScramble(Cube cube, int n) {
        cube.randomScramble(n);
        cube.syncToLists();
        return cube;
    }

    public Cube createSolvedCube() {
        Cube cube = new Cube();

        cube.setUp(cube.makeCompletedFace(Color.W));
        cube.setFront(cube.makeCompletedFace(Color.R));
        cube.setRight(cube.makeCompletedFace(Color.B));
        cube.setLeft(cube.makeCompletedFace(Color.G));
        cube.setBack(cube.makeCompletedFace(Color.O));
        cube.setDown(cube.makeCompletedFace(Color.Y));
        cube.initArrays();

        return cube;
    }

    public boolean testSolutionIntegrity(int repetitions) {
        long totalTimeMs = 0;
        int totalMoves = 0;

        for (int i = 0; i < repetitions; i++) {
            System.out.println("Iteration " + (i + 1));

            long startTime = System.nanoTime(); // start pomiaru czasu

            // 1. Stwórz losową kostkę
            Cube randomCube = createSolvedCube();
            getRandomScramble(randomCube, 20);

            if (!validationService.isCubeValid(randomCube)) {
                System.out.println("Invalid scrambled cube");
                return false;
            }

            // Skopiuj stan początkowy kostki
            Cube originalCube = deepCopyCube(randomCube);

            // 2. Rozwiąż ją
            List<String> solutionMoves = solve(randomCube);

            // 3. Zastosuj ruchy do oryginalnej kostki
            applyMoves(originalCube, solutionMoves);

            // Koniec pomiaru czasu
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000; // w milisekundach

            totalTimeMs += durationMs;
            totalMoves += solutionMoves.size();

            // 4. Sprawdź czy jest rozwiązana
            if (!originalCube.isCubeCompleted()) {
                System.out.println("Failed");
                return false; // nie udało się w którymś podejściu
            } else {
                System.out.println("Completed in " + solutionMoves.size() + " moves, time: " + durationMs + " ms");
            }
        }

        // Średnie wartości
        double avgTimeMs = (double) totalTimeMs / repetitions;
        double avgMoves = (double) totalMoves / repetitions;

        System.out.println("All " + repetitions + " iterations successful!");
        System.out.println("Total time: " + totalTimeMs + " ms");
        System.out.println("Average time: " + String.format("%.2f", avgTimeMs) + " ms");
        System.out.println("Average moves: " + String.format("%.2f", avgMoves));

        return true;
    }

    private Cube deepCopyCube(Cube original) {
        Cube copy = new Cube();
        copy.setUp(deepCopyFace(original.getUp()));
        copy.setFront(deepCopyFace(original.getFront()));
        copy.setBack(deepCopyFace(original.getBack()));
        copy.setLeft(deepCopyFace(original.getLeft()));
        copy.setRight(deepCopyFace(original.getRight()));
        copy.setDown(deepCopyFace(original.getDown()));
        copy.initArrays();
        return copy;
    }

    private List<List<Color>> deepCopyFace(List<List<Color>> face) {
        List<List<Color>> newFace = new ArrayList<>();
        for (List<Color> row : face) {
            newFace.add(new ArrayList<>(row));
        }
        return newFace;
    }

}
