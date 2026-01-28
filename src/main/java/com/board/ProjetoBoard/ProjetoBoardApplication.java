package com.board.ProjetoBoard;

import com.board.ProjetoBoard.persistence.migration.MigrationStrategy;
import com.board.ProjetoBoard.ui.MainMenu;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;

import static com.board.ProjetoBoard.persistence.config.ConnectionConfig.getConnection;

@SpringBootApplication
public class ProjetoBoardApplication {

	public static void main(String[] args) throws SQLException {

		try(var connection = getConnection()){
			new MigrationStrategy(connection).executeMigration();
		}
		new MainMenu().execute();
	}

}
