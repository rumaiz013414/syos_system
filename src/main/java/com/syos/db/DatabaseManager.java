package com.syos.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.syos.config.ConfigLoader;

public class DatabaseManager {
	private static final String URL = ConfigLoader.get("db.url");
	private static final String USER = ConfigLoader.get("db.username");
	private static final String PASSWORD = ConfigLoader.get("db.password");

	private static DatabaseManager instance;
	private Connection connection;

	private DatabaseManager() throws SQLException {
		this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
	}

	public static synchronized DatabaseManager getInstance() throws SQLException {
		if (instance == null || instance.connection.isClosed()) {
			instance = new DatabaseManager();
		}
		return instance;
	}

	public Connection getConnection() {
		return connection;
	}
}
