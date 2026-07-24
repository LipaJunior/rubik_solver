package com.smse.rubik_solver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.smse.rubik_solver.service.CubeService;
import com.smse.rubik_solver.service.ValidationService;

class CubeRotationTest {

    private static Cube deepCopy(Cube o) {
        Cube c = new Cube();
        c.setUp(copyFace(o.getUp()));
        c.setFront(copyFace(o.getFront()));
        c.setRight(copyFace(o.getRight()));
        c.setLeft(copyFace(o.getLeft()));
        c.setBack(copyFace(o.getBack()));
        c.setDown(copyFace(o.getDown()));
        c.initArrays();
        return c;
    }

    private static List<List<Color>> copyFace(List<List<Color>> face) {
        List<List<Color>> nf = new ArrayList<>();
        for (List<Color> row : face) nf.add(new ArrayList<>(row));
        return nf;
    }

    private Cube solved() {
        Cube c = new Cube();
        c.setUp(c.makeCompletedFace(Color.W));
        c.setFront(c.makeCompletedFace(Color.R));
        c.setRight(c.makeCompletedFace(Color.B));
        c.setLeft(c.makeCompletedFace(Color.G));
        c.setBack(c.makeCompletedFace(Color.O));
        c.setDown(c.makeCompletedFace(Color.Y));
        c.initArrays();
        return c;
    }

    // Serializacja z tablic (getW..getY), bo obroty aktualizuja tablice a nie listy.
    static String ser(Cube c) {
        Color[][][] fs = { c.getW(), c.getR(), c.getB(), c.getG(), c.getO(), c.getY() };
        StringBuilder sb = new StringBuilder();
        for (Color[][] f : fs)
            for (Color[] row : f)
                for (Color col : row)
                    sb.append(col);
        return sb.toString();
    }

    @Test
    void rotationsHaveOrder4() {
        Cube c = solved();
        c.makeMovesFromList(List.of("R", "U", "F", "L'", "D2", "B", "U'", "R2"));
        String orig = ser(c);

        for (int i = 0; i < 4; i++) c.rotateY();
        assertEquals(orig, ser(c), "rotateY^4 powinno byc identycznoscia");

        for (int i = 0; i < 4; i++) c.rotateX();
        assertEquals(orig, ser(c), "rotateX^4 powinno byc identycznoscia");
    }

    // Ostateczny sprawdzian: kostka poprawna, ale namalowana w dowolnej orientacji,
    // ma byc uznana za poprawna i rozwiazana ruchami dzialajacymi w JEJ orientacji.
    @Test
    void reorientedCubeIsValidAndSolvableInUserFrame() {
        ValidationService vs = new ValidationService();
        CubeService cs = new CubeService();
        Random rnd = new Random(1234);
        String[] mv = { "U", "U'", "D", "D'", "R", "R'", "L", "L'", "F", "F'", "B", "B'",
                "U2", "D2", "R2", "L2", "F2", "B2" };

        for (int iter = 0; iter < 20; iter++) {
            Cube cube = solved();
            // Losowa orientacja CALEJ kostki (obrot bez zmiany fizycznego stanu).
            for (int i = 0, n = rnd.nextInt(4); i < n; i++) cube.rotateX();
            for (int i = 0, n = rnd.nextInt(4); i < n; i++) cube.rotateY();
            cube.syncToLists();
            // Losowy scramble ruchami scian - juz w tej orientacji.
            List<String> scramble = new ArrayList<>();
            for (int i = 0; i < 22; i++) scramble.add(mv[rnd.nextInt(mv.length)]);
            cube.makeMovesFromList(scramble);
            cube.syncToLists();

            // 1) Walidacja: obrocona-ale-poprawna kostka MUSI byc poprawna.
            assertTrue(vs.isCubeValid(deepCopy(cube)),
                    "obrocona kostka powinna byc poprawna (iter " + iter + ")");

            // 2) Rozwiazanie + zastosowanie w orientacji uzytkownika -> ulozona.
            List<String> solution = cs.solve(deepCopy(cube));
            Cube applyTo = deepCopy(cube);
            applyTo.makeMovesFromList(solution);
            applyTo.syncToLists();
            assertTrue(applyTo.isCubeCompleted(),
                    "po zastosowaniu przetlumaczonych ruchow kostka ulozona (iter " + iter + ")");
        }
    }
}
