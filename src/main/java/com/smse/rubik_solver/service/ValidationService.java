package com.smse.rubik_solver.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.smse.rubik_solver.model.Color;
import com.smse.rubik_solver.model.Cube;

@Service
public class ValidationService {
        public boolean isCubeValid(Cube cube) {
                return (areCentersValid(cube) && areCornersValid(cube) && areEdgesValid(cube)
                                && areCornerOrientationsValid(cube) && areEdgesOrientationsValid(cube)
                                && checkParity(cube));
        }

        private boolean areCentersValid(Cube cube) {
                return (cube.getFront().get(1).get(1) == Color.R && cube.getUp().get(1).get(1) == Color.W
                                && cube.getRight().get(1).get(1) == Color.B && cube.getLeft().get(1).get(1) == Color.G
                                && cube.getDown().get(1).get(1) == Color.Y && cube.getBack().get(1).get(1) == Color.O);
        }

        private boolean areCornersValid(Cube cube) {
                Set<Set<Color>> actualCorners = new HashSet<>();

                actualCorners.add(new HashSet<>(Arrays.asList( // U-F-L
                                cube.getFront().get(0).get(0),
                                cube.getLeft().get(0).get(2),
                                cube.getUp().get(2).get(0))));

                actualCorners.add(new HashSet<>(Arrays.asList( // U-F-R
                                cube.getFront().get(0).get(2),
                                cube.getRight().get(0).get(0),
                                cube.getUp().get(2).get(2))));

                actualCorners.add(new HashSet<>(Arrays.asList( // U-B-L
                                cube.getBack().get(0).get(2),
                                cube.getLeft().get(0).get(0),
                                cube.getUp().get(0).get(0))));

                actualCorners.add(new HashSet<>(Arrays.asList( // U-B-R
                                cube.getBack().get(0).get(0),
                                cube.getRight().get(0).get(2),
                                cube.getUp().get(0).get(2))));

                actualCorners.add(new HashSet<>(Arrays.asList( // D-F-L
                                cube.getFront().get(2).get(0),
                                cube.getLeft().get(2).get(2),
                                cube.getDown().get(0).get(0))));

                actualCorners.add(new HashSet<>(Arrays.asList( // D-F-R
                                cube.getFront().get(2).get(2),
                                cube.getRight().get(2).get(0),
                                cube.getDown().get(0).get(2))));

                actualCorners.add(new HashSet<>(Arrays.asList( // D-B-L
                                cube.getBack().get(2).get(2),
                                cube.getLeft().get(2).get(0),
                                cube.getDown().get(2).get(0))));

                actualCorners.add(new HashSet<>(Arrays.asList( // D-B-R
                                cube.getBack().get(2).get(0),
                                cube.getRight().get(2).get(2),
                                cube.getDown().get(2).get(2))));

                Set<Set<Color>> validCorners = new HashSet<>(Arrays.asList(
                                Set.of(Color.W, Color.R, Color.G), // U-F-L
                                Set.of(Color.W, Color.R, Color.B), // U-F-R
                                Set.of(Color.W, Color.O, Color.G), // U-B-L
                                Set.of(Color.W, Color.O, Color.B), // U-B-R
                                Set.of(Color.Y, Color.R, Color.G), // D-F-L
                                Set.of(Color.Y, Color.R, Color.B), // D-F-R
                                Set.of(Color.Y, Color.O, Color.G), // D-B-L
                                Set.of(Color.Y, Color.O, Color.B) // D-B-R
                ));

                return actualCorners.equals(validCorners);
        }

        private boolean areEdgesValid(Cube cube) {
                Set<Set<Color>> actualEdges = new HashSet<>();

                actualEdges.add(Set.of( // U-F
                                cube.getFront().get(0).get(1),
                                cube.getUp().get(2).get(1)));

                actualEdges.add(Set.of( // U-L
                                cube.getLeft().get(0).get(1),
                                cube.getUp().get(1).get(0)));

                actualEdges.add(Set.of( // U-R
                                cube.getRight().get(0).get(1),
                                cube.getUp().get(1).get(2)));

                actualEdges.add(Set.of( // U-B
                                cube.getBack().get(0).get(1),
                                cube.getUp().get(0).get(1)));

                actualEdges.add(Set.of( // D-F
                                cube.getFront().get(2).get(1),
                                cube.getDown().get(0).get(1)));

                actualEdges.add(Set.of( // D-L
                                cube.getLeft().get(2).get(1),
                                cube.getDown().get(1).get(0)));

                actualEdges.add(Set.of( // D-R
                                cube.getRight().get(2).get(1),
                                cube.getDown().get(1).get(2)));

                actualEdges.add(Set.of( // D-B
                                cube.getBack().get(2).get(1),
                                cube.getDown().get(2).get(1)));

                actualEdges.add(Set.of( // F-L
                                cube.getFront().get(1).get(0),
                                cube.getLeft().get(1).get(2)));

                actualEdges.add(Set.of( // F-R
                                cube.getFront().get(1).get(2),
                                cube.getRight().get(1).get(0)));

                actualEdges.add(Set.of( // B-L
                                cube.getBack().get(1).get(2),
                                cube.getLeft().get(1).get(0)));

                actualEdges.add(Set.of( // B-R
                                cube.getBack().get(1).get(0),
                                cube.getRight().get(1).get(2)));

                Set<Set<Color>> validEdges = Set.of(
                                Set.of(Color.W, Color.R), // U-F
                                Set.of(Color.W, Color.G), // U-L
                                Set.of(Color.W, Color.B), // U-R
                                Set.of(Color.W, Color.O), // U-B
                                Set.of(Color.Y, Color.R), // D-F
                                Set.of(Color.Y, Color.G), // D-L
                                Set.of(Color.Y, Color.B), // D-R
                                Set.of(Color.Y, Color.O), // D-B
                                Set.of(Color.R, Color.G), // F-L
                                Set.of(Color.R, Color.B), // F-R
                                Set.of(Color.O, Color.G), // B-L
                                Set.of(Color.O, Color.B) // B-R
                );

                return actualEdges.equals(validEdges);
        }

        private boolean areCornerOrientationsValid(Cube cube) {

                int sum = 0;

                List<List<Color>> corners = List.of(
                                // UFR
                                List.of(cube.getUp().get(2).get(2), cube.getFront().get(0).get(2),
                                                cube.getRight().get(0).get(0)),
                                // URB
                                List.of(cube.getUp().get(0).get(2), cube.getRight().get(0).get(2),
                                                cube.getBack().get(0).get(0)),
                                // UBL
                                List.of(cube.getUp().get(0).get(0), cube.getBack().get(0).get(2),
                                                cube.getLeft().get(0).get(0)),
                                // ULF
                                List.of(cube.getUp().get(2).get(0), cube.getLeft().get(0).get(2),
                                                cube.getFront().get(0).get(0)),

                                // DRF
                                List.of(cube.getDown().get(0).get(2),
                                                cube.getRight().get(2).get(0), cube.getFront().get(2).get(2)),
                                // DBR
                                List.of(cube.getDown().get(2).get(2), cube.getBack().get(2).get(0),
                                                cube.getRight().get(2).get(2)),
                                // DLB
                                List.of(cube.getDown().get(2).get(0), cube.getLeft().get(2).get(0),
                                                cube.getBack().get(2).get(2)),
                                // DFL
                                List.of(cube.getDown().get(0).get(0), cube.getFront().get(2).get(0),
                                                cube.getLeft().get(2).get(2)));

                for (List<Color> corner : corners) {

                        for (int i = 0; i < 3; i++) {
                                if (corner.get(i) == Color.W || corner.get(i) == Color.Y) {
                                        sum += i;
                                        break;
                                }
                        }
                }

                return (sum % 3 == 0);
        }

        private boolean areEdgesOrientationsValid(Cube cube) {

                int sum = 0;

                // UF: U(2,1) + F(0,1)
                if (cube.getUp().get(2).get(1) == Color.G
                                || cube.getUp().get(2).get(1) == Color.B
                                || ((cube.getUp().get(2).get(1) == Color.R || cube.getUp().get(2).get(1) == Color.O)
                                                && (cube.getFront().get(0).get(1) == Color.W
                                                                || cube.getFront().get(0).get(1) == Color.Y))) {
                        sum += 1;
                }

                // UR: U(1,2) + R(0,1)
                if (cube.getUp().get(1).get(2) == Color.G
                                || cube.getUp().get(1).get(2) == Color.B
                                || ((cube.getUp().get(1).get(2) == Color.R || cube.getUp().get(1).get(2) == Color.O)
                                                && (cube.getRight().get(0).get(1) == Color.W
                                                                || cube.getRight().get(0).get(1) == Color.Y))) {
                        sum += 1;
                }

                // UB: U(0,1) + B(0,1)
                if (cube.getUp().get(0).get(1) == Color.G
                                || cube.getUp().get(0).get(1) == Color.B
                                || ((cube.getUp().get(0).get(1) == Color.R || cube.getUp().get(0).get(1) == Color.O)
                                                && (cube.getBack().get(0).get(1) == Color.W
                                                                || cube.getBack().get(0).get(1) == Color.Y))) {
                        sum += 1;
                }

                // UL: U(1,0) + L(0,1)
                if (cube.getUp().get(1).get(0) == Color.G
                                || cube.getUp().get(1).get(0) == Color.B
                                || ((cube.getUp().get(1).get(0) == Color.R || cube.getUp().get(1).get(0) == Color.O)
                                                && (cube.getLeft().get(0).get(1) == Color.W
                                                                || cube.getLeft().get(0).get(1) == Color.Y))) {
                        sum += 1;
                }

                // DF: D(0,1) + F(2,1)
                if (cube.getDown().get(0).get(1) == Color.G
                                || cube.getDown().get(0).get(1) == Color.B
                                || ((cube.getDown().get(0).get(1) == Color.R || cube.getDown().get(0).get(1) == Color.O)
                                                && (cube.getFront().get(2).get(1) == Color.W
                                                                || cube.getFront().get(2).get(1) == Color.Y))) {
                        sum += 1;
                }

                // DR: D(1,2) + R(2,1)
                if (cube.getDown().get(1).get(2) == Color.G
                                || cube.getDown().get(1).get(2) == Color.B
                                || ((cube.getDown().get(1).get(2) == Color.R || cube.getDown().get(1).get(2) == Color.O)
                                                && (cube.getRight().get(2).get(1) == Color.W
                                                                || cube.getRight().get(2).get(1) == Color.Y))) {
                        sum += 1;
                }

                // DB: D(2,1) + B(2,1)
                if (cube.getDown().get(2).get(1) == Color.G
                                || cube.getDown().get(2).get(1) == Color.B
                                || ((cube.getDown().get(2).get(1) == Color.R || cube.getDown().get(2).get(1) == Color.O)
                                                && (cube.getBack().get(2).get(1) == Color.W
                                                                || cube.getBack().get(2).get(1) == Color.Y))) {
                        sum += 1;
                }

                // DL: D(1,0) + L(2,1)
                if (cube.getDown().get(1).get(0) == Color.G
                                || cube.getDown().get(1).get(0) == Color.B
                                || ((cube.getDown().get(1).get(0) == Color.R || cube.getDown().get(1).get(0) == Color.O)
                                                && (cube.getLeft().get(2).get(1) == Color.W
                                                                || cube.getLeft().get(2).get(1) == Color.Y))) {
                        sum += 1;
                }

                // FR: F(1,2) + R(1,0)
                if (cube.getFront().get(1).get(2) == Color.G
                                || cube.getFront().get(1).get(2) == Color.B
                                || ((cube.getFront().get(1).get(2) == Color.R
                                                || cube.getFront().get(1).get(2) == Color.O)
                                                && (cube.getRight().get(1).get(0) == Color.W
                                                                || cube.getRight().get(1).get(0) == Color.Y))) {
                        sum += 1;
                }

                // FL: F(1,0) + L(1,2)
                if (cube.getFront().get(1).get(0) == Color.G
                                || cube.getFront().get(1).get(0) == Color.B
                                || ((cube.getFront().get(1).get(0) == Color.R
                                                || cube.getFront().get(1).get(0) == Color.O)
                                                && (cube.getLeft().get(1).get(2) == Color.W
                                                                || cube.getLeft().get(1).get(2) == Color.Y))) {
                        sum += 1;
                }

                // BR: B(1,0) + R(1,2)
                if (cube.getBack().get(1).get(0) == Color.G
                                || cube.getBack().get(1).get(0) == Color.B
                                || ((cube.getBack().get(1).get(0) == Color.R || cube.getBack().get(1).get(0) == Color.O)
                                                && (cube.getRight().get(1).get(2) == Color.W
                                                                || cube.getRight().get(1).get(2) == Color.Y))) {
                        sum += 1;
                }

                // BL: B(1,2) + L(1,0)
                if (cube.getBack().get(1).get(2) == Color.G
                                || cube.getBack().get(1).get(2) == Color.B
                                || ((cube.getBack().get(1).get(2) == Color.R || cube.getBack().get(1).get(2) == Color.O)
                                                && (cube.getLeft().get(1).get(0) == Color.W
                                                                || cube.getLeft().get(1).get(0) == Color.Y))) {
                        sum += 1;
                }

                return sum % 2 == 0;
        }

        private int cornerParity(Cube cube) {
                Map<Set<Color>, Integer> validCorners = new HashMap<>();
                validCorners.put(Set.of(Color.W, Color.R, Color.G), 0); // UFL
                validCorners.put(Set.of(Color.W, Color.R, Color.B), 1); // UFR
                validCorners.put(Set.of(Color.W, Color.O, Color.B), 2); // UBR
                validCorners.put(Set.of(Color.W, Color.O, Color.G), 3); // UBL
                validCorners.put(Set.of(Color.Y, Color.R, Color.G), 4); // DFL
                validCorners.put(Set.of(Color.Y, Color.R, Color.B), 5); // DFR
                validCorners.put(Set.of(Color.Y, Color.O, Color.B), 6); // DBR
                validCorners.put(Set.of(Color.Y, Color.O, Color.G), 7); // DBL

                List<Set<Color>> actualCorners = new ArrayList<>();
                // U-F-L
                actualCorners.add(Set.of(
                                cube.getFront().get(0).get(0),
                                cube.getLeft().get(0).get(2),
                                cube.getUp().get(2).get(0)));
                // U-F-R
                actualCorners.add(Set.of(
                                cube.getFront().get(0).get(2),
                                cube.getRight().get(0).get(0),
                                cube.getUp().get(2).get(2)));
                // U-B-R
                actualCorners.add(Set.of(
                                cube.getBack().get(0).get(0),
                                cube.getRight().get(0).get(2),
                                cube.getUp().get(0).get(2)));
                // U-B-L
                actualCorners.add(Set.of(
                                cube.getBack().get(0).get(2),
                                cube.getLeft().get(0).get(0),
                                cube.getUp().get(0).get(0)));
                // D-F-L
                actualCorners.add(Set.of(
                                cube.getFront().get(2).get(0),
                                cube.getLeft().get(2).get(2),
                                cube.getDown().get(0).get(0)));
                // D-F-R
                actualCorners.add(Set.of(
                                cube.getFront().get(2).get(2),
                                cube.getRight().get(2).get(0),
                                cube.getDown().get(0).get(2)));
                // D-B-R
                actualCorners.add(Set.of(
                                cube.getBack().get(2).get(0),
                                cube.getRight().get(2).get(2),
                                cube.getDown().get(2).get(2)));
                // D-B-L
                actualCorners.add(Set.of(
                                cube.getBack().get(2).get(2),
                                cube.getLeft().get(2).get(0),
                                cube.getDown().get(2).get(0)));

                List<Integer> perm = new ArrayList<>(8);
                for (Set<Color> corner : actualCorners) {
                        Integer idx = validCorners.get(corner);
                        if (idx == null)
                                return -1;
                        perm.add(idx);
                }

                int cycles = cycleCount(perm);
                if (cycles < 0)
                        return -1;
                return (8 - cycles) & 1; // parzystość 0=parzysta, 1=nieparzysta
        }

        private int edgeParity(Cube cube) {
                Map<Set<Color>, Integer> validEdges = new HashMap<>();

                validEdges.put(Set.of(Color.W, Color.R), 0); // U–F
                validEdges.put(Set.of(Color.W, Color.B), 1); // U–R
                validEdges.put(Set.of(Color.W, Color.O), 2); // U–B
                validEdges.put(Set.of(Color.W, Color.G), 3); // U–L
                validEdges.put(Set.of(Color.Y, Color.R), 4); // D–F
                validEdges.put(Set.of(Color.Y, Color.B), 5); // D–R
                validEdges.put(Set.of(Color.Y, Color.O), 6); // D–B
                validEdges.put(Set.of(Color.Y, Color.G), 7); // D–L
                validEdges.put(Set.of(Color.R, Color.B), 8); // F–R
                validEdges.put(Set.of(Color.B, Color.O), 9); // R–B
                validEdges.put(Set.of(Color.O, Color.G), 10); // B–L
                validEdges.put(Set.of(Color.G, Color.R), 11); // L–F

                List<Set<Color>> actualEdges = new ArrayList<>();
                // up
                actualEdges.add(Set.of(cube.getUp().get(2).get(1), cube.getFront().get(0).get(1))); // U–F
                actualEdges.add(Set.of(cube.getUp().get(1).get(2), cube.getRight().get(0).get(1))); // U–R
                actualEdges.add(Set.of(cube.getUp().get(0).get(1), cube.getBack().get(0).get(1))); // U–B
                actualEdges.add(Set.of(cube.getUp().get(1).get(0), cube.getLeft().get(0).get(1))); // U–L
                // down
                actualEdges.add(Set.of(cube.getDown().get(0).get(1), cube.getFront().get(2).get(1))); // D–F
                actualEdges.add(Set.of(cube.getDown().get(1).get(2), cube.getRight().get(2).get(1))); // D–R
                actualEdges.add(Set.of(cube.getDown().get(2).get(1), cube.getBack().get(2).get(1))); // D–B
                actualEdges.add(Set.of(cube.getDown().get(1).get(0), cube.getLeft().get(2).get(1))); // D–L
                // middle
                actualEdges.add(Set.of(cube.getFront().get(1).get(2), cube.getRight().get(1).get(0))); // F–R
                actualEdges.add(Set.of(cube.getRight().get(1).get(2), cube.getBack().get(1).get(0))); // R–B
                actualEdges.add(Set.of(cube.getBack().get(1).get(2), cube.getLeft().get(1).get(0))); // B–L
                actualEdges.add(Set.of(cube.getLeft().get(1).get(2), cube.getFront().get(1).get(0))); // L–F

                List<Integer> perm = new ArrayList<>(12);
                for (Set<Color> edge : actualEdges) {
                        Integer idx = validEdges.get(edge);
                        if (idx == null)
                                return -1;
                        perm.add(idx);
                }

                int cycles = cycleCount(perm);
                if (cycles < 0)
                        return -1;
                return (12 - cycles) & 1; // 0=parzysta, 1=nieparzysta
        }

        private boolean checkParity(Cube cube) {
                int cp = cornerParity(cube);
                int ep = edgeParity(cube);
                if (cp < 0 || ep < 0)
                        return false;
                return cp == ep; // warunek legalności: parzystości muszą być równe
        }

        private int cycleCount(List<Integer> perm) {
                int n = perm.size();
                boolean[] seen = new boolean[n];

                for (int idx : perm) {
                        if (idx < 0 || idx >= n)
                                return -1;
                        if (seen[idx])
                                return -1;
                        seen[idx] = true;
                }

                int cycles = 0;
                boolean[] vis = new boolean[n];
                for (int i = 0; i < n; i++) {
                        if (!vis[i]) {
                                cycles++;
                                int j = i;
                                while (!vis[j]) {
                                        vis[j] = true;
                                        j = perm.get(j);
                                }
                        }
                }
                return cycles;
        }

}
