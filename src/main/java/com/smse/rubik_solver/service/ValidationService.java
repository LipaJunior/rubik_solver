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
        cube.initArrays();
        
        return (areCentersValid(cube) && areCornersValid(cube) && areEdgesValid(cube)
                && areCornerOrientationsValid(cube) && areEdgesOrientationsValid(cube)
                && checkParity(cube));
    }

    private boolean areCentersValid(Cube cube) {
        return (cube.getR()[1][1] == Color.R && cube.getW()[1][1] == Color.W
                && cube.getB()[1][1] == Color.B && cube.getG()[1][1] == Color.G
                && cube.getY()[1][1] == Color.Y && cube.getO()[1][1] == Color.O);
    }

    private boolean areCornersValid(Cube cube) {
        Set<Set<Color>> actualCorners = new HashSet<>();

        actualCorners.add(new HashSet<>(Arrays.asList( // U-F-L
                cube.getR()[0][0],    
                cube.getG()[0][2],      
                cube.getW()[2][0]))); 
        actualCorners.add(new HashSet<>(Arrays.asList( // U-F-R
                cube.getR()[0][2],    
                cube.getB()[0][0],    
                cube.getW()[2][2]))); 

        actualCorners.add(new HashSet<>(Arrays.asList( // U-B-L
                cube.getO()[0][2],   
                cube.getG()[0][0],   
                cube.getW()[0][0]))); 

        actualCorners.add(new HashSet<>(Arrays.asList( // U-B-R
                cube.getO()[0][0],    
                cube.getB()[0][2],   
                cube.getW()[0][2])));
        actualCorners.add(new HashSet<>(Arrays.asList( // D-F-L
                cube.getR()[2][0],   
                cube.getG()[2][2],    
                cube.getY()[0][0]))); 

        actualCorners.add(new HashSet<>(Arrays.asList( // D-F-R
                cube.getR()[2][2],    
                cube.getB()[2][0],    
                cube.getY()[0][2]))); 

        actualCorners.add(new HashSet<>(Arrays.asList( // D-B-L
                cube.getO()[2][2],    
                cube.getG()[2][0],    
                cube.getY()[2][0]))); 

        actualCorners.add(new HashSet<>(Arrays.asList( // D-B-R
                cube.getO()[2][0],    
                cube.getB()[2][2],    
                cube.getY()[2][2]))); 

        Set<Set<Color>> validCorners = new HashSet<>(Arrays.asList(
                Set.of(Color.W, Color.R, Color.G), // U-F-L
                Set.of(Color.W, Color.R, Color.B), // U-F-R
                Set.of(Color.W, Color.O, Color.G), // U-B-L
                Set.of(Color.W, Color.O, Color.B), // U-B-R
                Set.of(Color.Y, Color.R, Color.G), // D-F-L
                Set.of(Color.Y, Color.R, Color.B), // D-F-R
                Set.of(Color.Y, Color.O, Color.G), // D-B-L
                Set.of(Color.Y, Color.O, Color.B)  // D-B-R
        ));

        return actualCorners.equals(validCorners);
    }

    private boolean areEdgesValid(Cube cube) {
        Set<Set<Color>> actualEdges = new HashSet<>();

        actualEdges.add(Set.of( // U-F
                cube.getR()[0][1],    
                cube.getW()[2][1]));  

        actualEdges.add(Set.of( // U-L
                cube.getG()[0][1],    
                cube.getW()[1][0]));  

        actualEdges.add(Set.of( // U-R
                cube.getB()[0][1],   
                cube.getW()[1][2]));  

        actualEdges.add(Set.of( // U-B
                cube.getO()[0][1],    
                cube.getW()[0][1]));  

        actualEdges.add(Set.of( // D-F
                cube.getR()[2][1],    
                cube.getY()[0][1]));  

        actualEdges.add(Set.of( // D-L
                cube.getG()[2][1],   
                cube.getY()[1][0]));  

        actualEdges.add(Set.of( // D-R
                cube.getB()[2][1],    
                cube.getY()[1][2]));  

        actualEdges.add(Set.of( // D-B
                cube.getO()[2][1],    
                cube.getY()[2][1]));  

        actualEdges.add(Set.of( // F-L
                cube.getR()[1][0],   
                cube.getG()[1][2]));  

        actualEdges.add(Set.of( // F-R
                cube.getR()[1][2],    
                cube.getB()[1][0]));  

        actualEdges.add(Set.of( // B-L
                cube.getO()[1][2],    
                cube.getG()[1][0]));  

        actualEdges.add(Set.of( // B-R
                cube.getO()[1][0],    
                cube.getB()[1][2]));  

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
                Set.of(Color.O, Color.B)  // B-R
        );

        return actualEdges.equals(validEdges);
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
        int sum = 0;


        // UF
        if (cube.getW()[2][1] == Color.G || cube.getW()[2][1] == Color.B
                || ((cube.getW()[2][1] == Color.R || cube.getW()[2][1] == Color.O)
                && (cube.getR()[0][1] == Color.W || cube.getR()[0][1] == Color.Y))) {
            sum += 1;
        }

        // UR
        if (cube.getW()[1][2] == Color.G || cube.getW()[1][2] == Color.B
                || ((cube.getW()[1][2] == Color.R || cube.getW()[1][2] == Color.O)
                && (cube.getB()[0][1] == Color.W || cube.getB()[0][1] == Color.Y))) {
            sum += 1;
        }

        // UB
        if (cube.getW()[0][1] == Color.G || cube.getW()[0][1] == Color.B
                || ((cube.getW()[0][1] == Color.R || cube.getW()[0][1] == Color.O)
                && (cube.getO()[0][1] == Color.W || cube.getO()[0][1] == Color.Y))) {
            sum += 1;
        }

        // UL
        if (cube.getW()[1][0] == Color.G || cube.getW()[1][0] == Color.B
                || ((cube.getW()[1][0] == Color.R || cube.getW()[1][0] == Color.O)
                && (cube.getG()[0][1] == Color.W || cube.getG()[0][1] == Color.Y))) {
            sum += 1;
        }

        // DF
        if (cube.getY()[0][1] == Color.G || cube.getY()[0][1] == Color.B
                || ((cube.getY()[0][1] == Color.R || cube.getY()[0][1] == Color.O)
                && (cube.getR()[2][1] == Color.W || cube.getR()[2][1] == Color.Y))) {
            sum += 1;
        }

        // DR
        if (cube.getY()[1][2] == Color.G || cube.getY()[1][2] == Color.B
                || ((cube.getY()[1][2] == Color.R || cube.getY()[1][2] == Color.O)
                && (cube.getB()[2][1] == Color.W || cube.getB()[2][1] == Color.Y))) {
            sum += 1;
        }

        // DB
        if (cube.getY()[2][1] == Color.G || cube.getY()[2][1] == Color.B
                || ((cube.getY()[2][1] == Color.R || cube.getY()[2][1] == Color.O)
                && (cube.getO()[2][1] == Color.W || cube.getO()[2][1] == Color.Y))) {
            sum += 1;
        }

        // DL
        if (cube.getY()[1][0] == Color.G || cube.getY()[1][0] == Color.B
                || ((cube.getY()[1][0] == Color.R || cube.getY()[1][0] == Color.O)
                && (cube.getG()[2][1] == Color.W || cube.getG()[2][1] == Color.Y))) {
            sum += 1;
        }

        // FR
        if (cube.getR()[1][2] == Color.G || cube.getR()[1][2] == Color.B
                || ((cube.getR()[1][2] == Color.R || cube.getR()[1][2] == Color.O)
                && (cube.getB()[1][0] == Color.W || cube.getB()[1][0] == Color.Y))) {
            sum += 1;
        }

        // FL
        if (cube.getR()[1][0] == Color.G || cube.getR()[1][0] == Color.B
                || ((cube.getR()[1][0] == Color.R || cube.getR()[1][0] == Color.O)
                && (cube.getG()[1][2] == Color.W || cube.getG()[1][2] == Color.Y))) {
            sum += 1;
        }

        // BR
        if (cube.getO()[1][0] == Color.G || cube.getO()[1][0] == Color.B
                || ((cube.getO()[1][0] == Color.R || cube.getO()[1][0] == Color.O)
                && (cube.getB()[1][2] == Color.W || cube.getB()[1][2] == Color.Y))) {
            sum += 1;
        }

        // BL
        if (cube.getO()[1][2] == Color.G || cube.getO()[1][2] == Color.B
                || ((cube.getO()[1][2] == Color.R || cube.getO()[1][2] == Color.O)
                && (cube.getG()[1][0] == Color.W || cube.getG()[1][0] == Color.Y))) {
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
        actualCorners.add(Set.of( // U-F-L
                cube.getR()[0][0], cube.getG()[0][2], cube.getW()[2][0]));
        actualCorners.add(Set.of( // U-F-R
                cube.getR()[0][2], cube.getB()[0][0], cube.getW()[2][2]));
        actualCorners.add(Set.of( // U-B-R
                cube.getO()[0][0], cube.getB()[0][2], cube.getW()[0][2]));
        actualCorners.add(Set.of( // U-B-L
                cube.getO()[0][2], cube.getG()[0][0], cube.getW()[0][0]));
        actualCorners.add(Set.of( // D-F-L
                cube.getR()[2][0], cube.getG()[2][2], cube.getY()[0][0]));
        actualCorners.add(Set.of( // D-F-R
                cube.getR()[2][2], cube.getB()[2][0], cube.getY()[0][2]));
        actualCorners.add(Set.of( // D-B-R
                cube.getO()[2][0], cube.getB()[2][2], cube.getY()[2][2]));
        actualCorners.add(Set.of( // D-B-L
                cube.getO()[2][2], cube.getG()[2][0], cube.getY()[2][0]));

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

        List<Set<Color>> actualEdges = new ArrayList<>();
        actualEdges.add(Set.of(cube.getW()[2][1], cube.getR()[0][1])); // U–F
        actualEdges.add(Set.of(cube.getW()[1][2], cube.getB()[0][1])); // U–R
        actualEdges.add(Set.of(cube.getW()[0][1], cube.getO()[0][1])); // U–B
        actualEdges.add(Set.of(cube.getW()[1][0], cube.getG()[0][1])); // U–L
        actualEdges.add(Set.of(cube.getY()[0][1], cube.getR()[2][1])); // D–F
        actualEdges.add(Set.of(cube.getY()[1][2], cube.getB()[2][1])); // D–R
        actualEdges.add(Set.of(cube.getY()[2][1], cube.getO()[2][1])); // D–B
        actualEdges.add(Set.of(cube.getY()[1][0], cube.getG()[2][1])); // D–L
        actualEdges.add(Set.of(cube.getR()[1][2], cube.getB()[1][0])); // F–R
        actualEdges.add(Set.of(cube.getB()[1][2], cube.getO()[1][0])); // R–B
        actualEdges.add(Set.of(cube.getO()[1][2], cube.getG()[1][0])); // B–L
        actualEdges.add(Set.of(cube.getG()[1][2], cube.getR()[1][0])); // L–F

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
