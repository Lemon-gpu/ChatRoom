import com.microsoft.sqlserver.jdbc.*;

import java.sql.*;

public class Database {
    static void connect() throws SQLException, ClassNotFoundException {
        //Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DriverManager.registerDriver(new SQLServerDriver());
        String userName = "Morgan";
        String password = "123456";
        String database = "ChatLog";
        String address = "127.0.0.1:1433";
        String connectionUrl =
                "jdbc:sqlserver://"+address+";"
                        + "database = "+database+";"
                        + "user="+userName+";"
                        + "password="+password+";";
        Connection connection = DriverManager.getConnection(connectionUrl);
        if(connection!=null){
            System.out.println("Success!");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from Test");
            while(resultSet.next()){
                System.out.println(resultSet.getString("ID")+" "+resultSet.getString("Name"));
            }

        }
        connection.close();
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        connect();
    }
}
