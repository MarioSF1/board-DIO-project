package com.board.ProjetoBoard.ui;

import com.board.ProjetoBoard.dto.BoardColumnInfoDTO;
import com.board.ProjetoBoard.persistence.entity.BoardColumnEntity;
import com.board.ProjetoBoard.persistence.entity.BoardEntity;
import com.board.ProjetoBoard.persistence.entity.CardEntity;
import com.board.ProjetoBoard.service.BoardColumnQueryService;
import com.board.ProjetoBoard.service.BoardQueryService;
import com.board.ProjetoBoard.service.CardQueryService;
import com.board.ProjetoBoard.service.CardService;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.Scanner;

import static com.board.ProjetoBoard.persistence.config.ConnectionConfig.getConnection;
import static com.board.ProjetoBoard.persistence.entity.BoardColumnKindEnum.INITIAL;

@AllArgsConstructor
public class BoardMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    private final BoardEntity entity;

    public void execute() {
        try {


            System.out.printf("Bem vindo ao board %s: %s, selecione a operação desejada\n", entity.getId(), entity.getName());
            var option = -1;
            while (option != 9) {
                System.out.println("1- Cria um card");
                System.out.println("2- mover um card");
                System.out.println("3- bloquear um card");
                System.out.println("4- desbloquear um card");
                System.out.println("5- cancelar um card");
                System.out.println("6- ver colunas");
                System.out.println("7- ver coluna com cards");
                System.out.println("8- ver card");
                System.out.println("9- voltar para o menu anterior");
                System.out.println("10- Sair");
                option = scanner.nextInt();

                switch (option) {
                    case 1 -> createCard();
                    case 2 -> moveCardToNextColumn();
                    case 3 -> blockCard();
                    case 4 -> unblockCard();
                    case 5 -> cancelCard();
                    case 6 -> showBoard();
                    case 7 -> showColumn();
                    case 8 -> showCard();
                    case 9 -> System.out.println("Voltando para o men anterior");
                    case 10 -> System.exit(0);
                    default -> System.out.println("Opção inválida, informe uma opção do menu");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void createCard() throws SQLException {
        var card = new CardEntity();
        System.out.println("Informe o título do card");
        card.setTitle(scanner.next());
        System.out.println("Informe a descrição do card");
        card.setDescription(scanner.next());
        card.setBoardColumn(entity.getInitialColumn());
        try (var connection = getConnection()){
            new CardService(connection).insert(card);
        }
    }

    private void moveCardToNextColumn() throws SQLException {
        System.out.println("Informe o id do card que deseja mover");
        var cardId = scanner.nextLong();
        var boardColumnsInfo = entity.getBoardColumns().stream().map(b -> new BoardColumnInfoDTO(b.getId(), b.getOrder(), b.getKind())).toList();
        try(var connection = getConnection()){
            new CardService(connection).moveToNextColumn(cardId, boardColumnsInfo);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void blockCard() throws SQLException  {
        System.out.println("Informe o id do card que será bloqueado");
        var cardId = scanner.nextLong();
        System.out.println("Informe o motivo do bloqueio do card");
        var blockReason = scanner.next();
        var boardColumnsInfo = entity.getBoardColumns().stream().map(b -> new BoardColumnInfoDTO(b.getId(), b.getOrder(), b.getKind())).toList();
        try(var connection = getConnection()){
            new CardService(connection).block(cardId, blockReason, boardColumnsInfo);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void unblockCard() throws SQLException  {
        System.out.println("Informe o id do card que será desbloqueado");
        var cardId = scanner.nextLong();
        System.out.println("Informe o motivo do desbloqueio do card");
        var unblockReason = scanner.next();
        try(var connection = getConnection()){
            new CardService(connection).unblock(cardId, unblockReason);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void cancelCard() throws SQLException {
        System.out.println("Informe o id do card que deseja mover para a coluna de cancelamento");
        var cardId = scanner.nextLong();
        var cancelColumn = entity.getCancelColumn();
        var boardColumnsInfo = entity.getBoardColumns().stream().map(b -> new BoardColumnInfoDTO(b.getId(), b.getOrder(), b.getKind())).toList();
        try(var connection = getConnection()){
            new CardService(connection).cancel(cardId, cancelColumn.getId(), boardColumnsInfo);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    private void showBoard() throws SQLException {
        try(var connection = getConnection()){
            var optional = new BoardQueryService(connection).showBoardDetails(entity.getId());
            optional.ifPresent(b -> {
                System.out.printf("Board %s: %s\n", b.id(), b.name());
                b.columns().forEach(c -> {
                    System.out.printf("Coluna %s | tipo: %s | tem %s cards\n", c.name(), c.kind(), c.cardsAmount());
                });
            });

        }
    }

    private void showColumn() throws SQLException {
        System.out.printf("Escolha uma coluna do board %s\n", entity.getName());
        var columnsIds = entity.getBoardColumns().stream().map(BoardColumnEntity::getId).toList();
        var selectedColumn = -1L;
        while (!columnsIds.contains(entity.getId())) {
            entity.getBoardColumns().forEach(c -> {
                System.out.printf("%s - %s [%s]\n", c.getId(), c.getName(), c.getKind());
            });
            selectedColumn = scanner.nextLong();
        }
        try(var connection = getConnection()) {
            var column = new BoardColumnQueryService(connection).findById(selectedColumn);
            column.ifPresent(co -> {
                System.out.printf("Coluna %s tipo %s\n", co.getName(), co.getKind());
                co.getCards().forEach(c -> {
                    System.out.printf("Card %s - %s\nDescrição: %s\n", c.getId(), c.getTitle(), c.getDescription());
                });
            });
        }
    }

    private void showCard() throws SQLException {
        System.out.println("Informe o Id do card que deseja visualizar");
        var selectedCardId = scanner.nextLong();
        try (var connection = getConnection()){
            new CardQueryService(connection).findById(selectedCardId).ifPresentOrElse(
            c -> {
                System.out.printf("Card %s - %s.\n", c.id(), c.title());
                System.out.printf("Descricão: %s\n", c.description());
                System.out.println(c.blocked() ?
                        "Está bloqueado. Motivo: " + c.blockReason() :
                        "Não esta bloqueado");
                System.out.printf("Já foi bloqueado %s vez(es)\n", c.blocksAmount());
                System.out.printf("Está no momento na coluna %s - %s\n", c.columnId(), c.columnName());
            },
            () -> System.out.println("Card de id " + selectedCardId + " não encontrado "));
        }

    }
}
