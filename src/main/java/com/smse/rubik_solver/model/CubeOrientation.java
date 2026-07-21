package com.smse.rubik_solver.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Normalizacja orientacji kostki. Aplikacja zaklada sztywna orientacje (bialy
 * srodek na gorze, czerwony z przodu, niebieski prawo, zielony lewo, pomaranczowy
 * tyl, zolty dol). Uzytkownik moze jednak namalowac poprawna kostke obrocona
 * inaczej - te metody sprowadzaja ja do orientacji kanonicznej na potrzeby
 * walidacji i rozwiazania, a ruchy rozwiazania tlumacza z powrotem na uklad
 * uzytkownika (dzieki czemu kostka na ekranie zostaje tak jak namalowana).
 */
public final class CubeOrientation {

    private CubeOrientation() {
    }

    // Kolory srodkow w orientacji kanonicznej (wg pozycji sciany).
    private static final Map<Character, Color> CANONICAL_FACE_COLOR = Map.of(
            'U', Color.W, 'D', Color.Y,
            'R', Color.B, 'L', Color.G,
            'F', Color.R, 'B', Color.O);

    /**
     * Zwraca kopie kostki obrocona do orientacji kanonicznej, albo null jesli
     * srodki nie tworza poprawnego standardowego schematu (zadna rotacja calej
     * kostki nie daje kanonicznej orientacji - kostka jest niepoprawna).
     */
    public static Cube toCanonical(Cube cube) {
        Deque<Cube> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();

        Cube start = deepCopy(cube);
        queue.add(start);
        seen.add(signature(start));

        while (!queue.isEmpty()) {
            Cube c = queue.poll();
            if (isCanonical(c)) {
                return c;
            }
            for (Cube next : List.of(rotatedY(c), rotatedX(c))) {
                if (seen.add(signature(next))) {
                    queue.add(next);
                }
            }
        }
        return null;
    }

    /**
     * Tlumaczy ruchy rozwiazania z ukladu kanonicznego na uklad uzytkownika.
     * Ruch dotyczy sciany o danym kolorze srodka; szukamy, na ktorej pozycji ta
     * barwa jest u uzytkownika, i podmieniamy litere sciany (zachowujac ' / 2).
     */
    public static List<String> translate(List<String> canonicalMoves, Cube userCube) {
        userCube.initArrays();

        Map<Color, Character> colorToUserFace = new HashMap<>();
        colorToUserFace.put(userCube.getW()[1][1], 'U');
        colorToUserFace.put(userCube.getY()[1][1], 'D');
        colorToUserFace.put(userCube.getB()[1][1], 'R');
        colorToUserFace.put(userCube.getG()[1][1], 'L');
        colorToUserFace.put(userCube.getR()[1][1], 'F');
        colorToUserFace.put(userCube.getO()[1][1], 'B');

        List<String> out = new ArrayList<>(canonicalMoves.size());
        for (String move : canonicalMoves) {
            char canonicalFace = move.charAt(0);
            Color color = CANONICAL_FACE_COLOR.get(canonicalFace);
            char userFace = colorToUserFace.get(color);
            out.add(userFace + move.substring(1));
        }
        return out;
    }

    private static boolean isCanonical(Cube c) {
        return c.getW()[1][1] == Color.W && c.getR()[1][1] == Color.R
                && c.getB()[1][1] == Color.B && c.getG()[1][1] == Color.G
                && c.getO()[1][1] == Color.O && c.getY()[1][1] == Color.Y;
    }

    private static Cube rotatedY(Cube c) {
        Cube n = deepCopy(c);
        n.rotateY();
        n.syncToLists();
        return n;
    }

    private static Cube rotatedX(Cube c) {
        Cube n = deepCopy(c);
        n.rotateX();
        n.syncToLists();
        return n;
    }

    private static String signature(Cube c) {
        Color[][][] faces = { c.getW(), c.getR(), c.getB(), c.getG(), c.getO(), c.getY() };
        StringBuilder sb = new StringBuilder(54);
        for (Color[][] face : faces)
            for (Color[] row : face)
                for (Color col : row)
                    sb.append(col);
        return sb.toString();
    }

    private static Cube deepCopy(Cube original) {
        Cube copy = new Cube();
        copy.setUp(copyFace(original.getUp()));
        copy.setFront(copyFace(original.getFront()));
        copy.setRight(copyFace(original.getRight()));
        copy.setLeft(copyFace(original.getLeft()));
        copy.setBack(copyFace(original.getBack()));
        copy.setDown(copyFace(original.getDown()));
        copy.initArrays();
        return copy;
    }

    private static List<List<Color>> copyFace(List<List<Color>> face) {
        List<List<Color>> newFace = new ArrayList<>();
        for (List<Color> row : face) {
            newFace.add(new ArrayList<>(row));
        }
        return newFace;
    }
}
