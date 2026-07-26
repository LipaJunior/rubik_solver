package com.smse.rubik_solver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.smse.rubik_solver.dto.SolveStage;
import com.smse.rubik_solver.model.Color;
import com.smse.rubik_solver.model.Cube;

import com.smse.rubik_solver.service.*;

@SpringBootTest
class RubikSolverApplicationTests {

	final ValidationService validationService = new ValidationService();
	final CubeService cubeService = new CubeService();

	@Test
	void contextLoads() {
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

	@Test
	@DisplayName("Solving a random cube")
	void solvesRandomScramble() {

		Cube scrambled = cubeService.createSolvedCube();
		cubeService.getRandomScramble(scrambled, 20);

		assertTrue(validationService.isCubeValid(scrambled), "Scrambled cube should be valid");

		Cube originalCopy = deepCopyCube(scrambled);
		List<String> solutionMoves = cubeService.solve(scrambled);

		assertNotNull(solutionMoves, "Solver should return a list of moves");

		cubeService.applyMoves(originalCopy, solutionMoves);
		assertTrue(originalCopy.isCubeCompleted(), "Cube should be solved after applying the solution moves");
	}

	@Test
	@DisplayName("Staged solution (per-stage optimized) still solves the cube")
	void stagedSolutionSolvesCube() {
		Cube scrambled = cubeService.createSolvedCube();
		cubeService.getRandomScramble(scrambled, 20);

		assertTrue(validationService.isCubeValid(scrambled), "Scrambled cube should be valid");

		Cube originalCopy = deepCopyCube(scrambled);
		List<SolveStage> stages = cubeService.solveStaged(scrambled);

		assertNotNull(stages, "Powinny byc etapy");
		assertFalse(stages.isEmpty(), "Powinien byc co najmniej jeden etap");

		List<String> allMoves = new ArrayList<>();
		for (SolveStage s : stages) {
			allMoves.addAll(s.moves());
		}

		cubeService.applyMoves(originalCopy, allMoves);
		assertTrue(originalCopy.isCubeCompleted(),
				"Sklejone etapy powinny ulozyc kostke (per-etapowa optymalizacja zachowuje poprawnosc)");
	}

	@Test
	@DisplayName("Solving a solved cube")
	void solvingSolvedCube() {

		Cube solved = cubeService.createSolvedCube();

		assertTrue(validationService.isCubeValid(solved), "Cube should be valid");

		Cube originalCopy = deepCopyCube(solved);
		List<String> solutionMoves = cubeService.solve(solved);

		assertNotNull(solutionMoves, "Solver should return a list of moves");
		assertTrue(solutionMoves.isEmpty(), "Solution moves list should be empty");

		cubeService.applyMoves(originalCopy, solutionMoves);
		assertTrue(originalCopy.isCubeCompleted(), "Cube should be solved after applying the solution moves");
	}

	private static String serialize(Cube c) {
		StringBuilder sb = new StringBuilder();
		List<List<List<Color>>> faces = List.of(
				c.getUp(), c.getFront(), c.getRight(), c.getLeft(), c.getBack(), c.getDown());
		for (List<List<Color>> f : faces)
			for (List<Color> row : f)
				for (Color col : row)
					sb.append(col);
		return sb.toString();
	}

	@Test
	@DisplayName("Each single move produces the exact expected facelet state")
	void singleMovesMatchGolden() {
		String[][] golden = {
				{ "U", "WWWWWWWWWBBBRRRRRROOOBBBBBBRRRGGGGGGGGGOOOOOOYYYYYYYYY" },
				{ "U'", "WWWWWWWWWGGGRRRRRRRRRBBBBBBOOOGGGGGGBBBOOOOOOYYYYYYYYY" },
				{ "D", "WWWWWWWWWRRRRRRGGGBBBBBBRRRGGGGGGOOOOOOOOOBBBYYYYYYYYY" },
				{ "D'", "WWWWWWWWWRRRRRRBBBBBBBBBOOOGGGGGGRRROOOOOOGGGYYYYYYYYY" },
				{ "R", "WWRWWRWWRRRYRRYRRYBBBBBBBBBGGGGGGGGGWOOWOOWOOYYOYYOYYO" },
				{ "R'", "WWOWWOWWORRWRRWRRWBBBBBBBBBGGGGGGGGGYOOYOOYOOYYRYYRYYR" },
				{ "L", "OWWOWWOWWWRRWRRWRRBBBBBBBBBGGGGGGGGGOOYOOYOOYRYYRYYRYY" },
				{ "L'", "RWWRWWRWWYRRYRRYRRBBBBBBBBBGGGGGGGGGOOWOOWOOWOYYOYYOYY" },
				{ "F", "WWWWWWGGGRRRRRRRRRWBBWBBWBBGGYGGYGGYOOOOOOOOOBBBYYYYYY" },
				{ "F'", "WWWWWWBBBRRRRRRRRRYBBYBBYBBGGWGGWGGWOOOOOOOOOGGGYYYYYY" },
				{ "B", "BBBWWWWWWRRRRRRRRRBBYBBYBBYWGGWGGWGGOOOOOOOOOYYYYYYGGG" },
				{ "B'", "GGGWWWWWWRRRRRRRRRBBWBBWBBWYGGYGGYGGOOOOOOOOOYYYYYYBBB" },
		};

		for (String[] g : golden) {
			Cube cube = cubeService.createSolvedCube();
			cubeService.applyMoves(cube, List.of(g[0]));
			assertTrue(g[1].equals(serialize(cube)),
					"Move " + g[0] + " expected " + g[1] + " but got " + serialize(cube));
		}
	}

	@Test
	@DisplayName("Double-move token X2 equals applying X twice")
	void doubleMoveEqualsTwoSingles() {
		String[] faces = { "U", "D", "R", "L", "F", "B" };
		for (String f : faces) {
			Cube twice = cubeService.createSolvedCube();
			cubeService.applyMoves(twice, List.of(f, f));

			Cube doubled = cubeService.createSolvedCube();
			cubeService.applyMoves(doubled, List.of(f + "2"));

			assertTrue(serialize(twice).equals(serialize(doubled)),
					"Move " + f + "2 should equal " + f + " " + f);
		}
	}

	@Test
	@DisplayName("Parity check rejects an impossible single edge swap")
	void rejectsSingleEdgeSwap() {
		Cube cube = cubeService.createSolvedCube();
		// zamien krawedzie UF i UR (gorne naklejki obie biale, wiec rozroznia je
		// naklejki boczne): pojedyncza transpozycja = nieparzysta permutacja = nielegalna
		cube.getFront().get(0).set(1, Color.B); // R -> B
		cube.getRight().get(0).set(1, Color.R); // B -> R

		assertFalse(validationService.isCubeValid(cube), "Single edge swap must be rejected");
	}

	@Test
	@DisplayName("Shortcut DFS solves a lightly scrambled cube in few moves")
	void shortcutSolvesLightScramble() {
		Cube cube = cubeService.createSolvedCube();
		// 3-ruchowy scramble ukladalny w skrocie (<= SHORTCUT_MAX_DEPTH)
		List<String> scramble = List.of("R", "U", "F");
		cubeService.applyMoves(cube, scramble);

		assertTrue(validationService.isCubeValid(cube), "Scrambled cube should be valid");

		Cube originalCopy = deepCopyCube(cube);
		List<String> solutionMoves = cubeService.solve(cube);

		assertNotNull(solutionMoves, "Solver should return a list of moves");
		assertFalse(solutionMoves.isEmpty(), "Solution should not be empty for a scrambled cube");
		assertTrue(solutionMoves.size() <= 6,
				"Shortcut should find a short solution, got " + solutionMoves.size() + ": " + solutionMoves);

		cubeService.applyMoves(originalCopy, solutionMoves);
		assertTrue(originalCopy.isCubeCompleted(), "Cube should be solved after applying the solution moves");
	}

}
