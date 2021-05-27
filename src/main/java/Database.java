import com.microsoft.sqlserver.jdbc.*;
import com.mysql.cj.jdbc.Driver;

import java.sql.*;

public class Database {
    static void connect() throws SQLException, ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");//——给SQL Server的
        //Class.forName("com.mysql.jdbc.Driver");——给旧版，也就是我们书上那版本的MySQL
        //Class.forName("com.mysql.cj.jdbc.Driver");——这是新版的MySQL
        //另外一种加载方式
        //DriverManager.registerDriver(new SQLServerDriver());
        //DriverManager.registerDriver(new Driver());
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
