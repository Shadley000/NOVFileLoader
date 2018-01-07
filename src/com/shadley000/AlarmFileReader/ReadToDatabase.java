package com.shadley000.AlarmFileReader;

import com.shadley000.AlarmFileReader.exceptions.HeaderLineException;
import com.shadley000.AlarmFileReader.exceptions.LoadFileException;
import com.shadley000.AlarmFileReader.exceptions.FileFormatException;
import com.shadley000.AlarmFileReader.exceptions.InfoException;
import com.shadley000.AlarmFileReader.exceptions.NoTagException;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.shadley000.utils.AlarmsSQLConnect;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.Map;

public class ReadToDatabase
{
    
    public final static String sql_selectInstallationID = "SELECT INSTALLATION_ID from INSTALLATION where CLIENT_ID=? AND INSTALLATION_NAME = ?;";
    public final static String sql_selectVendorID = "SELECT VENDOR_ID from VENDOR where CLIENT_ID=? AND  INSTALLATION_ID = ?  AND VENDOR_NAME = ?;";
    public final static String sql_getFileID = "select FILE_ID from ALARM_FILE where CLIENT_ID=? AND  INSTALLATION_ID = ? AND VENDOR_ID = ? AND FILE_NAME = ?;";
    
    public final static String sql_removeOldFileFromDB = " DELETE from ALARM_FILE where CLIENT_ID=? AND INSTALLATION_ID = ? AND VENDOR_ID = ? AND FILE_ID = ?;";
    public final static String sql_removeOldDataFromDB = " DELETE from ALARM_DATA where CLIENT_ID=? AND INSTALLATION_ID = ? AND VENDOR_ID = ? AND FILE_ID = ?;";
    public final static String sql_removeOldErrorsFromDB = "DELETE from ERROR_LOG where CLIENT_ID=? AND INSTALLATION_ID = ? AND VENDOR_ID = ? AND FILE_ID = ?;";
    
    public final static String sql_createNewFileInDB = "insert into ALARM_FILE (CLIENT_ID, INSTALLATION_ID, VENDOR_ID, FILE_NAME, LOAD_DATE) "
            + "values (?,?,?,?,now());";
    public final static String sql_Update_File = "UPDATE ALARM_FILE SET DATA_LINES=?, DATA_INSERTED=?, DATA_SKIPPED=?, DATA_ERROR=? "
            + "WHERE CLIENT_ID=? AND INSTALLATION_ID = ? AND VENDOR_ID = ? AND FILE_ID = ?;";
    public final static String sql_InsertError = "insert into ERROR_LOG (CLIENT_ID, INSTALLATION_ID, VENDOR_ID, FILE_ID, ERROR_MESSAGE, LINENUM) "
            + "values (?,?,?,?,?,?);";
    
    public final static String sql_InsertAlarm = "INSERT INTO ALARM_DATA "
            + "(CLIENT_ID, INSTALLATION_ID, VENDOR_ID, FILE_ID, ALM_TIME, "
            + "ALM_ID, SOURCE_NODE, SYSTEM,  SUBSYSTEM, TAGNAME, "
            + "MSGTYPE, PRIORITY, ALM_STATUS, DESCRIPTION ) "
            + "VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?); ";
    
    AlarmsSQLConnect mySQLConnect = null;
    Connection connection = null;
    
    PreparedStatement stmt_getInstallationID = null;
    PreparedStatement stmt_getVendorID = null;
    PreparedStatement stmt_getFileID = null;
    
    PreparedStatement stmt_removeOldFileFromDB = null;
    PreparedStatement stmt_removeOldErrorsFromDB = null;
    PreparedStatement stmt_removeOldDataFromDB = null;
    
    PreparedStatement stmt_createNewFileInDB = null;
    PreparedStatement stmt_updateFileData = null;
    PreparedStatement stmt_logError = null;
    
    PreparedStatement stmt_InsertAlarm = null;
    
    static PrintStream skippedOut = null;
    static PrintStream errOut = null;
    static PrintStream noTagOut = null;
    
    public ReadToDatabase() throws SQLException
    {
        mySQLConnect = new AlarmsSQLConnect();
        connection = mySQLConnect.connect();
        
        stmt_getInstallationID = connection.prepareStatement(sql_selectInstallationID);
        stmt_getVendorID = connection.prepareStatement(sql_selectVendorID);
        stmt_getFileID = connection.prepareStatement(sql_getFileID);
        
        stmt_removeOldFileFromDB = connection.prepareStatement(sql_removeOldFileFromDB);
        stmt_removeOldDataFromDB = connection.prepareStatement(sql_removeOldDataFromDB);
        stmt_removeOldErrorsFromDB = connection.prepareStatement(sql_removeOldErrorsFromDB);
        
        stmt_updateFileData = connection.prepareStatement(sql_Update_File);
        stmt_createNewFileInDB = connection.prepareStatement(sql_createNewFileInDB);
        stmt_logError = connection.prepareStatement(sql_InsertError);
        
        stmt_InsertAlarm = connection.prepareStatement(sql_InsertAlarm);
        
    }
    
    public static void main(String args[])
    {
        if (args.length != 1)
        {
            System.out.println("USAGE " + ReadToDatabase.class.getName() + " directory");
            System.out.println("\tLoads alarm files from the directory's subdirectories");
            return;
        }
        String directory = args[0];
         System.out.println("Loading alarm files from directory "+directory);
        File InstallationsDirectory = new File(directory);
        
        ReadToDatabase reader = null;
        try
        {
            reader = new ReadToDatabase();
        } catch (SQLException e)
        {
            e.printStackTrace();
            return;
        }
        
        int clientID = 100;
        
        if (InstallationsDirectory.isDirectory() )
        {
            String[] installationFileList = InstallationsDirectory.list();
            
            for (int i = 0; i < installationFileList.length; i++)
            {
                File installationFile = new File(installationFileList[i]);
                if (installationFile.isDirectory())
                {
                    if (installationFile.getName().equals("skipped")||installationFile.getName().equals("lib"))
                    {
                        
                    } else                    
                    {
                        loadInstallationDirectory(reader, clientID, installationFile);
                    }
                }
            }
        }
    }
    
    public static void loadInstallationDirectory(ReadToDatabase reader, int clientID, File installationFile)
    {
        String installationName = installationFile.getName();
        try
        {
            int installationID = reader.getInstallationID(clientID, installationName);
            System.out.println("Loading Installation " + installationName + " " + installationID);
            String[] vendorFileList = installationFile.list();
            
            for (int v = 0; v < vendorFileList.length; v++)
            {
                File vendorFile = new File(installationFile.getAbsoluteFile() + "/" + vendorFileList[v]);
                //System.out.println("vendorFile.getName():"+vendorFile.getName());

                if (vendorFile.isDirectory())
                {
                    loadVendorDirectory(reader, clientID, installationID, vendorFile);
                }
                
            }
        } catch (Exception e)
        {
            System.out.println("invalid installation :" + installationName);
            e.printStackTrace(System.out);
        }
    }
    
    public static void loadVendorDirectory(ReadToDatabase reader, int clientID, int installationID, File vendorFile)
    {
        String vendorName = vendorFile.getName();
        
        try
        {
            int vendorID = reader.getVendorID(clientID, installationID, vendorName);
            System.out.println("Loading Vendor " + vendorName + " " + vendorID);
            
            String[] importFileList = vendorFile.list();
            for (int f = 0; f < importFileList.length; f++)
            {
                String fileName = importFileList[f];
                File importFile = new File(vendorFile.getAbsoluteFile() + "/" + fileName);
                
                if (importFile.isFile() && (fileName.endsWith(".csv") || fileName.endsWith(".CSV")))
                {
                    System.out.println("importing  " +  importFile.getAbsolutePath());
                    
                    try
                    {
                        reader.importToDatabase(clientID, installationID, vendorID, importFile.getAbsolutePath());
                        
                        Path source = FileSystems.getDefault().getPath(importFile.getParent(), fileName);
                        Path target = FileSystems.getDefault().getPath(importFile.getParent() + "/archive/", fileName);

                        //System.out.println("move " + source.toString() +" "+ target.toString());
                        Files.move(source, target, REPLACE_EXISTING);
                    } catch (FileFormatException | LoadFileException | IOException | SQLException e)
                    {
                        e.printStackTrace(System.out);
                    }
                    
                }
            }
        } catch (Exception ex)
        {
            System.out.println("invalid vendor :" + vendorName);
            ex.printStackTrace(System.out);
        }
    }
    
    public void importToDatabase(int clientID, int installationID, int vendorID, String fileName) throws FileFormatException, FileNotFoundException, IOException, SQLException, LoadFileException
    {
        AlarmsSQLConnect mySQLConnect = new AlarmsSQLConnect();
        Connection connection = mySQLConnect.connect();
        
        removeOldFileFromDB(clientID, installationID, vendorID, fileName);
        
        File csvFile = new File(fileName);
        
        int lineNum = 0;
        int outputCount = 0;
        int exceptionCount = 0;
        int skippedCount = 0;

        // CSVReader reader = new CSVReader(new FileReader(csvFile));
        //String[] nextLine;
        BufferedReader reader = new BufferedReader(new FileReader(csvFile));
        String nextLine;
        
        Map<String, Integer> columnMap = null;
        
        int fileID = -1;
        
        while ((nextLine = reader.readLine()) != null)
        {
            CSVLine csvLine = new CSVLine(nextLine);
            if (columnMap == null)
            {
                try
                {
                    if (vendorID == 0)
                    {
                        columnMap = NOVLine.findHeaderFormat(csvLine);
                    } else if (vendorID == 1)
                    {
                        columnMap = KMLine.findHeaderFormat(csvLine);
                    }
                    fileID = createNewFileInDB(clientID, installationID, vendorID, csvFile.getName());
                    
                    try
                    {
                        skippedOut = new PrintStream(new FileOutputStream("skipped/" + csvFile.getName() + "_skipped_" + installationID + "_" + vendorID + "_" + fileID + ".csv", true));
                        noTagOut = new PrintStream(new FileOutputStream("skipped/" + csvFile.getName() + "_notag_" + installationID + "_" + vendorID + "_" + fileID + ".csv", true));
                        errOut = new PrintStream(new FileOutputStream("skipped/" + csvFile.getName() + "_err.csv", true));
                    } catch (IOException e)
                    {
                        e.printStackTrace(System.out);
                    }
                } catch (ParseException e)
                {
                    throw new FileFormatException("Fatal file format exception:" + e.getClass().getName() + " " + e.getMessage());
                }
            } else
            {
                AlarmRecord alarm = null;
                
                try
                {
                    if (vendorID == 0)
                    {
                        NOVLine novLine = new NOVLine();
                        alarm = novLine.parse(columnMap, csvLine);
                    } else if (vendorID == 1)
                    {
                        KMLine kmLine = new KMLine();
                        alarm = kmLine.parse(columnMap, csvLine);
                    }
                    
                    if (alarm != null)
                    {
                        alarm.setClientID(clientID);
                        alarm.setInstallationID(installationID);
                        alarm.setVendorID(vendorID);
                        alarm.setFileID(fileID);
                        alarm.fillPreparedStatement(stmt_InsertAlarm);
                        stmt_InsertAlarm.execute();
                        outputCount++;
                    }
                    
                } catch (HeaderLineException e)
                {
                    //ignore
                } catch (InfoException e)
                {
                    skippedCount++;
                    skippedOut.println(clientID + "," + installationID + "," + vendorID + "," + fileID + "," + csvLine.toString());
                } catch (NoTagException e)
                {
                    skippedCount++;
                    noTagOut.println(clientID + "," + installationID + "," + vendorID + "," + fileID + "," + csvLine.toString());
                } catch (ParseException e)
                {
                    exceptionCount++;
                    logException(clientID, installationID, vendorID, fileID, e.getClass().getName() + " " + e.getMessage(), lineNum);
                    
                    errOut.println(clientID + "," + installationID + "," + vendorID + "," + fileID + "," + csvLine.toString());
                } catch (SQLException e)
                {	//System.out.println("Description:"+alarm.getDescription());
                    if (alarm != null)
                    {
                        System.out.println(alarm.toString());
                    }
                    throw e;
                }
            }
            
            lineNum++;
        }
        reader.close();
        
        updateFileData(clientID, installationID, vendorID, fileID, lineNum, outputCount, skippedCount, exceptionCount);
        
        connection.close();
        mySQLConnect.disconnect();
        
        skippedOut.flush();
        noTagOut.flush();
        errOut.flush();
        skippedOut.close();
        noTagOut.close();
        errOut.close();
        System.out.println(outputCount+" records inserted, "+skippedCount+" skipped "+exceptionCount+" errors");
    }
    
    int getInstallationID(int clientID, String installationName) throws SQLException, Exception
    {
        stmt_getInstallationID.setInt(1, clientID);
        stmt_getInstallationID.setString(2, installationName);
        ResultSet rs = stmt_getInstallationID.executeQuery();
        if (rs.next())
        {
            int fileID = rs.getInt(1);
            return fileID;
        }
        throw new Exception("Unknown Installation:" + installationName);
    }
    
    int getVendorID(int clientID, int installationID, String vendorName) throws SQLException, Exception
    {
        stmt_getVendorID.setInt(1, clientID);
        stmt_getVendorID.setInt(2, installationID);
        stmt_getVendorID.setString(3, vendorName);
        ResultSet rs = stmt_getVendorID.executeQuery();
        if (rs.next())
        {
            int fileID = rs.getInt(1);
            return fileID;
        }
        throw new Exception("Unknown vendor type:" + vendorName);
    }
    
    public void removeOldFileFromDB(int clientID, int installationID, int vendorID, String filename) throws SQLException
    {
        stmt_getFileID.setInt(1, clientID);
        stmt_getFileID.setInt(2, installationID);
        stmt_getFileID.setInt(3, vendorID);
        stmt_getFileID.setString(4, filename);
        ResultSet rs = stmt_getFileID.executeQuery();
        
        if (rs.next())
        {
            int fileID = rs.getInt(1);
            
            stmt_removeOldDataFromDB.setInt(1, clientID);
            stmt_removeOldDataFromDB.setInt(2, installationID);
            stmt_removeOldDataFromDB.setInt(3, vendorID);
            stmt_removeOldDataFromDB.setInt(4, fileID);
            stmt_removeOldDataFromDB.execute();
            
            stmt_removeOldErrorsFromDB.setInt(1, clientID);
            stmt_removeOldErrorsFromDB.setInt(2, installationID);
            stmt_removeOldErrorsFromDB.setInt(3, vendorID);
            stmt_removeOldErrorsFromDB.setInt(4, fileID);
            stmt_removeOldErrorsFromDB.execute();
            
            stmt_removeOldFileFromDB.setInt(1, clientID);
            stmt_removeOldFileFromDB.setInt(2, installationID);
            stmt_removeOldFileFromDB.setInt(3, vendorID);
            stmt_removeOldFileFromDB.setInt(4, fileID);
            stmt_removeOldFileFromDB.execute();
        }
    }
    
    public int createNewFileInDB(int clientID, int installationID, int vendorID, String filename) throws SQLException, LoadFileException
    {
        stmt_createNewFileInDB.setInt(1, clientID);
        stmt_createNewFileInDB.setInt(2, installationID);
        stmt_createNewFileInDB.setInt(3, vendorID);
        stmt_createNewFileInDB.setString(4, filename);
        stmt_createNewFileInDB.execute();
        
        stmt_getFileID.setInt(1, clientID);
        stmt_getFileID.setInt(2, installationID);
        stmt_getFileID.setInt(3, vendorID);
        stmt_getFileID.setString(4, filename);
        ResultSet rs = stmt_getFileID.executeQuery();
        
        if (rs.next())
        {
            return rs.getInt(1);
        } else
        {
            throw new LoadFileException("Unable to create fileID for  " + filename);
        }
    }
    
    public void updateFileData(int clientID, int installationID, int vendorID, int file_id, int lines, int inserted, int skipped, int errors) throws SQLException
    {
        stmt_updateFileData.setInt(1, lines);
        stmt_updateFileData.setInt(2, inserted);
        stmt_updateFileData.setInt(3, skipped);
        stmt_updateFileData.setInt(4, errors);
        
        stmt_updateFileData.setInt(5, clientID);
        stmt_updateFileData.setInt(6, installationID);
        stmt_updateFileData.setInt(7, vendorID);
        stmt_updateFileData.setInt(8, file_id);
        
        stmt_updateFileData.execute();
    }
    
    private void logException(int clientID, int installationID, int vendorID, int fileID, String message, int lineNum) throws SQLException
    {
        if (message != null && message.length() > 512)
        {
            message = message.substring(0, 511);
        }
        stmt_logError.setInt(1, clientID);
        stmt_logError.setInt(2, installationID);
        stmt_logError.setInt(3, vendorID);
        stmt_logError.setInt(4, fileID);
        stmt_logError.setString(5, message);
        stmt_logError.setInt(6, lineNum);
        stmt_logError.execute();
    }
}
