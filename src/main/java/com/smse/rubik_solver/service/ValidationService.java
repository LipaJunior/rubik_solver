package com.smse.rubik_solver.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.smse.rubik_solver.model.Color;
import com.smse.rubik_solver.model.Cube;

@Service
public class ValidationService {
        public boolean isCubeValid(Cube cube) {
                return (areCentersValid(cube) && areCornersValid(cube) && areEdgesValid(cube)
                                && areCornerOrientationsValid(cube));
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

                        // znajdź, gdzie jest W/Y
                        for (int i = 0; i < 3; i++) {
                                if (corner.get(i) == Color.W || corner.get(i) == Color.Y) {
                                        sum += i;
                                        break;
                                }
                        }
                }

                // warunek osiągalności: suma skręceń ≡ 0 (mod 3)
                return (sum % 3 == 0);
        }

}
