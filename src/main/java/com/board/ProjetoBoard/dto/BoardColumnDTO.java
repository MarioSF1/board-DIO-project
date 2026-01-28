package com.board.ProjetoBoard.dto;

import com.board.ProjetoBoard.persistence.entity.BoardColumnKindEnum;

public record BoardColumnDTO(
        Long id,
        String name,
        BoardColumnKindEnum kind,
        int cardsAmount
) {
}
