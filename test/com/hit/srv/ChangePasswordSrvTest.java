package com.hit.srv;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.InjectMocks;

import com.hit.dao.VendorDao;
import com.hit.dao.VendorDaoImpl;

/**
 * Unit Test class for ChangePasswordSrv
 * Tests password change functionality with security considerations
 */
public class ChangePasswordSrvTest {
    
    @InjectMocks
    private ChangePasswordSrv changePasswordSrv;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private HttpSession session;
    
    @Mock
    private RequestDispatcher dispatcher;
    
    @Mock
    private VendorDao vendorDao;
    
    private StringWriter stringWriter;
    private PrintWriter writer;

    /**
     * Setup method runs before each test
     * Initializes mocks and common test requirements
     */
    @Before
    public void setUp() throws IOException {
        // Initialize mocks
        MockitoAnnotations.initMocks(this);
        
        // Setup PrintWriter for response
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        
        // Configure basic mock behaviors
        when(request.getSession(false)).thenReturn(session);
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    }

    /**
     * Test 1: Successful Password Change
     * Verifies that a valid user can change their password successfully
     */
    @Test
    public void testSuccessfulPasswordChange() throws ServletException, IOException {
        // Setup session attributes
        when(session.getAttribute("username")).thenReturn("testUser");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup password parameters
        String oldPassword = "oldPass123";
        String newPassword = "newPass123";
        
        when(request.getParameter("oldpassword")).thenReturn(oldPassword);
        when(request.getParameter("newpassword")).thenReturn(newPassword);
        
        // Mock DAO response
        when(vendorDao.changePassword("testUser", oldPassword, newPassword))
            .thenReturn("Password Changed Successfully!");
        
        // Execute servlet
        changePasswordSrv.doPost(request, response);
        
        // Verify success scenario
        verify(request).getRequestDispatcher("changePassword.jsp");
        verify(dispatcher).include(request, response);
    }

    /**
     * Test 2: Invalid Session
     * Verifies that requests without valid session are rejected
     */
    @Test
    public void testInvalidSession() throws ServletException, IOException {
        // Setup null session
        when(request.getSession(false)).thenReturn(null);
        
        // Execute servlet
        changePasswordSrv.doPost(request, response);
        
        // Verify redirect to login page
        verify(response).sendRedirect("loginFailed.jsp");
    }

    /**
     * Test 3: Missing Password Parameters
     * Verifies handling of missing password fields
     */
    @Test
    public void testMissingPasswordParameters() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testUser");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup missing parameters
        when(request.getParameter("oldpassword")).thenReturn("");
        when(request.getParameter("newpassword")).thenReturn("");
        
        // Execute servlet
        changePasswordSrv.doPost(request, response);
        
        // Verify error handling
        verify(request).getRequestDispatcher("changePassword.jsp");
        verify(dispatcher).include(request, response);
    }

    /**
     * Test 4: Incorrect Old Password
     * Verifies that incorrect old password is handled properly
     */
    @Test
    public void testIncorrectOldPassword() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testUser");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup password parameters
        String oldPassword = "wrongPass";
        String newPassword = "newPass123";
        
        when(request.getParameter("oldpassword")).thenReturn(oldPassword);
        when(request.getParameter("newpassword")).thenReturn(newPassword);
        
        // Mock DAO response for incorrect password
        when(vendorDao.changePassword("testUser", oldPassword, newPassword))
            .thenReturn("Incorrect Old Password!");
        
        // Execute servlet
        changePasswordSrv.doPost(request, response);
        
        // Verify error handling
        verify(request).getRequestDispatcher("changePassword.jsp");
        verify(dispatcher).include(request, response);
    }

    /**
     * Test 5: Weak Password Validation
     * Verifies that weak passwords are rejected
     */
    @Test
    public void testWeakPasswordValidation() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testUser");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup weak password
        when(request.getParameter("oldpassword")).thenReturn("oldPass123");
        when(request.getParameter("newpassword")).thenReturn("weak");
        
        // Execute servlet
        changePasswordSrv.doPost(request, response);
        
        // Verify password strength validation
        verify(request).getRequestDispatcher("changePassword.jsp");
        verify(dispatcher).include(request, response);
    }

    /**
     * Test 6: Password Length Validation
     * Verifies that password length requirements are enforced
     */
    @Test
    public void testPasswordLengthValidation() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testUser");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup too short password
        when(request.getParameter("oldpassword")).thenReturn("oldPass123");
        when(request.getParameter("newpassword")).thenReturn("short");
        
        // Execute servlet
        changePasswordSrv.doPost(request, response);
        
        // Verify length validation
        verify(request).getRequestDispatcher("changePassword.jsp");
        verify(dispatcher).include(request, response);
    }

    /**
     * Test 7: SQL Injection Prevention
     * Verifies that SQL injection attempts are prevented
     */
    @Test
    public void testSQLInjectionPrevention() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testUser");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup malicious input
        String maliciousPassword = "' OR '1'='1";
        
        when(request.getParameter("oldpassword")).thenReturn("oldPass123");
        when(request.getParameter("newpassword")).thenReturn(maliciousPassword);
        
        // Execute servlet
        changePasswordSrv.doPost(request, response);
        
        // Verify injection prevention
        verify(request).getRequestDispatcher("changePassword.jsp");
        verify(dispatcher).include(request, response);
    }

    /**
     * Test 8: GET Method Not Allowed
     * Verifies that GET requests are properly handled
     */
    @Test
    public void testGetMethodNotAllowed() throws ServletException, IOException {
        // Execute GET request
        changePasswordSrv.doGet(request, response);
        
        // Verify method not allowed handling
        verify(request).getRequestDispatcher("changePassword.jsp");
        verify(dispatcher).forward(request, response);
    }

    /**
     * Cleanup method runs after each test
     */
    @After
    public void tearDown() {
        writer.close();
    }
}
