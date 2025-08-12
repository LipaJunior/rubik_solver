package com.smse.rubik_solver.model;

import java.util.*;

public class CubeSolver {

    public List<String> solveCube(Cube cube) {
        cube.initArrays();

        List<String> allMoves = new ArrayList<>();

        allMoves.addAll(solveFirstLayer(cube));
        cube.syncToLists();

        allMoves.addAll(solveMiddleLayer(cube));
        cube.syncToLists();

        allMoves.addAll(solveLastLayer(cube));
        cube.syncToLists();

        return optimizeMoves(allMoves);
    }

    public List<String> solveLastLayer(Cube cube) {
        cube.initArrays();

        List<String> allMoves = new ArrayList<>();

        allMoves.addAll(solveCrossOnLastLayer(cube));
        cube.syncToLists();
        System.out.println("solveCrossOnLastLayer");

        allMoves.addAll(solveLastLayerPart2(cube));
        cube.syncToLists();
        System.out.println("solveLastLayerPart2");

        allMoves.addAll(solveLastLayerPart3(cube));
        cube.syncToLists();
        System.out.println("solveLastLayerPart3");

        allMoves.addAll(rotateEdges(cube));
        cube.syncToLists();
        System.out.println("rotateEdges");

        return optimizeMoves(allMoves);

    }

    public List<String> solveMiddleLayer(Cube cube) {
        cube.initArrays();

        List<String> allMoves = new ArrayList<>();

        allMoves.addAll(prepareForSolvingMiddleLayer(cube));
        cube.syncToLists();
        System.out.println("prepareForSolvingMiddleLayer");

        allMoves.addAll(solveMiddleLayer1(cube));
        cube.syncToLists();
        System.out.println("solveMiddleLayer1");

        return optimizeMoves(allMoves);

    }

    public List<String> solveFirstLayer(Cube cube) {
        cube.initArrays();

        List<String> allMoves = new ArrayList<>();

        allMoves.addAll(prepareForSolvingWhiteCross(cube));
        cube.syncToLists();
        System.out.println("prepareForSolvingWhiteCross");

        allMoves.addAll(solveWhiteCross(cube));
        cube.syncToLists();
        System.out.println("solveWhiteCross");

        allMoves.addAll(prepareForSolvingCornersFirstLayer(cube));
        cube.syncToLists();
        System.out.println("prepareForSolvingCornersFirstLayer");

        allMoves.addAll(solveCornersFirstLayer(cube));
        cube.syncToLists();
        System.out.println("solveCornersFirstLayer");

        return optimizeMoves(allMoves);

    }

    private List<String> optimizeMoves(List<String> moves) {
        Stack<String> stack = new Stack<>();

        for (String move : moves) {
            if (!stack.isEmpty()) {
                String top = stack.peek();
                if (isInverse(top, move)) {
                    stack.pop(); // Cancel inverse moves
                } else if (top.equals(move)) {
                    stack.pop();
                    if (!stack.isEmpty() && stack.peek().equals(move)) {
                        // Three same moves -> cancel all three and add inverse
                        stack.pop();
                        stack.push(getInverse(move));
                    } else {
                        // Two same moves -> store them temporarily
                        stack.push(move);
                        stack.push(move);
                    }
                } else {
                    stack.push(move);
                }
            } else {
                stack.push(move);
            }
        }

        return new ArrayList<>(stack);
    }

    private boolean isInverse(String move1, String move2) {
        return move1.equals(getInverse(move2));
    }

    private String getInverse(String move) {
        if (move.endsWith("'")) {
            return move.substring(0, move.length() - 1);
        } else {
            return move + "'";
        }
    }

    private List<String> solveCrossOnLastLayer(Cube cube) {
        Color center = cube.getDown().get(1).get(1);
        String state = ""
                + (cube.getDown().get(0).get(1) == center ? "U" : "")
                + (cube.getDown().get(1).get(0) == center ? "L" : "")
                + (cube.getDown().get(1).get(2) == center ? "R" : "")
                + (cube.getDown().get(2).get(1) == center ? "D" : "");

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
            cube.syncToLists();
            moves.add("D");
            rotations++;
            count = howManyEdgesInPlaceForPart2(cube);
        }

        if (count < 2) {
            throw new IllegalStateException("Invalid cube state: unable to position at least two last layer edges.");
        }

        if (count == 4)
            return moves;

        Color frontSticker = cube.getFront().get(2).get(1);
        Color frontCenter = cube.getFront().get(1).get(1);
        Color rightSticker = cube.getRight().get(2).get(1);
        Color rightCenter = cube.getRight().get(1).get(1);
        Color leftSticker = cube.getLeft().get(2).get(1);
        Color leftCenter = cube.getLeft().get(1).get(1);
        Color backSticker = cube.getBack().get(2).get(1);
        Color backCenter = cube.getBack().get(1).get(1);

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
        cube.syncToLists();
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
            cube.syncToLists();
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
            cube.syncToLists();
            moves.addAll(additionalMoves);
            count = whichCornersAreInPlace(cube).size();
        }

        return moves;
    }

    private List<String> rotateEdges(Cube cube) {
        List<String> moves = new ArrayList<>();

        if (whichCornersAreInPlace(cube).size() == 4) {
            for (int i = 0; i < 4; i++) {
                Color color = cube.getDown().get(0).get(2);
                while (!color.equals(Color.Y)) {
                    cube.sexyMove();
                    cube.syncToLists();
                    moves.addAll(Arrays.asList("R", "U", "R'", "U'"));
                    color = cube.getDown().get(0).get(2);
                }
                cube.moveD();
                cube.syncToLists();
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
                    cube.syncToLists();
                    moved = true;
                }
                if (r.equals(gr)) {
                    List<String> additional = Arrays.asList("D", "L", "D'", "L'", "D'", "F'", "D", "F");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
            }

            Set<Color> b = Set.of(cube.getB()[2][1], cube.getY()[1][2]);
            if (isEdgeOnCorrectPlaceForAlgForMiddleLayer(cube, Color.B)) {
                if (b.equals(bo)) {
                    List<String> additional = Arrays.asList("D'", "B'", "D", "B", "D", "R", "D'", "R'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
                if (b.equals(br)) {
                    List<String> additional = Arrays.asList("D", "F", "D'", "F'", "D'", "R'", "D", "R");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
            }

            Set<Color> g = Set.of(cube.getG()[2][1], cube.getY()[1][0]);
            if (isEdgeOnCorrectPlaceForAlgForMiddleLayer(cube, Color.G)) {
                if (g.equals(gr)) {
                    List<String> additional = Arrays.asList("D'", "F'", "D", "F", "D", "L", "D'", "L'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
                if (g.equals(go)) {
                    List<String> additional = Arrays.asList("D", "B", "D'", "B'", "D'", "L'", "D", "L");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
            }

            Set<Color> o = Set.of(cube.getO()[2][1], cube.getY()[2][1]);
            if (isEdgeOnCorrectPlaceForAlgForMiddleLayer(cube, Color.O)) {
                if (o.equals(go)) {
                    List<String> additional = Arrays.asList("D'", "L'", "D", "L", "D", "B", "D'", "B'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
                if (o.equals(bo)) {
                    List<String> additional = Arrays.asList("D", "R", "D'", "R'", "D'", "B'", "D", "B");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
            }

            if (!moved) {
                moves.add("D");
                cube.moveD();
                cube.syncToLists();
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
                    cube.syncToLists();
                    moved = true;
                } else if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.B)) {
                    List<String> additional = Arrays.asList("D", "F", "D'", "F'", "D'", "R'", "D", "R");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
            }
            if (!((cube.getB()[1][2] == Color.B && cube.getO()[1][0] == Color.O)
                    || (cube.getB()[1][2] == Color.Y || cube.getO()[1][0] == Color.Y))) {
                if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.B)) {
                    List<String> additional = Arrays.asList("D'", "B'", "D", "B", "D", "R", "D'", "R'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                } else if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.O)) {
                    List<String> additional = Arrays.asList("D", "R", "D'", "R'", "D'", "B'", "D", "B");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
            }
            if (!((cube.getO()[1][2] == Color.O && cube.getG()[1][0] == Color.G)
                    || (cube.getO()[1][2] == Color.Y || cube.getG()[1][0] == Color.Y))) {
                if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.O)) {
                    List<String> additional = Arrays.asList("D'", "L'", "D", "L", "D", "B", "D'", "B'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                } else if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.G)) {
                    List<String> additional = Arrays.asList("D", "B", "D'", "B'", "D'", "L'", "D", "L");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
            }
            if (!((cube.getR()[1][0] == Color.R && cube.getG()[1][2] == Color.G)
                    || (cube.getR()[1][0] == Color.Y || cube.getG()[1][2] == Color.Y))) {
                if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.R)) {
                    List<String> additional = Arrays.asList("D", "L", "D'", "L'", "D'", "F'", "D", "F");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                } else if (isEdgeOnCorrectPlaceForPreparingForMiddleLayer(cube, Color.G)) {
                    List<String> additional = Arrays.asList("D'", "F'", "D", "F", "D", "L", "D'", "L'");
                    moves.addAll(additional);
                    cube.makeMovesFromList(additional);
                    cube.syncToLists();
                    moved = true;
                }
            }
            if (!moved) {
                moves.add("D");
                cube.moveD();
                cube.syncToLists();
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
        if (cube.getFront().get(2).get(1) == cube.getFront().get(1).get(1))
            count++;
        if (cube.getRight().get(2).get(1) == cube.getRight().get(1).get(1))
            count++;
        if (cube.getLeft().get(2).get(1) == cube.getLeft().get(1).get(1))
            count++;
        if (cube.getBack().get(2).get(1) == cube.getBack().get(1).get(1))
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
                cube.syncToLists();
                moved = true;
            }
            if ((!(actualWRG.equals(WRG) || actualWRG.contains(Color.Y)))
                    && (actualYRG.equals(WRG) || actualYRG.contains(Color.Y))) {
                List<String> additional = Arrays.asList("F'", "D'", "F");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                cube.syncToLists();
                moved = true;
            }
            if ((!(actualWBO.equals(WBO) || actualWBO.contains(Color.Y)))
                    && (actualYBO.equals(WBO) || actualYBO.contains(Color.Y))) {
                List<String> additional = Arrays.asList("B'", "D'", "B");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                cube.syncToLists();
                moved = true;
            }
            if ((!(actualWGO.equals(WGO) || actualWGO.contains(Color.Y)))
                    && (actualYGO.equals(WGO) || actualYGO.contains(Color.Y))) {
                List<String> additional = Arrays.asList("L'", "D'", "L");
                moves.addAll(additional);
                cube.makeMovesFromList(additional);
                cube.syncToLists();
                moved = true;
            }
            if (!moved) {
                moves.add("D");
                cube.moveD();
                cube.syncToLists();
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
                    cube.syncToLists();
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
                    cube.syncToLists();
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
                    cube.syncToLists();
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
                    cube.syncToLists();
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
                cube.syncToLists();
            }
        }
        return moves;

    }

    public List<String> prepareForSolvingWhiteCross(Cube cube) {
        List<String> moves = dfs(cube, new ArrayList<>(), 0, 8);
        cube.makeMovesFromList(moves);
        cube.syncToLists();
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

            // Jeśli żaden warunek nie został spełniony, wykonaj D
            if (!moved) {
                moves.add("D");
                cube.moveD();
            }

            cube.syncToLists(); // synchronizuj po każdej pętli
        }

        return moves;
    }

    private List<String> dfs(Cube cube, List<String> path, int depth, int maxDepth) {
        if (isWhiteEdgesPrepared(cube)) {
            return path;
        }

        if (depth >= maxDepth) {
            return null;
        }

        List<String> moves = Arrays.asList("U", "U'", "D", "D'", "L", "L'", "R", "R'", "F", "F'", "B", "B'");

        for (String move : moves) {
            if (!isMoveAllowed(path, move)) {
                continue;
            }

            Cube newCube = deepCopyCube(cube);
            newCube.makeMovesFromList(Collections.singletonList(move));

            List<String> newPath = new ArrayList<>(path);
            newPath.add(move);

            List<String> result = dfs(newCube, newPath, depth + 1, maxDepth);
            if (result != null) {
                return result;
            }
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
