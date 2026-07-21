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
import com.smse.rubik_solver.model.CubeOrientation;


@Service
public class ValidationService {

    public boolean isCubeValid(Cube cube) {
        cube.initArrays();

        // Zaakceptuj poprawna kostke niezaleznie od orientacji: najpierw sprowadz ja
        // do orientacji kanonicznej (null => srodki nie tworza poprawnego schematu),
        // a pozostale reguly sprawdz juz na znormalizowanej kostce.
        Cube canonical = CubeOrientation.toCanonical(cube);
        if (canonical == null) {
            return false;
        }

        return (areCentersValid(canonical) && areCornersValid(canonical) && areEdgesValid(canonical)
                && areCornerOrientationsValid(canonical) && areEdgesOrientationsValid(canonical)
                && checkParity(canonical));
    }

    private boolean areCentersValid(Cube cube) {
        return (cube.getR()[1][1] == Color.R && cube.getW()[1][1] == Color.W
                && cube.getB()[1][1] == Color.B && cube.getG()[1][1] == Color.G
                && cube.getY()[1][1] == Color.Y && cube.getO()[1][1] == Color.O);
    }

    // Naroza w kanonicznej kolejnosci: UFL, UFR, UBR, UBL, DFL, DFR, DBR, DBL.
    // Ta sama kolejnosc jest wymagana przez cornerParity (indeksy permutacji),
    // dlatego odczyt wspoldzieli jedna metoda zamiast powielac wspolrzedne.
    private List<Set<Color>> readCorners(Cube cube) {
        return List.of(
                new HashSet<>(Arrays.asList(cube.getR()[0][0], cube.getG()[0][2], cube.getW()[2][0])), // UFL
                new HashSet<>(Arrays.asList(cube.getR()[0][2], cube.getB()[0][0], cube.getW()[2][2])), // UFR
                new HashSet<>(Arrays.asList(cube.getO()[0][0], cube.getB()[0][2], cube.getW()[0][2])), // UBR
                new HashSet<>(Arrays.asList(cube.getO()[0][2], cube.getG()[0][0], cube.getW()[0][0])), // UBL
                new HashSet<>(Arrays.asList(cube.getR()[2][0], cube.getG()[2][2], cube.getY()[0][0])), // DFL
                new HashSet<>(Arrays.asList(cube.getR()[2][2], cube.getB()[2][0], cube.getY()[0][2])), // DFR
                new HashSet<>(Arrays.asList(cube.getO()[2][0], cube.getB()[2][2], cube.getY()[2][2])), // DBR
                new HashSet<>(Arrays.asList(cube.getO()[2][2], cube.getG()[2][0], cube.getY()[2][0]))); // DBL
    }

    private boolean areCornersValid(Cube cube) {
        Set<Set<Color>> validCorners = Set.of(
                Set.of(Color.W, Color.R, Color.G), // UFL
                Set.of(Color.W, Color.R, Color.B), // UFR
                Set.of(Color.W, Color.O, Color.B), // UBR
                Set.of(Color.W, Color.O, Color.G), // UBL
                Set.of(Color.Y, Color.R, Color.G), // DFL
                Set.of(Color.Y, Color.R, Color.B), // DFR
                Set.of(Color.Y, Color.O, Color.B), // DBR
                Set.of(Color.Y, Color.O, Color.G)); // DBL

        return new HashSet<>(readCorners(cube)).equals(validCorners);
    }

    // Krawedzie w kanonicznej kolejnosci: UF, UR, UB, UL, DF, DR, DB, DL, FR, RB, BL, LF.
    // Kolejnosc wymagana przez edgeParity; odczyt wspoldzieli jedna metoda.
    private List<Set<Color>> readEdges(Cube cube) {
        return List.of(
                Set.of(cube.getW()[2][1], cube.getR()[0][1]), // UF
                Set.of(cube.getW()[1][2], cube.getB()[0][1]), // UR
                Set.of(cube.getW()[0][1], cube.getO()[0][1]), // UB
                Set.of(cube.getW()[1][0], cube.getG()[0][1]), // UL
                Set.of(cube.getY()[0][1], cube.getR()[2][1]), // DF
                Set.of(cube.getY()[1][2], cube.getB()[2][1]), // DR
                Set.of(cube.getY()[2][1], cube.getO()[2][1]), // DB
                Set.of(cube.getY()[1][0], cube.getG()[2][1]), // DL
                Set.of(cube.getR()[1][2], cube.getB()[1][0]), // FR
                Set.of(cube.getB()[1][2], cube.getO()[1][0]), // RB
                Set.of(cube.getO()[1][2], cube.getG()[1][0]), // BL
                Set.of(cube.getG()[1][2], cube.getR()[1][0])); // LF
    }

    private boolean areEdgesValid(Cube cube) {
        Set<Set<Color>> validEdges = Set.of(
                Set.of(Color.W, Color.R), // UF
                Set.of(Color.W, Color.B), // UR
                Set.of(Color.W, Color.O), // UB
                Set.of(Color.W, Color.G), // UL
                Set.of(Color.Y, Color.R), // DF
                Set.of(Color.Y, Color.B), // DR
                Set.of(Color.Y, Color.O), // DB
                Set.of(Color.Y, Color.G), // DL
                Set.of(Color.R, Color.B), // FR
                Set.of(Color.B, Color.O), // RB
                Set.of(Color.O, Color.G), // BL
                Set.of(Color.G, Color.R)); // LF

        return new HashSet<>(readEdges(cube)).equals(validEdges);
    }

    private boolean areCornerOrientationsValid(Cube cube) {
        int sum = 0;

        List<List<Color>> corners = List.of(
                // UFR
                List.of(cube.getW()[2][2], cube.getR()[0][2], cube.getB()[0][0]),
                // URB
                List.of(cube.getW()[0][2], cube.getB()[0][2], cube.getO()[0][0]),
                // UBL
                List.of(cube.getW()[0][0], cube.getO()[0][2], cube.getG()[0][0]),
                // ULF
                List.of(cube.getW()[2][0], cube.getG()[0][2], cube.getR()[0][0]),

                // DRF
                List.of(cube.getY()[0][2], cube.getB()[2][0], cube.getR()[2][2]),
                // DBR
                List.of(cube.getY()[2][2], cube.getO()[2][0], cube.getB()[2][2]),
                // DLB
                List.of(cube.getY()[2][0], cube.getG()[2][0], cube.getO()[2][2]),
                // DFL
                List.of(cube.getY()[0][0], cube.getR()[2][0], cube.getG()[2][2])
        );

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
        // Kazdy wiersz: { naklejka glowna, naklejka sasiednia }. Regula orientacji
        // (ta sama dla wszystkich 12 krawedzi) liczona jest raz, w petli.
        Color[][] edges = {
                { cube.getW()[2][1], cube.getR()[0][1] }, // UF
                { cube.getW()[1][2], cube.getB()[0][1] }, // UR
                { cube.getW()[0][1], cube.getO()[0][1] }, // UB
                { cube.getW()[1][0], cube.getG()[0][1] }, // UL
                { cube.getY()[0][1], cube.getR()[2][1] }, // DF
                { cube.getY()[1][2], cube.getB()[2][1] }, // DR
                { cube.getY()[2][1], cube.getO()[2][1] }, // DB
                { cube.getY()[1][0], cube.getG()[2][1] }, // DL
                { cube.getR()[1][2], cube.getB()[1][0] }, // FR
                { cube.getR()[1][0], cube.getG()[1][2] }, // FL
                { cube.getO()[1][0], cube.getB()[1][2] }, // BR
                { cube.getO()[1][2], cube.getG()[1][0] }, // BL
        };

        int sum = 0;
        for (Color[] edge : edges) {
            Color primary = edge[0];
            Color neighbour = edge[1];

            boolean badlyOriented = primary == Color.G || primary == Color.B
                    || ((primary == Color.R || primary == Color.O)
                            && (neighbour == Color.W || neighbour == Color.Y));

            if (badlyOriented) {
                sum++;
            }
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

        List<Set<Color>> actualCorners = readCorners(cube);

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
        return (8 - cycles) & 1;
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

        List<Set<Color>> actualEdges = readEdges(cube);

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
        return (12 - cycles) & 1;
    }

    private boolean checkParity(Cube cube) {
        int cp = cornerParity(cube);
        int ep = edgeParity(cube);
        if (cp < 0 || ep < 0)
            return false;
        return cp == ep;
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
