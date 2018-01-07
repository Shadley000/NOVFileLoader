/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.shadley000.AlarmFileReader;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author shadl
 */
public class AlarmRecord implements Serializable
{

    private int clientID;
    private int installationID;
    private int vendorID;
    private int fileID;
    private java.util.Date almTime;
    private String almID;
    private String sourceNode;
    private String system;
    private String subSystem;
    private String tagName;
    private String messageType;
    private String priority;
    private String status;
    private String description;

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(clientID + ", ");
        sb.append(installationID + ", ");
        sb.append(vendorID + ", ");
        sb.append(fileID + ", ");
        sb.append(almTime + ", ");
        sb.append(almID + ", ");
        sb.append(sourceNode + ", ");
        sb.append(system + ", ");
        sb.append(subSystem + ", ");
        sb.append(tagName + ", ");
        sb.append(messageType + ", ");
        sb.append(priority + ", ");
        sb.append(status + ", ");
        sb.append(description + ", ");
return sb.toString();
    }

    public AlarmRecord()
    {

    }

    public AlarmRecord(ResultSet rs) throws SQLException
    {
        loadData(rs);
    }

    public void fillPreparedStatement(PreparedStatement stmt) throws SQLException
    {
        stmt.setInt(1, clientID);
        stmt.setInt(2, installationID);
        stmt.setInt(3, vendorID);
        stmt.setInt(4, fileID);
        stmt.setTimestamp(5, new Timestamp(almTime.getTime()));
        stmt.setString(6, almID);
        stmt.setString(7, sourceNode);
        stmt.setString(8, system);
        stmt.setString(9, subSystem);
        stmt.setString(10, tagName);
        stmt.setString(11, messageType);
        stmt.setString(12, priority);
        stmt.setString(13, status);
        stmt.setString(14, description);
    }

    public void loadData(ResultSet rs) throws SQLException
    {
        clientID = rs.getInt(1);
        installationID = rs.getInt(2);
        vendorID = rs.getInt(3);
        fileID = rs.getInt(4);
        almTime = rs.getTimestamp(5);
        almID = rs.getString(6);
        sourceNode = rs.getString(7);
        system = rs.getString(8);
        subSystem = rs.getString(9);
        tagName = rs.getString(10);
        messageType = rs.getString(11);
        priority = rs.getString(12);
        status = rs.getString(13);
        description = rs.getString(14);
    }

    private String limitStringSize(String d, int length)
    {
        if (d != null && d.length() > length)
        {
            d = d.substring(length - 1);
        }
        return d;
    }

    public void setClientID(int i)
    {
        clientID = i;
    }

    public void setFileID(int i)
    {
        fileID = i;
    }

    public void setInstallationID(int i)
    {
        installationID = i;
    }

    public void setVendorID(int i)
    {
        vendorID = i;
    }

    public void setAlarmTime(java.util.Date d)
    {
        almTime = d;
    }

    public void setAlarmID(String s)
    {
        almID = limitStringSize(s, 32);
    }

    public void setSourceNode(String s)
    {
        sourceNode = limitStringSize(s, 32);
    }

    public void setSystem(String s)
    {
        system = limitStringSize(s, 64);
    }

    public void setSubSystem(String s)
    {
        subSystem = limitStringSize(s, 64);
    }

    public void setTagName(String s)
    {
        tagName = limitStringSize(s, 32);
    }

    public void setMessageType(String s)
    {
        messageType = limitStringSize(s, 64);
    }

    public void setPriority(String s)
    {
        priority = limitStringSize(s, 16);
    }

    public void setStatus(String s)
    {
        status = limitStringSize(s, 16);
    }

    public void setDescription(String s)
    {
        description = limitStringSize(s, 255);
    }

    public int getClientID()
    {
        return clientID;
    }

    public int getFileID()
    {
        return fileID;
    }

    public int getInstallationID()
    {
        return installationID;
    }

    public int getVendorID()
    {
        return vendorID;
    }

    public java.util.Date getAlarmTime()
    {
        return almTime;
    }

    public String getAlarmID()
    {
        return almID;
    }

    public String getSourceNode()
    {
        return sourceNode;
    }

    public String getSystem()
    {
        return system;
    }

    public String getSubSystem()
    {
        return subSystem;
    }

    public String getTagName()
    {
        return tagName;
    }

    public String getMessageType()
    {
        return messageType;
    }

    public String getPriority()
    {
        return priority;
    }

    public String getStatus()
    {
        return status;
    }

    public String getDescription()
    {
        return description;
    }

}
