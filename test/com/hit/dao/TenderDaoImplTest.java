package com.hit.dao;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.hit.beans.TenderBean;
import com.hit.beans.TenderStatusBean;
import com.hit.utility.DBUtil;

/**
 * Unit tests for TenderDaoImpl
 * Tests all CRUD operations and tender management functionality
 */
public class TenderDaoImplTest {

    private TenderDaoImpl tenderDao;
    private MockedStatic<DBUtil> mockedDBUtil;

    @Mock
    private Connection connection;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private ResultSet resultSet;

    /**
     * Setup method runs before each test
     */
    @Before
    public void setUp() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        tenderDao = new TenderDaoImpl();
        
        // Setup static mock for DBUtil
        mockedDBUtil = mockStatic(DBUtil.class);
        mockedDBUtil.when(DBUtil::provideConnection).thenReturn(connection);
        
        // Configure default behavior
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    /**
     * Cleanup method runs after each test
     */
    @After
    public void tearDown() {
        mockedDBUtil.close();
    }

    /**
     * Test getting tender details by ID
     */
    @Test
    public void testGetTenderDetailsById() throws SQLException {
        // Setup
        String tenderId = "TENDER001";
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn(tenderId);
        when(resultSet.getString(2)).thenReturn("Test Tender");
        when(resultSet.getString(3)).thenReturn("Construction");
        when(resultSet.getInt(4)).thenReturn(1000000);
        when(resultSet.getString(5)).thenReturn("Test Description");
        when(resultSet.getString(6)).thenReturn("2024-12-31");
        when(resultSet.getString(7)).thenReturn("Test Location");
        
        // Execute test
        List<TenderBean> tenders = tenderDao.getTenderDetails(tenderId);
        
        // Verify
        assertFalse(tenders.isEmpty());
        assertEquals(tenderId, tenders.get(0).getId());
        verify(preparedStatement).setString(1, tenderId);
    }

    /**
     * Test getting all tenders
     */
    @Test
    public void testGetAllTenders() throws SQLException {
        // Setup
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("tid")).thenReturn("TENDER001", "TENDER002");
        when(resultSet.getString("tname")).thenReturn("Tender 1", "Tender 2");
        when(resultSet.getString("ttype")).thenReturn("Type 1", "Type 2");
        when(resultSet.getInt("tprice")).thenReturn(1000, 2000);
        when(resultSet.getString("tdesc")).thenReturn("Desc 1", "Desc 2");
        when(resultSet.getDate(6)).thenReturn(new Date(System.currentTimeMillis()));
        when(resultSet.getString("tloc")).thenReturn("Loc 1", "Loc 2");
        
        // Execute test
        List<TenderBean> tenders = tenderDao.getAllTenders();
        
        // Verify
        assertEquals(2, tenders.size());
    }

    /**
     * Test creating a new tender
     */
    @Test
    public void testCreateTender() throws SQLException {
        // Setup
        TenderBean tender = new TenderBean();
        tender.setId("TENDER001");
        tender.setName("Test Tender");
        tender.setType("Construction");
        tender.setPrice(1000000);
        tender.setDesc("Test Description");
        tender.setDeadline(new java.util.Date());
        tender.setLocation("Test Location");
        
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = tenderDao.createTender(tender);
        
        // Verify
        assertTrue(result.contains("New Tender Inserted"));
        verify(preparedStatement).setString(1, tender.getId());
    }

    /**
     * Test removing a tender
     */
    @Test
    public void testRemoveTender() throws SQLException {
        // Setup
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        boolean result = tenderDao.removeTender("TENDER001");
        
        // Verify
        assertTrue(result);
        verify(preparedStatement).setString(1, "TENDER001");
    }

    /**
     * Test updating tender details
     */
    @Test
    public void testUpdateTender() throws SQLException {
        // Setup
        TenderBean tender = new TenderBean();
        tender.setId("TENDER001");
        tender.setName("Updated Tender");
        tender.setType("Updated Type");
        tender.setPrice(2000000);
        tender.setDesc("Updated Description");
        tender.setDeadline(new java.util.Date());
        tender.setLocation("Updated Location");
        
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = tenderDao.updateTender(tender);
        
        // Verify
        assertTrue(result.contains("UPDATED SUCCSESFULLY"));
    }

    /**
     * Test getting tender status
     */
    @Test
    public void testGetTenderStatus() throws SQLException {
        // Setup
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        
        // Execute test
        String status = tenderDao.getTenderStatus("TENDER001");
        
        // Verify
        assertEquals("Assigned", status);
    }

    /**
     * Test assigning tender to vendor
     */
    @Test
    public void testAssignTender() throws SQLException {
        // Setup
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = tenderDao.assignTender("TENDER001", "VENDOR001", "BID001");
        
        // Verify
        assertTrue(result.contains("has been Assigned"));
    }

    /**
     * Test getting all assigned tenders
     */
    @Test
    public void testGetAllAssignedTenders() throws SQLException {
        // Setup
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("tid")).thenReturn("TENDER001");
        when(resultSet.getString("bid")).thenReturn("BID001");
        when(resultSet.getString("status")).thenReturn("Assigned");
        when(resultSet.getString("vid")).thenReturn("VENDOR001");
        
        // Execute test
        List<TenderStatusBean> statusList = tenderDao.getAllAssignedTenders();
        
        // Verify
        assertFalse(statusList.isEmpty());
        assertEquals("TENDER001", statusList.get(0).getTendorId()); // Changed to getTendorId()
    }

    /**
     * Test SQL injection prevention in tender search
     */
    @Test
    public void testGetTenderDetailsWithSQLInjection() throws SQLException {
        // Setup malicious input
        String maliciousInput = "' OR '1'='1";
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        
        // Execute test
        tenderDao.getTenderDetails(maliciousInput);
        
        // Verify PreparedStatement is used (preventing SQL injection)
        verify(preparedStatement).setString(1, maliciousInput);
    }

    /**
     * Test error handling in tender creation
     */
    @Test
    public void testCreateTenderWithError() throws SQLException {
        // Setup
        TenderBean tender = new TenderBean();
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));
        
        // Execute test
        String result = tenderDao.createTender(tender);
        
        // Verify error handling
        assertTrue(result.contains("Error"));
    }
}
