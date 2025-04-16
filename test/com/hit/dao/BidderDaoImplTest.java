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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.hit.beans.BidderBean;
import com.hit.utility.DBUtil;
import com.hit.utility.IDUtil;

/**
 * Unit tests for BidderDaoImpl's acceptBid method
 */
public class BidderDaoImplTest {

    private BidderDaoImpl bidderDao;
    private MockedStatic<DBUtil> mockedDBUtil;

    @Mock
    private Connection connection;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private ResultSet resultSet;
    
    @Mock
    private TenderDao tenderDao;

    /**
     * Setup method runs before each test
     */
    @Before
    public void setUp() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        bidderDao = new BidderDaoImpl();
        
        // Setup static mock for DBUtil
        mockedDBUtil = mockStatic(DBUtil.class);
        mockedDBUtil.when(DBUtil::provideConnection).thenReturn(connection);
        
        // Configure default behavior
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @After
    public void tearDown() {
        mockedDBUtil.close();
    }


    /**
     * Test successful bid acceptance when tender is not already assigned
     */
    @Test
    public void testAcceptBidSuccessful() throws SQLException {
        // Setup test data
        String applicationId = "BID001";
        String tenderId = "TENDER001";
        String vendorId = "VENDOR001";

        // Mock the tender status check
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Tender not already assigned
        
        // Mock the bid status update
        when(preparedStatement.executeUpdate()).thenReturn(1); // Update successful
        
        // Execute test
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);
        
        // Verify the result
        assertTrue(result.contains("Bid Has Been Accepted Successfully"));
        
        // Verify interactions
        verify(preparedStatement).setString(1, tenderId); // Verify tender status check
        verify(preparedStatement).setString(1, "Accepted"); // Verify status update
        verify(preparedStatement).setString(2, applicationId);
        verify(preparedStatement).setString(3, "Pending");
    }

    /**
     * Test bid acceptance when tender is already assigned
     */
    @Test
    public void testAcceptBidTenderAlreadyAssigned() throws SQLException {
        // Setup
        String applicationId = "BID001";
        String tenderId = "TENDER001";
        String vendorId = "VENDOR001";

        // Mock tender status check to return already assigned
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // Tender already assigned
        
        // Execute test
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);
        
        // Verify
        assertEquals("Project Already Assigned", result);
        
        // Verify interactions
        verify(preparedStatement).setString(1, tenderId);
        verify(preparedStatement, never()).setString(1, "Accepted"); // Should not attempt to update
    }

    /**
     * Test bid acceptance with database error during status check
     */
    @Test
    public void testAcceptBidDatabaseErrorDuringStatusCheck() throws SQLException {
        // Setup
        String applicationId = "BID001";
        String tenderId = "TENDER001";
        String vendorId = "VENDOR001";

        // Mock database error during tender status check
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));
        
        // Execute test
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);
        
        // Verify
        assertTrue(result.contains("Bid Acceptance Failed"));
        assertTrue(result.contains("Error"));
    }

    /**
     * Test bid acceptance with database error during update
     */
    @Test
    public void testAcceptBidDatabaseErrorDuringUpdate() throws SQLException {
        // Setup
        String applicationId = "BID001";
        String tenderId = "TENDER001";
        String vendorId = "VENDOR001";

        // Mock successful tender status check
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        
        // Mock database error during update
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
        
        // Execute test
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);
        
        // Verify
        assertTrue(result.contains("Bid Acceptance Failed"));
        assertTrue(result.contains("Error"));
    }

    /**
     * Test bid acceptance with SQL injection attempt
     */
    @Test
    public void testAcceptBidWithSQLInjectionAttempt() throws SQLException {
        // Setup with malicious input
        String applicationId = "BID001'; DROP TABLE bidder; --";
        String tenderId = "TENDER001";
        String vendorId = "VENDOR001";

        // Mock normal behavior
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        bidderDao.acceptBid(applicationId, tenderId, vendorId);
        
        // Verify PreparedStatement is used (preventing SQL injection)
        verify(preparedStatement).setString(2, applicationId);
    }

    /**
     * Test bid acceptance with null parameters
     */
    /**
     * Test bid acceptance with null parameters
     * Verifies proper handling of null inputs and database interactions
     */
    @Test
    public void testAcceptBidWithNullParameters() throws SQLException {
        // Setup
        String applicationId = null;
        String tenderId = null;
        String vendorId = null;

        // Mock database interactions
        when(connection.prepareStatement("select * from tenderstatus where tid=?"))
            .thenReturn(preparedStatement);
        
        // Mock PreparedStatement behavior with null parameter
        doNothing().when(preparedStatement).setString(1, null);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Mock second PreparedStatement for update
        PreparedStatement updatePs = mock(PreparedStatement.class);
        when(connection.prepareStatement("update bidder set status = ? where bid=? and status=?"))
            .thenReturn(updatePs);
        
        // Mock update PreparedStatement behavior
        doNothing().when(updatePs).setString(1, "Accepted");
        doNothing().when(updatePs).setString(2, null);
        doNothing().when(updatePs).setString(3, "Pending");
        when(updatePs.executeUpdate()).thenReturn(0); // Assuming update fails due to null values

        // Execute test
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);

        // Verify the result
        assertTrue(result.contains("Bid Acceptance Failed"));

        // Verify all interactions
        verify(connection).prepareStatement("select * from tenderstatus where tid=?");
        verify(preparedStatement).setString(1, null);
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();

        // Verify proper resource cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)), times(1));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)), times(1));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(ResultSet.class)), times(1));

        // Verify no unexpected interactions
        verifyNoMoreInteractions(preparedStatement);
        verifyNoMoreInteractions(resultSet);
    }


    /**
     * Test bid acceptance with empty parameters
     */
    @Test
    public void testAcceptBidWithEmptyParameters() throws SQLException {
        // Setup
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test with empty strings
        String result = bidderDao.acceptBid("", "", "");
        
        // Verify
        assertTrue(result.contains("Bid Acceptance Failed") || result.contains("Error"));
    }

    /**
     * Test successful bid acceptance with boundary values
     */
    @Test
    public void testAcceptBidWithBoundaryValues() throws SQLException {
        // Setup with very long strings
        String applicationId = "B" + "1".repeat(254); // 255 characters
        String tenderId = "T" + "1".repeat(254);
        String vendorId = "V" + "1".repeat(254);

        // Mock successful responses
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);
        
        // Verify
        assertTrue(result.contains("Bid Has Been Accepted Successfully"));
    }
    
    /**
     * Test successful bid rejection
     */
    @Test
    public void testRejectBidSuccessful() throws SQLException {
        // Setup
        String applicationId = "BID001";
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Execute test
        String result = bidderDao.rejectBid(applicationId);

        // Verify result
        assertEquals("Bid Has Been Rejected Successfully!", result);

        // Verify interactions
        verify(connection).prepareStatement("update bidder set status = ? where bid=? and status = ?");
        verify(preparedStatement).setString(1, "Rejected");
        verify(preparedStatement).setString(2, applicationId);
        verify(preparedStatement).setString(3, "Pending");
        verify(preparedStatement).executeUpdate();

        // Verify resource cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
    }

    /**
     * Test bid rejection when no rows are updated
     */
    @Test
    public void testRejectBidNoRowsUpdated() throws SQLException {
        // Setup
        String applicationId = "BID001";
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // Execute test
        String result = bidderDao.rejectBid(applicationId);

        // Verify result
        assertEquals("Bid Rejection Failed", result);

        // Verify interactions
        verify(connection).prepareStatement("update bidder set status = ? where bid=? and status = ?");
        verify(preparedStatement).setString(1, "Rejected");
        verify(preparedStatement).setString(2, applicationId);
        verify(preparedStatement).setString(3, "Pending");
        verify(preparedStatement).executeUpdate();

        // Verify resource cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
    }

    /**
     * Test bid rejection with database error
     */
    @Test
    public void testRejectBidDatabaseError() throws SQLException {
        // Setup
        String applicationId = "BID001";
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Execute test
        String result = bidderDao.rejectBid(applicationId);

        // Verify result
        assertTrue(result.contains("Bid Rejection Failed"));
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("Database error"));

        // Verify interactions
        verify(connection).prepareStatement("update bidder set status = ? where bid=? and status = ?");
        verify(preparedStatement).setString(1, "Rejected");
        verify(preparedStatement).setString(2, applicationId);
        verify(preparedStatement).setString(3, "Pending");

        // Verify resource cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
    }

    /**
     * Test bid rejection with null application ID
     */
    @Test
    public void testRejectBidWithNullApplicationId() throws SQLException {
        // Setup
        String applicationId = null;
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // Execute test
        String result = bidderDao.rejectBid(applicationId);

        // Verify result
        assertEquals("Bid Rejection Failed", result);

        // Verify interactions
        verify(connection).prepareStatement("update bidder set status = ? where bid=? and status = ?");
        verify(preparedStatement).setString(1, "Rejected");
        verify(preparedStatement).setString(2, null);
        verify(preparedStatement).setString(3, "Pending");

        // Verify resource cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
    }

    /**
     * Test bid rejection with SQL injection attempt
     */
    @Test
    public void testRejectBidWithSQLInjectionAttempt() throws SQLException {
        // Setup
        String maliciousApplicationId = "BID001' OR '1'='1";
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // Execute test
        String result = bidderDao.rejectBid(maliciousApplicationId);

        // Verify result
        assertEquals("Bid Rejection Failed", result);

        // Verify interactions and SQL injection prevention
        verify(connection).prepareStatement("update bidder set status = ? where bid=? and status = ?");
        verify(preparedStatement).setString(1, "Rejected");
        verify(preparedStatement).setString(2, maliciousApplicationId);
        verify(preparedStatement).setString(3, "Pending");

        // Verify resource cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
    }

    /**
     * Test bid rejection with empty application ID
     */
    @Test
    public void testRejectBidWithEmptyApplicationId() throws SQLException {
        // Setup
        String applicationId = "";
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // Execute test
        String result = bidderDao.rejectBid(applicationId);

        // Verify result
        assertEquals("Bid Rejection Failed", result);

        // Verify interactions
        verify(connection).prepareStatement("update bidder set status = ? where bid=? and status = ?");
        verify(preparedStatement).setString(1, "Rejected");
        verify(preparedStatement).setString(2, "");
        verify(preparedStatement).setString(3, "Pending");

        // Verify resource cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
    }

    /**
     * Test bid rejection with connection failure
     */
    @Test
    public void testRejectBidConnectionFailure() {
        // Setup - Return null connection to simulate connection failure
        mockedDBUtil.when(DBUtil::provideConnection).thenReturn(null);

        // Execute test
        String result = bidderDao.rejectBid("BID001");

        // Verify result
        assertEquals("Bid Rejection Failed", result);

        // Verify no interactions with PreparedStatement
        verifyNoInteractions(preparedStatement);

        // Verify resource cleanup attempts
        mockedDBUtil.verify(() -> DBUtil.closeConnection((Connection) null));
        mockedDBUtil.verify(() -> DBUtil.closeConnection((PreparedStatement) null));
    }


    // Tests for bidTender method
    @Test
    public void testBidTenderSuccessful() throws SQLException {
        // Setup
        String tenderId = "T001";
        String vendorId = "V001";
        String bidAmount = "1000";
        String bidDeadline = "2024-12-31";
        String generatedBidId = "BID001";

        // Mock IDUtil using try-with-resources
        try (MockedStatic<IDUtil> mockedIDUtil = Mockito.mockStatic(IDUtil.class)) {
            mockedIDUtil.when(IDUtil::generateBidderId).thenReturn(generatedBidId);

            // Mock database operations
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            // Execute test
            String result = bidderDao.bidTender(tenderId, vendorId, bidAmount, bidDeadline);

            // Verify result
            assertEquals("You have successfully Bid for the tender", result);

            // Verify interactions
            verify(preparedStatement).setString(1, generatedBidId);
            verify(preparedStatement).setString(2, vendorId);
            verify(preparedStatement).setString(3, tenderId);
            verify(preparedStatement).setInt(4, Integer.parseInt(bidAmount));
            verify(preparedStatement).setString(6, "Pending");

            // Verify cleanup
            mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
            mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
        }
    }

    @Test
    public void testBidTenderDatabaseError() throws SQLException {
        // Setup
        String tenderId = "T001";
        String vendorId = "V001";
        String bidAmount = "1000";
        String bidDeadline = "2024-12-31";
        String generatedBidId = "BID001";

        try (MockedStatic<IDUtil> mockedIDUtil = Mockito.mockStatic(IDUtil.class)) {
            // Mock IDUtil
            mockedIDUtil.when(IDUtil::generateBidderId).thenReturn(generatedBidId);
            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

            // Execute test
            String result = bidderDao.bidTender(tenderId, vendorId, bidAmount, bidDeadline);

            // Verify result
            assertEquals("Tender Bidding Failed!", result);

            // Verify DBUtil method calls
            mockedDBUtil.verify(DBUtil::provideConnection);
            verify(connection).prepareStatement(anyString());
        }
    }

    // Tests for getAllBidsOfaTender method
    @Test
    public void testGetAllBidsOfaTenderSuccessful() throws SQLException {
        // Setup
        String tenderId = "T001";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false); // Two records
        
        // Mock ResultSet data
        when(resultSet.getInt("bidamount")).thenReturn(1000, 2000);
        when(resultSet.getDate("deadline")).thenReturn(new Date(System.currentTimeMillis()));
        when(resultSet.getString("bid")).thenReturn("BID001", "BID002");
        when(resultSet.getString("status")).thenReturn("Pending", "Pending");
        when(resultSet.getString("tid")).thenReturn(tenderId);
        when(resultSet.getString("vid")).thenReturn("V001", "V002");

        // Execute test
        List<BidderBean> result = bidderDao.getAllBidsOfaTender(tenderId);

        // Verify result
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("BID001", result.get(0).getBidId());
        assertEquals("BID002", result.get(1).getBidId());

        // Verify cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(ResultSet.class)));
    }

    @Test
    public void testGetAllBidsOfaTenderNoResults() throws SQLException {
        // Setup
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Execute test
        List<BidderBean> result = bidderDao.getAllBidsOfaTender("T001");

        // Verify result
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(ResultSet.class)));
    }

    // Tests for getAllBidsOfaVendor method
    @Test
    public void testGetAllBidsOfaVendorSuccessful() throws SQLException {
        // Setup
        String vendorId = "V001";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false); // Two records
        
        // Mock ResultSet data
        when(resultSet.getInt("bidamount")).thenReturn(1000, 2000);
        when(resultSet.getDate("deadline")).thenReturn(new Date(System.currentTimeMillis()));
        when(resultSet.getString("bid")).thenReturn("BID001", "BID002");
        when(resultSet.getString("status")).thenReturn("Pending", "Pending");
        when(resultSet.getString("tid")).thenReturn("T001", "T002");
        when(resultSet.getString("vid")).thenReturn(vendorId);

        // Execute test
        List<BidderBean> result = bidderDao.getAllBidsOfaVendor(vendorId);

        // Verify result
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(vendorId, result.get(0).getVendorId());
        assertEquals(vendorId, result.get(1).getVendorId());

        // Verify cleanup
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(Connection.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(PreparedStatement.class)));
        mockedDBUtil.verify(() -> DBUtil.closeConnection(any(ResultSet.class)));
    }

    @Test
    public void testGetAllBidsOfaVendorDatabaseError() throws SQLException {
        // Setup
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // Execute test
        List<BidderBean> result = bidderDao.getAllBidsOfaVendor("V001");

        // Verify result
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify interactions
        mockedDBUtil.verify(DBUtil::provideConnection);
        verify(connection).prepareStatement(anyString());

        // Verify cleanup - using times() to verify number of calls
        mockedDBUtil.verify(() -> DBUtil.closeConnection((Connection) null), times(1));
        mockedDBUtil.verify(() -> DBUtil.closeConnection((PreparedStatement) null), times(1));
        mockedDBUtil.verify(() -> DBUtil.closeConnection((ResultSet) null), times(1));
    }
    
    
    @Test
    public void testBidTenderWithNullConnection() throws SQLException {
        // Setup
        mockedDBUtil.when(DBUtil::provideConnection).thenReturn(null);

        // Execute test
        String result = bidderDao.bidTender("T001", "V001", "1000", "2024-12-31");

        // Verify result
        assertEquals("Tender Bidding Failed!", result);

        // Verify no interactions with PreparedStatement
        verifyNoInteractions(preparedStatement);
    }

    @Test
    public void testGetAllBidsOfaTenderWithNullConnection() {
        // Setup
        mockedDBUtil.when(DBUtil::provideConnection).thenReturn(null);

        // Execute test
        List<BidderBean> result = bidderDao.getAllBidsOfaTender("T001");

        // Verify result
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify no interactions with PreparedStatement
        verifyNoInteractions(preparedStatement);
    }

    @Test
    public void testGetAllBidsOfaVendorWithNullConnection() {
        // Setup
        mockedDBUtil.when(DBUtil::provideConnection).thenReturn(null);

        // Execute test
        List<BidderBean> result = bidderDao.getAllBidsOfaVendor("V001");

        // Verify result
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify no interactions with PreparedStatement
        verifyNoInteractions(preparedStatement);
    }
    
}
