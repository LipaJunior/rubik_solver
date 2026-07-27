package com.smse.rubik_solver.dto;

import java.util.List;

/**
 * Jeden etap rozwiazania (widok premium "krok po kroku"). Ruchy sa zoptymalizowane
 * OSOBNO w obrebie etapu - dzieki temu granice etapow sa czyste (zaden ruch nie
 * "przecieka" ani nie znika na styku etapow), a sklejone etapy nadal ukladaja kostke.
 */
public record SolveStage(String name, String description, List<String> moves) {
}
