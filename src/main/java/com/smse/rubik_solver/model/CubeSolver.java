package com.smse.rubik_solver.model;

import java.util.*;

public class CubeSolver {

    // Maksymalna glebokosc (w ruchach polobrotowych) dla skrotu DFS na starcie.
    // Kostki dajace sie ulozyc w tylu ruchach omijaja metode LBL, ktora robilaby
    // ich znacznie wiecej. Kazdy ruch to U/D/R/L/F/B jako obrot, obrot odwrotny
    // lub podwojny (liczony jako jeden krok).
    private static final int SHORTCUT_MAX_DEPTH = 4;

    private static final char[] SHORTCUT_FACES = { 'U', 'D', 'R', 'L', 'F', 'B' };

    public List<String> solveCube(Cube cube) {
        cube.initArrays();

        // Skrot: sprobuj ulozyc cala kostke w kilku ruchach, zanim ruszy LBL.
        List<String> shortcut = solveWithShortcut(cube);
        if (shortcut != null) {
            cube.makeMovesFromList(shortcut);
            cube.syncToLists();
            return optimizeMoves(shortcut);
        }

        List<String> allMoves = new ArrayList<>();

        allMoves.addAll(solveFirstLayer(cube));
        allMoves.addAll(solveMiddleLayer(cube));
        allMoves.addAll(solveLastLayer(cube));
        cube.syncToLists();

        return optimizeMoves(allMoves);
    }

    // Iteracyjne poglebianie: zwraca najkrotsza sekwencje pojedynczych ruchow
    // ukladajaca cala kostke, albo null jesli nie znaleziono w limicie glebokosci.
    private List<String> solveWithShortcut(Cube cube) {
        if (cube.isCubeCompleted()) {
            return new ArrayList<>();
        }

        for (int limit = 1; limit <= SHORTCUT_MAX_DEPTH; limit++) {
            List<String> path = new ArrayList<>();
            if (shortcutDfs(cube, path, limit, ' ')) {
                return path;
            }
        }
        return null;
    }

    // Przeszukiwanie make/undo na jednej kostce (bez kopiowania i bez syncToLists),
    // wolajac bezposrednio metody obrotu operujace na tablicach - to kluczowe dla
    // wydajnosci, bo wezlow sa setki tysiecy. Po znalezieniu rozwiazania ruchy sa
    // cofane w trakcie zwijania rekurencji, wiec kostka wraca do stanu wejsciowego,
    // a `path` zachowuje pelna sekwencje.
    private boolean shortcutDfs(Cube cube, List<String> path, int remaining, char lastFace) {
        if (cube.isCubeCompleted()) {
            return true;
        }
        if (remaining == 0) {
            return false;
        }

        for (char face : SHORTCUT_FACES) {
            // Dwa kolejne ruchy tej samej sciany zawsze da sie polaczyc w jeden,
            // wiec ich nie rozwazamy osobno.
            if (face == lastFace) {
                continue;
            }

            for (int kind = 0; kind < 3; kind++) {
                List<String> tokens = shortcutTokens(face, kind);

                applyShortcutMove(cube, face, kind);
                path.addAll(tokens);

                boolean found = shortcutDfs(cube, path, remaining - 1, face);

                applyShortcutMove(cube, face, inverseKind(kind));
                if (found) {
                    return true;
                }
                for (int k = 0; k < tokens.size(); k++) {
                    path.remove(path.size() - 1);
                }
            }
        }

        return false;
    }

    // kind: 0 = obrot, 1 = obrot odwrotny, 2 = podwojny (jako dwa pojedyncze ruchy,
    // dzieki czemu wynik pozostaje zgodny z makeMovesFromList i frontendem).
    private List<String> shortcutTokens(char face, int kind) {
        String f = String.valueOf(face);
        switch (kind) {
            case 0:
                return Collections.singletonList(f);
            case 1:
                return Collections.singletonList(f + "'");
            default:
                return Arrays.asList(f, f);
        }
    }

    private int inverseKind(int kind) {
        if (kind == 0)
            return 1; // obrot <-> obrot odwrotny
        if (kind == 1)
            return 0;
        return 2; // podwojny jest wlasna odwrotnoscia
    }

    private void applyShortcutMove(Cube cube, char face, int kind) {
        switch (kind) {
            case 0:
                turn(cube, face, false);
                break;
            case 1:
                turn(cube, face, true);
                break;
            default:
                turn(cube, face, false);
                turn(cube, face, false);
                break;
        }
    }

    private void turn(Cube cube, char face, boolean prime) {
        switch (face) {
            case 'U':
                if (prime)
                    cube.moveUprim();
                else
                    cube.moveU();
                break;
            case 'D':
                if (prime)
                    cube.moveDprim();
                else
                    cube.moveD();
                break;
            case 'R':
                if (prime)
                    cube.moveRprim();
                else
                    cube.moveR();
                break;
            case 'L':
                if (prime)
                    cube.moveLprim();
                else
                    cube.moveL();
                break;
            case 'F':
                if (prime)
                    cube.moveFprim();
                else
                    cube.moveF();
                break;
            case 'B':
                if (prime)
                    cube.moveBprim();
                else
                    cube.moveB();
                break;
        }
    }

    public List<String> solveLastLayer(Cube cube) {
        List<String> allMoves = new ArrayList<>();

        allMoves.addAll(solveCrossOnLastLayer(cube));
        allMoves.addAll(solveLastLayerPart2(cube));
        allMoves.addAll(solveLastLayerPart3(cube));
        allMoves.addAll(rotateEdges(cube));

        return allMoves;

    }

    public List<String> solveMiddleLayer(Cube cube) {
        List<String> allMoves = new ArrayList<>();

        allMoves.addAll(prepareForSolvingMiddleLayer(cube));
        allMoves.addAll(solveMiddleLayer1(cube));
        return allMoves;

    }

    public List<String> solveFirstLayer(Cube cube) {
        List<String> allMoves = new ArrayList<>();

        allMoves.addAll(prepareForSolvingWhiteCross(cube));
        allMoves.addAll(solveWhiteCross(cube));
        allMoves.addAll(prepareForSolvingCornersFirstLayer(cube));
        allMoves.addAll(solveCornersFirstLayer(cube));
        return allMoves;

    }

    private static final Map<Character, Integer> AXIS = Map.of(
            'U', 0, 'D', 0,
            'R', 1, 'L', 1,
            'F', 2, 'B', 2);

    public List<String> optimizeMoves(List<String> moves) {
        Stack<String> stack = new Stack<>();

        for (String move : moves) {
            stack.push(move);

            int axis = AXIS.getOrDefault(baseFace(move), -1);
            normalizeAxisTail(stack, axis);

            optimizeSequences(stack);
        }
        normalizeAxisTail(stack, 0); // UD
        normalizeAxisTail(stack, 1); // RL
        normalizeAxisTail(stack, 2); // FB

        return new ArrayList<>(stack);
    }

    private void normalizeAxisTail(Stack<String> stack, int axis) {
        if (axis < 0 || stack.isEmpty())
            return;

        int end = stack.size() - 1;
        int start = end;

        while (start >= 0) {
            String mv = stack.get(start);
            Integer ax = AXIS.get(baseFace(mv));
            if (ax == null || ax != axis)
                break;
            start--;
        }
        start++;
        if (start > end)
            return;

        int a = 0, b = 0;

        for (int i = start; i <= end; i++) {
            String mv = stack.get(i);
            char f = baseFace(mv);
            int d = isPrime(mv) ? -1 : 1;
            switch (axis) {
                case 0:
                    if (f == 'U')
                        a += d;
                    else if (f == 'D')
                        b += d;
                    break;
                case 1:
                    if (f == 'R')
                        a += d;
                    else if (f == 'L')
                        b += d;
                    break;
                case 2:
                    if (f == 'F')
                        a += d;
                    else if (f == 'B')
                        b += d;
                    break;
            }
        }
        for (int i = end; i >= start; i--)
            stack.pop();

        switch (axis) {
            case 0:
                pushReduced(stack, 'U', a);
                pushReduced(stack, 'D', b);
                break;
            case 1:
                pushReduced(stack, 'R', a);
                pushReduced(stack, 'L', b);
                break;
            case 2:
                pushReduced(stack, 'F', a);
                pushReduced(stack, 'B', b);
                break;
        }
    }

    private void pushReduced(Stack<String> stack, char face, int exp) {
        int k = ((exp % 4) + 4) % 4;
        if (k == 0)
            return;
        if (k == 1)
            stack.push(String.valueOf(face));
        else if (k == 2) {
            stack.push(String.valueOf(face));
            stack.push(String.valueOf(face));
        } else if (k == 3)
            stack.push(face + "'");
    }

    private void optimizeSequences(Stack<String> stack) {
        if (stack.size() < 4)
            return;

        String[] firstMoves = { "F", "L", "R", "B" };
        String secondMove = "D";

        for (String firstMove : firstMoves) {
            int repeatCount = countSequenceRepetitions(stack, firstMove, secondMove);

            if (repeatCount > 3) {
                optimizeSequenceRepetition(stack, firstMove, secondMove, repeatCount);
                return;
            }
        }
    }

    private int countSequenceRepetitions(Stack<String> stack, String first, String second) {
        int count = 0;
        int stackSize = stack.size();

        while (stackSize >= (count + 1) * 4) {
            int baseIndex = stackSize - 1 - count * 4;
            String m4 = stack.get(baseIndex);
            String m3 = stack.get(baseIndex - 1);
            String m2 = stack.get(baseIndex - 2);
            String m1 = stack.get(baseIndex - 3);

            if (isSequence(m1, m2, m3, m4, first, second)) {
                count++;
            } else
                break;
        }
        return count;
    }

    private void optimizeSequenceRepetition(Stack<String> stack, String first, String second, int repeatCount) {
        int optimizedCount = 6 - repeatCount;

        for (int i = 0; i < repeatCount * 4; i++)
            stack.pop();

        if (optimizedCount > 0) {
            for (int i = 0; i < optimizedCount; i++) {
                String[] toPush = { second, first, getInverse(second), getInverse(first) };
                for (String mv : toPush) {
                    stack.push(mv);
                    int ax = AXIS.getOrDefault(baseFace(mv), -1);
                    normalizeAxisTail(stack, ax);
                }
            }
        }
    }

    private boolean isSequence(String m1, String m2, String m3, String m4, String first, String second) {
        return (m1.equals(first) && m2.equals(second) &&
                m3.equals(getInverse(first)) && m4.equals(getInverse(second)));
    }

    private String getInverse(String move) {
        if (move.endsWith("'"))
            return move.substring(0, move.length() - 1);
        return move + "'";
    }

    private char baseFace(String move) {
        return move.charAt(0);
    }

    private boolean isPrime(String move) {
        return move.endsWith("'");
    }

    private List<String> solveCrossOnLastLayer(Cube cube) {
        Color center = cube.getY()[1][1];
        String state = ""
                + (cube.getY()[0][1] == center ? "U" : "")
                + (cube.getY()[1][0] == center ? "L" : "")
                + (cube.getY()[1][2] == center ? "R" : "")
                + (cube.getY()[2][1] == center ? "D" : "");

        List<String> moves;

        switch (state) {
            case "":
                moves = Arrays.asList("F", "L", "D", "L'", "D'", "F'", "B", "R", "D", "R'", "D'", "R", "D", "R'", "D'",
                        "B'");
                break;
            case "UL":
                moves = Arrays.asList("B", "R", "D", "R'", "D'", "R", "D", "R'", "D'", "B'");
                break;
            case "UR":
                moves = Arrays.asList("L", "B", "D", "B'", "D'", "B", "D", "B'", "D'", "L'");
                break;
            case "LD":
                moves = Arrays.asList("R", "F", "D", "F'", "D'", "F", "D", "F'", "D'", "R'");
                break;
            case "RD":
                moves = Arrays.asList("F", "L", "D", "L'", "D'", "L", "D", "L'", "D'", "F'");
                break;
            case "UD":
                moves = Arrays.asList("L", "B", "D", "B'", "D'", "L'");
                break;
            case "LR":
                moves = Arrays.asList("F", "L", "D", "L'", "D'", "F'");
                break;
            default:
                moves = new ArrayList<>();
        }

        cube.makeMovesFromList(moves);
        return moves;
    }

    private List<String> solveLastLayerPart2(Cube cube) {
        List<String> moves = new ArrayList<>();
        int count = howManyEdgesInPlaceForPart2(cube);

        int rotations = 0;
        while (count < 2 && rotations < 4) {
            cube.moveD();
            moves.add("D");
            rotations++;
            count = howManyEdgesInPlaceForPart2(cube);
        }

        if (count < 2) {
            throw new IllegalStateException("Invalid cube state: unable to position at least two last layer edges.");
        }

        if (count == 4)
            return moves;

        Color frontSticker = cube.getR()[2][1];
        Color frontCenter = cube.getR()[1][1];
        Color rightSticker = cube.getB()[2][1];
        Color rightCenter = cube.getB()[1][1];
        Color leftSticker = cube.getG()[2][1];
        Color leftCenter = cube.getG()[1][1];
        Color backSticker = cube.getO()[2][1];
        Color backCenter = cube.getO()[1][1];

        String state = ""
                + (frontSticker == frontCenter ? "F" : "")
                + (rightSticker == rightCenter ? "R" : "")
                + (leftSticker == leftCenter ? "L" : "")
                + (backSticker == backCenter ? "B" : "");

        List<String> additionalMoves;
        switch (state) {
            case "RB":
                additionalMoves = Arrays.asList("D'", "B", "D'", "D'", "B'", "D'", "B", "D'", "B'");
                break;
            case "FR":
                additionalMoves = Arrays.asList("D'", "R", "D'", "D'", "R'", "D'", "R", "D'", "R'");
                break;
            case "LB":
                additionalMoves = Arrays.asList("D'", "L", "D'", "D'", "L'", "D'", "L", "D'", "L'");
                break;
            case "FL":
                additionalMoves = Arrays.asList("D'", "F", "D'", "D'", "F'", "D'", "F", "D'", "F'");
                break;
            case "RL":
                additionalMoves = Arrays.asList("D'", "L", "D'", "D'", "L'", "D'", "L", "D'", "L'",
                        "B", "D'", "D'", "B'", "D'", "B", "D'", "B'");
                break;
            case "FB":
                additionalMoves = Arrays.asList("D'", "B", "D'", "D'", "B'", "D'", "B", "D'", "B'",
                        "R", "D'", "D'", "R'", "D'", "R", "D'", "R'");
                break;
            default:
                additionalMoves = new ArrayList<>();
        }

        cube.makeMovesFromList(additionalMoves);
        moves.addAll(additionalMoves);
        return moves;
    }

    private List<String> solveLastLayerPart3(Cube cube) {
        List<String> moves = new ArrayList<>();
        List<String> additionalMoves;
        int count = whichCornersAreInPlace(cube).size();

        if (count == 4)
            return moves;

        int tries = 0;
        while (count == 0 && tries < 4) {
            additionalMoves = Arrays.asList("D", "L", "D'", "R'", "D", "L'", "D'", "R");
            cube.makeMovesFromList(additionalMoves);
            moves.addAll(additionalMoves);
            count = whichCornersAreInPlace(cube).size();
            tries++;
        }

        if (count == 0)
            throw new IllegalStateException("No corners could be placed after 4 attempts.");

        while (count == 1) {
            String correctCorner = whichCornersAreInPlace(cube).get(0);
            switch (correctCorner) {
                case "DLF":
                    additionalMoves = Arrays.asList("D", "L", "D'", "R'", "D", "L'", "D'", "R");
                    break;
                case "DFR":
                    additionalMoves = Arrays.asList("D", "F", "D'", "B'", "D", "F'", "D'", "B");
                    break;
                case "DRB":
                    additionalMoves = Arrays.asList("D", "R", "D'", "L'", "D", "R'", "D'", "L");
                    break;
                case "DBL":
                    additionalMoves = Arrays.asList("D", "B", "D'", "F'", "D", "B'", "D'", "F");
                    break;
                default:
                    throw new IllegalStateException("Unknown correct corner: " + correctCorner);
            }

            cube.makeMovesFromList(additionalMoves);
            moves.addAll(additionalMoves);
            count = whichCornersAreInPlace(cube).size();
        }

        return moves;
    }

    private List<String> rotateEdges(Cube cube) {
        List<String> moves = new ArrayList<>();

        if (whichCornersAreInPlace(cube).size() == 4) {
            for (int i = 0; i < 4; i++) {
                Color color = cube.getY()[0][2];
                while (!color.equals(Color.Y)) {
                    cube.sexyMove();
                    moves.addAll(Arrays.asList("R", "U", "R'", "U'"));
                    color = cube.getY()[0][2];
                }
                cube.moveD();
                moves.add("D");
            }
        }

        return moves;
    }

    private List<String> solveMiddleLayer1(Cube cube) {
        List<String> moves = new ArrayList<>();

        Set<Color> br = Set.of(Color.B, Color.R);
        Set<Color> bo = Set.of(Color.B, Color.O);
        Set<Color> gr = Set.of(Color.G, Color.R);
        Set<Color> go = Set.of(Color.G, Color.O);

        while (!isMiddleLayerSolved(cube)) {
            boolean moved = false;

            Set<Color> r = Set.of(cube.getR()[2][1], cube.getY()[0][1]);
            if (isEdgeOnCorrectPlaceForAlgForMiddleLayer(cube, Color.R)) {
                if (r.equals(br)) {
                    List<String> additional = Arrays.asList("D'", "R'", "D", "R", "D", "F", "D'", "F'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    // cube.syncToLists();
                    moved = true;
                }
                if (r.equals(gr)) {
                    List<String> additional = Arrays.asList("D", "L", "D'", "L'", "D'", "F'", "D", "F");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    // cube.syncToLists();
                    moved = true;
                }
            }

            Set<Color> b = Set.of(cube.getB()[2][1], cube.getY()[1][2]);
            if (isEdgeOnCorrectPlaceForAlgForMiddleLayer(cube, Color.B)) {
                if (b.equals(bo)) {
                    List<String> additional = Arrays.asList("D'", "B'", "D", "B", "D", "R", "D'", "R'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
                if (b.equals(br)) {
                    List<String> additional = Arrays.asList("D", "F", "D'", "F'", "D'", "R'", "D", "R");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
            }

            Set<Color> g = Set.of(cube.getG()[2][1], cube.getY()[1][0]);
            if (isEdgeOnCorrectPlaceForAlgForMiddleLayer(cube, Color.G)) {
                if (g.equals(gr)) {
                    List<String> additional = Arrays.asList("D'", "F'", "D", "F", "D", "L", "D'", "L'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
                if (g.equals(go)) {
                    List<String> additional = Arrays.asList("D", "B", "D'", "B'", "D'", "L'", "D", "L");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
            }

            Set<Color> o = Set.of(cube.getO()[2][1], cube.getY()[2][1]);
            if (isEdgeOnCorrectPlaceForAlgForMiddleLayer(cube, Color.O)) {
                if (o.equals(go)) {
                    List<String> additional = Arrays.asList("D'", "L'", "D", "L", "D", "B", "D'", "B'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
                if (o.equals(bo)) {
                    List<String> additional = Arrays.asList("D", "R", "D'", "R'", "D'", "B'", "D", "B");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
            }

            if (!moved) {
                moves.add("D");
                cube.moveD();
            }
        }

        return moves;
    }

    private List<String> prepareForSolvingMiddleLayer(Cube cube) {
        List<String> moves = new ArrayList<>();

        while (!isMiddleLayerReadyForSolving(cube)) {
            boolean moved = false;
            if (!((cube.getB()[1][0] == Color.B && cube.getR()[1][2] == Color.R)
                    || (cube.getB()[1][0] == Color.Y || cube.getR()[1][2] == Color.Y))) {
                if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.R)) {
                    List<String> additional = Arrays.asList("D'", "R'", "D", "R", "D", "F", "D'", "F'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                } else if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.B)) {
                    List<String> additional = Arrays.asList("D", "F", "D'", "F'", "D'", "R'", "D", "R");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
            }
            if (!((cube.getB()[1][2] == Color.B && cube.getO()[1][0] == Color.O)
                    || (cube.getB()[1][2] == Color.Y || cube.getO()[1][0] == Color.Y))) {
                if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.B)) {
                    List<String> additional = Arrays.asList("D'", "B'", "D", "B", "D", "R", "D'", "R'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                } else if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.O)) {
                    List<String> additional = Arrays.asList("D", "R", "D'", "R'", "D'", "B'", "D", "B");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
            }
            if (!((cube.getO()[1][2] == Color.O && cube.getG()[1][0] == Color.G)
                    || (cube.getO()[1][2] == Color.Y || cube.getG()[1][0] == Color.Y))) {
                if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.O)) {
                    List<String> additional = Arrays.asList("D'", "L'", "D", "L", "D", "B", "D'", "B'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                } else if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.G)) {
                    List<String> additional = Arrays.asList("D", "B", "D'", "B'", "D'", "L'", "D", "L");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
            }
            if (!((cube.getR()[1][0] == Color.R && cube.getG()[1][2] == Color.G)
                    || (cube.getR()[1][0] == Color.Y || cube.getG()[1][2] == Color.Y))) {
                if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.R)) {
                    List<String> additional = Arrays.asList("D", "L", "D'", "L'", "D'", "F'", "D", "F");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                } else if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.G)) {
                    List<String> additional = Arrays.asList("D'", "F'", "D", "F", "D", "L", "D'", "L'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
            }
            if (!moved) {
                moves.add("D");
                cube.moveD();
            }
        }
        return moves;

    }

    private boolean isMiddleLayerReadyForSolving(Cube cube) {
        // BR
        if (!((cube.getB()[1][0] == Color.B && cube.getR()[1][2] == Color.R)
                || (cube.getB()[1][0] == Color.Y || cube.getR()[1][2] == Color.Y)))
            return false;

        // BO
        if (!((cube.getB()[1][2] == Color.B && cube.getO()[1][0] == Color.O)
                || (cube.getB()[1][2] == Color.Y || cube.getO()[1][0] == Color.Y)))
            return false;

        // GR
        if (!((cube.getG()[1][2] == Color.G && cube.getR()[1][0] == Color.R)
                || (cube.getG()[1][2] == Color.Y || cube.getR()[1][0] == Color.Y)))
            return false;

        // GO
        if (!((cube.getG()[1][0] == Color.G && cube.getO()[1][2] == Color.O)
                || (cube.getG()[1][0] == Color.Y || cube.getO()[1][2] == Color.Y)))
            return false;

        return true;
    }

    private boolean isMiddleLayerSolved(Cube cube) {
        // BR
        if (!(cube.getB()[1][0] == Color.B && cube.getR()[1][2] == Color.R))
            return false;

        // BO
        if (!(cube.getB()[1][2] == Color.B && cube.getO()[1][0] == Color.O))
            return false;

        // GR
        if (!(cube.getG()[1][2] == Color.G && cube.getR()[1][0] == Color.R))
            return false;

        // GO
        if (!(cube.getG()[1][0] == Color.G && cube.getO()[1][2] == Color.O))
            return false;

        return true;
    }

    private boolean isEdgeOnCorrectPlaceForAlgForMiddleLayer(Cube cube, Color color) {
        Set<Color> br = Set.of(Color.B, Color.R);
        Set<Color> bo = Set.of(Color.B, Color.O);
        Set<Color> gr = Set.of(Color.G, Color.R);
        Set<Color> go = Set.of(Color.G, Color.O);

        Set<Color> r = Set.of(cube.getR()[2][1], cube.getY()[0][1]);
        Set<Color> b = Set.of(cube.getB()[2][1], cube.getY()[1][2]);
        Set<Color> g = Set.of(cube.getG()[2][1], cube.getY()[1][0]);
        Set<Color> o = Set.of(cube.getO()[2][1], cube.getY()[2][1]);
        if (color.equals(Color.R) && ((r.equals(br) || r.equals(gr)) && cube.getR()[2][1].equals(Color.R))) {
            return true;
        }
        if (color.equals(Color.B) && ((b.equals(br) || b.equals(bo)) && cube.getB()[2][1].equals(Color.B))) {
            return true;
        }
        if (color.equals(Color.G) && ((g.equals(gr) || g.equals(go)) && cube.getG()[2][1].equals(Color.G))) {
            return true;
        }
        if (color.equals(Color.O) && ((o.equals(bo) || o.equals(go)) && cube.getO()[2][1].equals(Color.O))) {
            return true;
        }
        return false;
    }

    private boolean isEdgeOnCorrectPlaceForPreparingForMiddleLayer(Cube cube, Color color) {

        Set<Color> r = Set.of(cube.getR()[2][1], cube.getY()[0][1]);
        Set<Color> b = Set.of(cube.getB()[2][1], cube.getY()[1][2]);
        Set<Color> g = Set.of(cube.getG()[2][1], cube.getY()[1][0]);
        Set<Color> o = Set.of(cube.getO()[2][1], cube.getY()[2][1]);
        if (color.equals(Color.R) && (r.contains(Color.Y))) {
            return true;
        }
        if (color.equals(Color.B) && (b.contains(Color.Y))) {
            return true;
        }
        if (color.equals(Color.G) && (g.contains(Color.Y))) {
            return true;
        }
        if (color.equals(Color.O) && (o.contains(Color.Y))) {
            return true;
        }
        return false;
    }

    private List<String> whichCornersAreInPlace(Cube cube) {
        List<String> correctCorners = new ArrayList<>();

        Color downCenter = cube.getY()[1][1];
        Color frontCenter = cube.getR()[1][1];
        Color rightCenter = cube.getB()[1][1];
        Color backCenter = cube.getO()[1][1];
        Color leftCenter = cube.getG()[1][1];

        Set<Color> dfr = Set.of(cube.getY()[0][2], cube.getR()[2][2], cube.getB()[2][0]);
        if (dfr.equals(Set.of(downCenter, frontCenter, rightCenter)))
            correctCorners.add("DFR");

        Set<Color> drb = Set.of(cube.getY()[2][2], cube.getB()[2][2], cube.getO()[2][0]);
        if (drb.equals(Set.of(downCenter, rightCenter, backCenter)))
            correctCorners.add("DRB");

        Set<Color> dbl = Set.of(cube.getY()[2][0], cube.getO()[2][2], cube.getG()[2][0]);
        if (dbl.equals(Set.of(downCenter, backCenter, leftCenter)))
            correctCorners.add("DBL");

        Set<Color> dlf = Set.of(cube.getY()[0][0], cube.getG()[2][2], cube.getR()[2][0]);
        if (dlf.equals(Set.of(downCenter, leftCenter, frontCenter)))
            correctCorners.add("DLF");

        return correctCorners;
    }

    private int howManyEdgesInPlaceForPart2(Cube cube) {
        int count = 0;
        if (cube.getR()[2][1] == cube.getR()[1][1])
            count++;
        if (cube.getB()[2][1] == cube.getB()[1][1])
            count++;
        if (cube.getG()[2][1] == cube.getG()[1][1])
            count++;
        if (cube.getO()[2][1] == cube.getO()[1][1])
            count++;
        return count;
    }

    private boolean areCornersFirstLayerReadyForSolving(Cube cube) {
        Set<Color> WRB = EnumSet.of(Color.W, Color.R, Color.B);
        Set<Color> WRG = EnumSet.of(Color.W, Color.R, Color.G);
        Set<Color> WBO = EnumSet.of(Color.W, Color.B, Color.O);
        Set<Color> WGO = EnumSet.of(Color.W, Color.G, Color.O);

        Set<Color> actualWRB = EnumSet.of(cube.getW()[2][2], cube.getR()[0][2], cube.getB()[0][0]);
        Set<Color> actualWRG = EnumSet.of(cube.getW()[2][0], cube.getR()[0][0], cube.getG()[0][2]);
        Set<Color> actualWBO = EnumSet.of(cube.getW()[0][2], cube.getB()[0][2], cube.getO()[0][0]);
        Set<Color> actualWGO = EnumSet.of(cube.getW()[0][0], cube.getG()[0][0], cube.getO()[0][2]);
        // WRB
        if (!(actualWRB.equals(WRB) || actualWRB.contains(Color.Y)))
            return false;

        // WRG
        if (!(actualWRG.equals(WRG) || actualWRG.contains(Color.Y)))
            return false;

        // WBO
        if (!(actualWBO.equals(WBO) || actualWBO.contains(Color.Y)))
            return false;

        // WGO
        if (!(actualWGO.equals(WGO) || actualWGO.contains(Color.Y)))
            return false;

        return true;
    }

    private boolean areCornersSolvedFirstLayer(Cube cube) {
        // WRB
        if (!(cube.getW()[2][2] == Color.W && cube.getR()[0][2] == Color.R && cube.getB()[0][0] == Color.B))
            return false;

        // WRG
        if (!(cube.getW()[2][0] == Color.W && cube.getR()[0][0] == Color.R && cube.getG()[0][2] == Color.G))
            return false;

        // WBO
        if (!(cube.getW()[0][2] == Color.W && cube.getB()[0][2] == Color.B && cube.getO()[0][0] == Color.O))
            return false;

        // WGO
        if (!(cube.getW()[0][0] == Color.W && cube.getG()[0][0] == Color.G && cube.getO()[0][2] == Color.O))
            return false;

        return true;
    }

    public List<String> prepareForSolvingCornersFirstLayer(Cube cube) {
        Set<Color> WRB = EnumSet.of(Color.W, Color.R, Color.B);
        Set<Color> WRG = EnumSet.of(Color.W, Color.R, Color.G);
        Set<Color> WBO = EnumSet.of(Color.W, Color.B, Color.O);
        Set<Color> WGO = EnumSet.of(Color.W, Color.G, Color.O);

        List<String> moves = new ArrayList<>();

        while (!areCornersFirstLayerReadyForSolving(cube)) {
            Set<Color> actualWRB = EnumSet.of(cube.getW()[2][2], cube.getR()[0][2], cube.getB()[0][0]);
            Set<Color> actualWRG = EnumSet.of(cube.getW()[2][0], cube.getR()[0][0], cube.getG()[0][2]);
            Set<Color> actualWBO = EnumSet.of(cube.getW()[0][2], cube.getB()[0][2], cube.getO()[0][0]);
            Set<Color> actualWGO = EnumSet.of(cube.getW()[0][0], cube.getG()[0][0], cube.getO()[0][2]);

            Set<Color> actualYRB = EnumSet.of(cube.getY()[0][2], cube.getR()[2][2], cube.getB()[2][0]);
            Set<Color> actualYRG = EnumSet.of(cube.getY()[0][0], cube.getR()[2][0], cube.getG()[2][2]);
            Set<Color> actualYBO = EnumSet.of(cube.getY()[2][2], cube.getB()[2][2], cube.getO()[2][0]);
            Set<Color> actualYGO = EnumSet.of(cube.getY()[2][0], cube.getG()[2][0], cube.getO()[2][2]);
            boolean moved = false;
            if ((!(actualWRB.equals(WRB) || actualWRB.contains(Color.Y)))
                    && (actualYRB.equals(WRB) || actualYRB.contains(Color.Y))) {
                List<String> additional = Arrays.asList("R'", "D'", "R");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                moved = true;
            }
            if ((!(actualWRG.equals(WRG) || actualWRG.contains(Color.Y)))
                    && (actualYRG.equals(WRG) || actualYRG.contains(Color.Y))) {
                List<String> additional = Arrays.asList("F'", "D'", "F");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                moved = true;
            }
            if ((!(actualWBO.equals(WBO) || actualWBO.contains(Color.Y)))
                    && (actualYBO.equals(WBO) || actualYBO.contains(Color.Y))) {
                List<String> additional = Arrays.asList("B'", "D'", "B");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                moved = true;
            }
            if ((!(actualWGO.equals(WGO) || actualWGO.contains(Color.Y)))
                    && (actualYGO.equals(WGO) || actualYGO.contains(Color.Y))) {
                List<String> additional = Arrays.asList("L'", "D'", "L");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                moved = true;
            }
            if (!moved) {
                moves.add("D");
                cube.moveD();
            }
        }
        return moves;

    }

    public List<String> solveCornersFirstLayer(Cube cube) {
        Set<Color> WRB = EnumSet.of(Color.W, Color.R, Color.B);
        Set<Color> WRG = EnumSet.of(Color.W, Color.R, Color.G);
        Set<Color> WBO = EnumSet.of(Color.W, Color.B, Color.O);
        Set<Color> WGO = EnumSet.of(Color.W, Color.G, Color.O);

        List<String> moves = new ArrayList<>();

        while (!areCornersSolvedFirstLayer(cube)) {
            Set<Color> actualWRB = EnumSet.of(cube.getW()[2][2], cube.getR()[0][2], cube.getB()[0][0]);
            Set<Color> actualWRG = EnumSet.of(cube.getW()[2][0], cube.getR()[0][0], cube.getG()[0][2]);
            Set<Color> actualWBO = EnumSet.of(cube.getW()[0][2], cube.getB()[0][2], cube.getO()[0][0]);
            Set<Color> actualWGO = EnumSet.of(cube.getW()[0][0], cube.getG()[0][0], cube.getO()[0][2]);

            Set<Color> actualYRB = EnumSet.of(cube.getY()[0][2], cube.getR()[2][2], cube.getB()[2][0]);
            Set<Color> actualYRG = EnumSet.of(cube.getY()[0][0], cube.getR()[2][0], cube.getG()[2][2]);
            Set<Color> actualYBO = EnumSet.of(cube.getY()[2][2], cube.getB()[2][2], cube.getO()[2][0]);
            Set<Color> actualYGO = EnumSet.of(cube.getY()[2][0], cube.getG()[2][0], cube.getO()[2][2]);
            boolean moved = false;
            if (actualWRB.equals(WRB) || actualYRB.equals(WRB)) {
                while (!(cube.getW()[2][2] == Color.W && cube.getR()[0][2] == Color.R
                        && cube.getB()[0][0] == Color.B)) {
                    List<String> additional = Arrays.asList("F", "D", "F'", "D'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }
            }
            actualWRB = EnumSet.of(cube.getW()[2][2], cube.getR()[0][2], cube.getB()[0][0]);
            actualWRG = EnumSet.of(cube.getW()[2][0], cube.getR()[0][0], cube.getG()[0][2]);
            actualWBO = EnumSet.of(cube.getW()[0][2], cube.getB()[0][2], cube.getO()[0][0]);
            actualWGO = EnumSet.of(cube.getW()[0][0], cube.getG()[0][0], cube.getO()[0][2]);

            actualYRB = EnumSet.of(cube.getY()[0][2], cube.getR()[2][2], cube.getB()[2][0]);
            actualYRG = EnumSet.of(cube.getY()[0][0], cube.getR()[2][0], cube.getG()[2][2]);
            actualYBO = EnumSet.of(cube.getY()[2][2], cube.getB()[2][2], cube.getO()[2][0]);
            actualYGO = EnumSet.of(cube.getY()[2][0], cube.getG()[2][0], cube.getO()[2][2]);
            if (actualWRG.equals(WRG) || actualYRG.equals(WRG)) {
                while (!(cube.getW()[2][0] == Color.W && cube.getR()[0][0] == Color.R
                        && cube.getG()[0][2] == Color.G)) {
                    List<String> additional = Arrays.asList("L", "D", "L'", "D'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }

            }
            actualWRB = EnumSet.of(cube.getW()[2][2], cube.getR()[0][2], cube.getB()[0][0]);
            actualWRG = EnumSet.of(cube.getW()[2][0], cube.getR()[0][0], cube.getG()[0][2]);
            actualWBO = EnumSet.of(cube.getW()[0][2], cube.getB()[0][2], cube.getO()[0][0]);
            actualWGO = EnumSet.of(cube.getW()[0][0], cube.getG()[0][0], cube.getO()[0][2]);

            actualYRB = EnumSet.of(cube.getY()[0][2], cube.getR()[2][2], cube.getB()[2][0]);
            actualYRG = EnumSet.of(cube.getY()[0][0], cube.getR()[2][0], cube.getG()[2][2]);
            actualYBO = EnumSet.of(cube.getY()[2][2], cube.getB()[2][2], cube.getO()[2][0]);
            actualYGO = EnumSet.of(cube.getY()[2][0], cube.getG()[2][0], cube.getO()[2][2]);
            if (actualWBO.equals(WBO) || actualYBO.equals(WBO)) {
                while (!(cube.getW()[0][2] == Color.W && cube.getB()[0][2] == Color.B
                        && cube.getO()[0][0] == Color.O)) {
                    List<String> additional = Arrays.asList("R", "D", "R'", "D'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }

            }
            actualWRB = EnumSet.of(cube.getW()[2][2], cube.getR()[0][2], cube.getB()[0][0]);
            actualWRG = EnumSet.of(cube.getW()[2][0], cube.getR()[0][0], cube.getG()[0][2]);
            actualWBO = EnumSet.of(cube.getW()[0][2], cube.getB()[0][2], cube.getO()[0][0]);
            actualWGO = EnumSet.of(cube.getW()[0][0], cube.getG()[0][0], cube.getO()[0][2]);

            actualYRB = EnumSet.of(cube.getY()[0][2], cube.getR()[2][2], cube.getB()[2][0]);
            actualYRG = EnumSet.of(cube.getY()[0][0], cube.getR()[2][0], cube.getG()[2][2]);
            actualYBO = EnumSet.of(cube.getY()[2][2], cube.getB()[2][2], cube.getO()[2][0]);
            actualYGO = EnumSet.of(cube.getY()[2][0], cube.getG()[2][0], cube.getO()[2][2]);
            if (actualWGO.equals(WGO) || actualYGO.equals(WGO)) {
                while (!(cube.getW()[0][0] == Color.W && cube.getG()[0][0] == Color.G
                        && cube.getO()[0][2] == Color.O)) {
                    List<String> additional = Arrays.asList("B", "D", "B'", "D'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    moved = true;
                }

            }
            actualWRB = EnumSet.of(cube.getW()[2][2], cube.getR()[0][2], cube.getB()[0][0]);
            actualWRG = EnumSet.of(cube.getW()[2][0], cube.getR()[0][0], cube.getG()[0][2]);
            actualWBO = EnumSet.of(cube.getW()[0][2], cube.getB()[0][2], cube.getO()[0][0]);
            actualWGO = EnumSet.of(cube.getW()[0][0], cube.getG()[0][0], cube.getO()[0][2]);

            actualYRB = EnumSet.of(cube.getY()[0][2], cube.getR()[2][2], cube.getB()[2][0]);
            actualYRG = EnumSet.of(cube.getY()[0][0], cube.getR()[2][0], cube.getG()[2][2]);
            actualYBO = EnumSet.of(cube.getY()[2][2], cube.getB()[2][2], cube.getO()[2][0]);
            actualYGO = EnumSet.of(cube.getY()[2][0], cube.getG()[2][0], cube.getO()[2][2]);
            if (!moved) {
                moves.add("D");
                cube.moveD();
            }
        }
        return moves;

    }

    public List<String> prepareForSolvingWhiteCross(Cube cube) {
        List<String> moves = dfs(cube, new ArrayList<>(), 0, 8);
        cube.makeMovesFromList(moves);
        return (moves);
    }

    public List<String> solveWhiteCross(Cube cube) {
        List<String> moves = new ArrayList<>();

        while (!isWhiteCrossSolved(cube)) {
            boolean moved = false;

            if (cube.getR()[2][1] == Color.R && cube.getY()[0][1] == Color.W) {
                List<String> additional = Arrays.asList("F", "F");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                moved = true;
            } else if (cube.getB()[2][1] == Color.B && cube.getY()[1][2] == Color.W) {
                List<String> additional = Arrays.asList("R", "R");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                moved = true;
            } else if (cube.getG()[2][1] == Color.G && cube.getY()[1][0] == Color.W) {
                List<String> additional = Arrays.asList("L", "L");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                moved = true;
            } else if (cube.getO()[2][1] == Color.O && cube.getY()[2][1] == Color.W) {
                List<String> additional = Arrays.asList("B", "B");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                moved = true;
            }

            if (!moved) {
                moves.add("D");
                cube.moveD();
            }
        }

        return moves;
    }

    private static final List<String> DFS_MOVES = Arrays.asList(
            "U", "D", "R", "L", "F", "B",
            "U'", "D'", "R'", "L'", "F'", "B'");

    // make/undo na jednej kostce zamiast deepCopy na kazdym wezle. Po zakonczeniu
    // (sukces lub porazka) kostka jest przywracana do stanu wejsciowego, bo ruchy
    // stosuje dopiero prepareForSolvingWhiteCross.
    private List<String> dfs(Cube cube, List<String> path, int depth, int maxDepth) {
        if (isWhiteEdgesPrepared(cube)) {
            return new ArrayList<>(path);
        }

        if (depth >= maxDepth) {
            return null;
        }

        for (String move : DFS_MOVES) {
            if (!isMoveAllowed(path, move)) {
                continue;
            }

            char face = move.charAt(0);
            boolean prime = move.endsWith("'");

            turn(cube, face, prime);
            path.add(move);

            List<String> result = dfs(cube, path, depth + 1, maxDepth);

            turn(cube, face, !prime); // cofnij ruch
            if (result != null) {
                return result;
            }
            path.remove(path.size() - 1);
        }

        return null;
    }

    private boolean isMoveAllowed(List<String> path, String move) {
        int size = path.size();

        // Zakaz cofania ostatniego ruchu
        if (size >= 1 && isOpposite(move, path.get(size - 1))) {
            return false;
        }

        // Zakaz 3x tego samego ruchu pod rząd
        if (size >= 2 && path.get(size - 1).equals(move) && path.get(size - 2).equals(move)) {
            return false;
        }

        return true;
    }

    private boolean isOpposite(String move1, String move2) {
        return move1.charAt(0) == move2.charAt(0) && move1.length() != move2.length();
    }

    private boolean isWhiteCrossSolved(Cube cube) {

        return (cube.getW()[0][1] == Color.W && cube.getW()[1][0] == Color.W && cube.getW()[1][2] == Color.W
                && cube.getW()[2][1] == Color.W && cube.getW()[1][1] == Color.W && cube.getR()[0][1] == Color.R
                && cube.getB()[0][1] == Color.B && cube.getG()[0][1] == Color.G && cube.getO()[0][1] == Color.O);
    }

    private boolean isWhiteEdgesPrepared(Cube cube) {
        // Krawędzie białe powinny być albo na żółtym środku, albo dobrze ułożone na
        // białym
        return getWhiteEdgeCountPrepared(cube) == 4;
    }

    private int getWhiteEdgeCountPrepared(Cube cube) {
        int count = 0;
        if (cube.getY()[2][1] == Color.W)
            count++;
        if (cube.getY()[1][2] == Color.W)
            count++;
        if (cube.getY()[1][0] == Color.W)
            count++;
        if (cube.getY()[0][1] == Color.W)
            count++;

        if (cube.getW()[0][1] == Color.W && cube.getO()[0][1] == Color.O)
            count++;
        if (cube.getW()[1][0] == Color.W && cube.getG()[0][1] == Color.G)
            count++;
        if (cube.getW()[1][2] == Color.W && cube.getB()[0][1] == Color.B)
            count++;
        if (cube.getW()[2][1] == Color.W && cube.getR()[0][1] == Color.R)
            count++;

        return Math.min(count, 4);
    }
}
