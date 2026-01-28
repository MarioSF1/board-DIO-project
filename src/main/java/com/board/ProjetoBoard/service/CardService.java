package com.board.ProjetoBoard.service;

import com.board.ProjetoBoard.dto.BoardColumnInfoDTO;
import com.board.ProjetoBoard.dto.CardDetailsDTO;
import com.board.ProjetoBoard.exception.CardBlockedException;
import com.board.ProjetoBoard.exception.CardFinishedException;
import com.board.ProjetoBoard.exception.EntityNotFoundException;
import com.board.ProjetoBoard.persistence.dao.BlockDAO;
import com.board.ProjetoBoard.persistence.dao.CardDAO;
import com.board.ProjetoBoard.persistence.entity.CardEntity;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static com.board.ProjetoBoard.persistence.entity.BoardColumnKindEnum.CANCEL;
import static com.board.ProjetoBoard.persistence.entity.BoardColumnKindEnum.FINAL;

@AllArgsConstructor
public class CardService {

    private final Connection connection;

    public void insert(final CardEntity entity) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            dao.insert(entity);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void moveToNextColumn(final Long cardId, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            var optional = dao.findById(cardId);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(cardId))
            );
            if (dto.blocked()){
                throw new CardBlockedException("O card de id %s está bloqueado".formatted(cardId));
            }
            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));
            if (currentColumn.kind().equals(FINAL)){
                throw new CardFinishedException("O card de id %s já foi finalizado".formatted(cardId));
            }
            var nextColumn = boardColumnsInfo.stream().filter(bc -> bc.order() == currentColumn.order() + 1).findFirst()
                            .orElseThrow(() -> new IllegalStateException("O card está cancelado"));
            dao.moveToColumn(currentColumn.id(), cardId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void cancel(final Long cardId, final Long cancelColumnId, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            var optional = dao.findById(cardId);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(cardId))
            );
            if (dto.blocked()){
                throw new CardBlockedException("O card de id %s está bloqueado".formatted(cardId));
            }

            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));

            if (currentColumn.kind().equals(FINAL)){
                throw new CardFinishedException("O card de id %s já foi finalizado".formatted(cardId));
            }

            boardColumnsInfo.stream().filter(bc -> bc.order() == currentColumn.order() + 1).findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card está cancelado"));

            dao.moveToColumn(cancelColumnId, cardId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void block(final Long id, final String reason, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(id))
            );
            if (dto.blocked()){
                throw new CardBlockedException("O card de id %s já está bloqueado".formatted(id));
            }
            var currentColumn = boardColumnsInfo.stream().filter(bc -> bc.id().equals(dto.columnId())).findFirst().orElseThrow();
            if (currentColumn.kind().equals(FINAL) || currentColumn.kind().equals(CANCEL)) {
                throw new IllegalStateException("O card está em uma coluna do tipo %s que não pode ser bloqueado".formatted(currentColumn.kind()));
            }
            var blockDAO = new BlockDAO(connection);
            blockDAO.block(reason, id);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new RuntimeException(e);
        }
    }

    public void unblock(final Long id, final String reason) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            var optional = dao.findById(id);

            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(id))
            );

            if (!dto.blocked()){
                throw new CardBlockedException("O card de id %s não está bloqueado".formatted(id));
            }


            var blockDAO = new BlockDAO(connection);
            blockDAO.unblock(reason, id);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

}
