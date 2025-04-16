package com.hit.dao;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
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

import com.hit.beans.VendorBean;
import com.hit.utility.DBUtil;

/**
 * Unit tests for VendorDaoImpl
 * Tests all vendor management operations and security aspects
 */
public class VendorDaoImplTest {

    private VendorDaoImpl vendorDao;
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
        vendorDao = new VendorDaoImpl();
        
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
     * Test successful vendor registration
     */
    @Test
    public void testRegisterVendorSuccess() throws SQLException {
        // Setup
        VendorBean vendor = createTestVendor();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Email not already registered
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = vendorDao.registerVendor(vendor);
        
        // Verify
        assertTrue(result.contains("Registration Successful"));
        verify(preparedStatement).setString(1, vendor.getEmail());
    }

    /**
     * Test vendor registration with duplicate email
     */
    @Test
    public void testRegisterVendorDuplicateEmail() throws SQLException {
        // Setup
        VendorBean vendor = createTestVendor();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // Email already exists
        
        // Execute test
        String result = vendorDao.registerVendor(vendor);
        
        // Verify
        assertTrue(result.contains("Email Id already Registered"));
    }

    /**
     * Test getting all vendors
     */
    @Test
    public void testGetAllVendors() throws SQLException {
        // Setup
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false); // Two vendors
        when(resultSet.getString("vid")).thenReturn("V001", "V002");
        when(resultSet.getString("vname")).thenReturn("Vendor 1", "Vendor 2");
        when(resultSet.getString("vmob")).thenReturn("1234567890", "0987654321");
        when(resultSet.getString("vemail")).thenReturn("vendor1@test.com", "vendor2@test.com");
        when(resultSet.getString("address")).thenReturn("Address 1", "Address 2");
        when(resultSet.getString("company")).thenReturn("Company 1", "Company 2");
        when(resultSet.getString("password")).thenReturn("pass1", "pass2");
        
        // Execute test
        List<VendorBean> vendors = vendorDao.getAllVendors();
        
        // Verify
        assertEquals(2, vendors.size());
        assertEquals("V001", vendors.get(0).getId());
        assertEquals("V002", vendors.get(1).getId());
    }

    /**
     * Test password validation
     */
    @Test
    public void testValidatePassword() throws SQLException {
        // Setup
        String vendorId = "V001";
        String password = "password123";
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        
        // Execute test
        boolean result = vendorDao.validatePassword(vendorId, password);
        
        // Verify
        assertTrue(result);
        verify(preparedStatement).setString(1, vendorId);
        verify(preparedStatement).setString(2, password);
    }

    /**
     * Test updating vendor profile
     */
    @Test
    public void testUpdateProfile() throws SQLException {
        // Setup
        VendorBean vendor = createTestVendor();
        // Mock password validation
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = vendorDao.updateProfile(vendor);
        
        // Verify
        assertTrue(result.contains("Updated Successfully"));
    }

    /**
     * Test changing vendor password
     */
    @Test
    public void testChangePassword() throws SQLException {
        // Setup
        String vendorId = "V001";
        String oldPassword = "oldPass";
        String newPassword = "newPass";
        
        // Mock password validation
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = vendorDao.changePassword(vendorId, oldPassword, newPassword);
        
        // Verify
        assertTrue(result.contains("Updated Successfully"));
    }

    /**
     * Test getting vendor data by ID
     */
    @Test
    public void testGetVendorDataById() throws SQLException {
        // Setup
        String vendorId = "V001";
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("vid")).thenReturn(vendorId);
        when(resultSet.getString("vname")).thenReturn("Test Vendor");
        when(resultSet.getString("vmob")).thenReturn("1234567890");
        when(resultSet.getString("vemail")).thenReturn("vendor@test.com");
        when(resultSet.getString("address")).thenReturn("Test Address");
        when(resultSet.getString("company")).thenReturn("Test Company");
        when(resultSet.getString("password")).thenReturn("password");
        
        // Execute test
        VendorBean vendor = vendorDao.getVendorDataById(vendorId);
        
        // Verify
        assertNotNull(vendor);
        assertEquals(vendorId, vendor.getId());
    }

    /**
     * Test SQL injection prevention in password validation
     */
    @Test
    public void testPasswordValidationSQLInjection() throws SQLException {
        // Setup malicious input
        String vendorId = "V001";
        String maliciousPassword = "' OR '1'='1";
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        
        // Execute test
        boolean result = vendorDao.validatePassword(vendorId, maliciousPassword);
        
        // Verify
        assertFalse(result);
        verify(preparedStatement).setString(2, maliciousPassword);
    }

    /**
     * Helper method to create a test vendor
     */
    private VendorBean createTestVendor() {
        VendorBean vendor = new VendorBean();
        vendor.setId("V001");
        vendor.setName("Test Vendor");
        vendor.setMobile("1234567890");
        vendor.setEmail("vendor@test.com");
        vendor.setCompany("Test Company");
        vendor.setAddress("Test Address");
        vendor.setPassword("password123");
        return vendor;
    }
}
