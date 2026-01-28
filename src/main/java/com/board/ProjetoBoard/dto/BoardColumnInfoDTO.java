package com.board.ProjetoBoard.dto;

import com.board.ProjetoBoard.persistence.entity.BoardColumnKindEnum;

public record BoardColumnInfoDTO(Long id, int order, BoardColumnKindEnum kind) {
}
