package com.smse.rubik_solver.dto;

import com.smse.rubik_solver.model.Cube;

import lombok.Data;

@Data
public class CubeSolveRequest {
    private Cube cube;
}
