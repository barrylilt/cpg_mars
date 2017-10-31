package cpg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class ConnectionUtil {

	private static final String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
 
	private static final String connectionURL = "jdbc:sqlserver://tablupeademo.cyrs41rq6kvj.us-east-1.rds.amazonaws.com:1433;databaseName=PEA_DEMO";

	private static final String username = "musername";

	private static final String password = "demo123456";

	private Connection con = null;

	
	private  Connection getDBConnection() {
		if (con == null) {

			try {
				Class.forName(driver);
				
				con = DriverManager.getConnection(connectionURL, username, password);
			} catch (SQLException e) {
				
				e.printStackTrace();
				
			} catch (ClassNotFoundException e) {
				
				e.printStackTrace();
			}
		}
		return con;
	}

	
	public ConnectionUtil(){
		con = getDBConnection();
	}
	
	public  String executeQuery(String sql) {
		String res = "0.0";
	
		try {

			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				res = rs.getString(1);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return res;
	}

	/*public  void closeConnection(){
		if(con != null){
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}*/
	
	
}
