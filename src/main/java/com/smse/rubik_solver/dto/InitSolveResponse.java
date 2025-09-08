package com.smse.rubik_solver.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitSolveResponse {
    private String sessionID;
    private List<String> moves;
}
