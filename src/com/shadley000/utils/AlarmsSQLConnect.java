package com.shadley000.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class AlarmsSQLConnect
{
    // init database constants

    private static final String DATABASE_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DATABASE_URL =  "jdbc:mysql://a4alarms.ccbaz5k8ib32.us-east-2.rds.amazonaws.com:3306/a4alarms";
     //private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/alarms";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "0verl00k";
    private static final String MAX_POOL = "5";

    private Connection connection;
    private Properties properties;

    // create properties
    private Properties getProperties()
    {
        if (properties == null)
        {
            properties = new Properties();
            properties.setProperty("user", USERNAME);
            properties.setProperty("password", PASSWORD);
            properties.setProperty("MaxPooledStatements", MAX_POOL);
            properties.setProperty("useSSL", "false");
            //properties.setProperty("jdbcCompliantTruncation", "false");
            //properties.setProperty("characterEncoding", "UTF-8");
//zeroDateTimeBehavior=convertToNull

        }
        return properties;
    }

    // connect database
    public Connection connect()
    {
        if (connection == null)
        {
            try
            {
                Class.forName(DATABASE_DRIVER);
                connection = DriverManager.getConnection(DATABASE_URL, getProperties());
            } catch (ClassNotFoundException | SQLException e)
            {
                e.printStackTrace();
            }
        }
        return connection;
    }

    // disconnect database
    public void disconnect()
    {
        if (connection != null)
        {
            try
            {
                connection.close();
                connection = null;
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[])
    {
        AlarmsSQLConnect mySQLConnect = new AlarmsSQLConnect();
        Connection connection = mySQLConnect.connect();

        mySQLConnect.disconnect();

    }
}
