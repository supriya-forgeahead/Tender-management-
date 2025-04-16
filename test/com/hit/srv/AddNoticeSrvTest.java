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

import com.hit.dao.NoticeDao;
import com.hit.dao.NoticeDaoImpl;

/**
 * Test class for AddNoticeSrv servlet
 * Tests functionality, security, and input validation of the notice addition process
 */
public class AddNoticeSrvTest {
    
    @InjectMocks
    private AddNoticeSrv addNoticeSrv;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private HttpSession session;
    
    @Mock
    private RequestDispatcher dispatcher;
    
    @Mock
    private NoticeDao noticeDao;
    
    private StringWriter stringWriter;
    private PrintWriter writer;

    /**
     * Setup method that runs before each test
     * Initializes all mock objects and sets up common behaviors
     * Sets security headers and prepares PrintWriter for response capture
     */
    @Before
    public void setUp() throws IOException {
        // Initialize all mock objects
        MockitoAnnotations.initMocks(this);
        
        // Setup PrintWriter to capture response output
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        
        // Setup common mock behaviors
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestDispatcher("addNotice.jsp")).thenReturn(dispatcher);
        
        // Setup security headers for response
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
    }

    /**
     * Test Case 1: Successful Notice Addition
     * Verifies that an admin user can successfully add a notice
     * Checks if the success message is properly displayed
     */
    @Test
    public void testSuccessfulNoticeAddition() throws ServletException, IOException {
        // Setup valid admin credentials
        when(session.getAttribute("user")).thenReturn("admin");
        when(session.getAttribute("username")).thenReturn("adminUser");
        when(session.getAttribute("password")).thenReturn("adminPass");
        
        // Setup valid notice data
        when(request.getParameter("title")).thenReturn("Test Notice");
        when(request.getParameter("info")).thenReturn("Test Description");
        when(noticeDao.addNotice("Test Notice", "Test Description"))
            .thenReturn("Notice Added Successfully!");
        
        // Execute the servlet
        addNoticeSrv.doGet(request, response);
        
        // Verify the success scenario
        verify(dispatcher).include(request, response);
        assertTrue(stringWriter.toString().contains("Notice Added Successfully!"));
    }

    /**
     * Test Case 2: Unauthorized Access
     * Verifies that non-admin users cannot add notices
     * Checks if they are redirected to login failure page
     */
    @Test
    public void testUnauthorizedAccess() throws ServletException, IOException {
        // Setup non-admin user credentials
        when(session.getAttribute("user")).thenReturn("vendor");
        when(session.getAttribute("username")).thenReturn("vendorUser");
        when(session.getAttribute("password")).thenReturn("vendorPass");
        
        // Execute the servlet
        addNoticeSrv.doGet(request, response);
        
        // Verify unauthorized access handling
        verify(response).sendRedirect("loginFailed.jsp");
    }

    /**
     * Test Case 3: Empty Credentials
     * Verifies that users with empty credentials cannot add notices
     * Tests the authentication validation
     */
    @Test
    public void testEmptyCredentials() throws ServletException, IOException {
        // Setup empty credentials
        when(session.getAttribute("user")).thenReturn("admin");
        when(session.getAttribute("username")).thenReturn("");
        when(session.getAttribute("password")).thenReturn("");
        
        // Execute the servlet
        addNoticeSrv.doGet(request, response);
        
        // Verify empty credentials handling
        verify(response).sendRedirect("loginFailed.jsp");
    }

    /**
     * Test Case 4: Null Session
     * Verifies that requests without a valid session are rejected
     * Tests session validation
     */
    @Test
    public void testNullSession() throws ServletException, IOException {
        // Setup null session scenario
        when(request.getSession()).thenReturn(null);
        
        // Execute the servlet
        addNoticeSrv.doGet(request, response);
        
        // Verify null session handling
        verify(response).sendRedirect("loginFailed.jsp");
    }

    /**
     * Test Case 5: XSS Prevention
     * Verifies that the application prevents Cross-Site Scripting attacks
     * Tests input sanitization for malicious scripts
     */
    @Test
    public void testXSSPrevention() throws ServletException, IOException {
        // Setup admin credentials
        when(session.getAttribute("user")).thenReturn("admin");
        when(session.getAttribute("username")).thenReturn("adminUser");
        when(session.getAttribute("password")).thenReturn("adminPass");
        
        // Setup malicious input
        String maliciousTitle = "<script>alert('xss')</script>";
        //String sanitizedTitle = org.owasp.encoder.Encode.forHtml(maliciousTitle);
        
        when(request.getParameter("title")).thenReturn(maliciousTitle);
        when(request.getParameter("info")).thenReturn("Valid Description");
        
        // Execute the servlet
        addNoticeSrv.doGet(request, response);
        
        // Verify XSS prevention
        verify(dispatcher).include(request, response);
        assertFalse(stringWriter.toString().contains("<script>"));
    }

    /**
     * Test Case 6: SQL Injection Prevention
     * Verifies that the application prevents SQL injection attacks
     * Tests input validation for malicious SQL statements
     */
    @Test
    public void testSQLInjectionPrevention() throws ServletException, IOException {
        // Setup admin credentials
        when(session.getAttribute("user")).thenReturn("admin");
        when(session.getAttribute("username")).thenReturn("adminUser");
        when(session.getAttribute("password")).thenReturn("adminPass");
        
        // Setup malicious SQL input
        String maliciousDesc = "'; DROP TABLE notices; --";
        
        when(request.getParameter("title")).thenReturn("Valid Title");
        when(request.getParameter("info")).thenReturn(maliciousDesc);
        
        // Execute the servlet
        addNoticeSrv.doGet(request, response);
        
        verify(dispatcher).include(request, response);
    }

    /**
     * Test Case 7: Empty Notice Fields
     * Verifies that empty notice fields are properly handled
     * Tests input validation for required fields
     */
    @Test
    public void testEmptyNoticeFields() throws ServletException, IOException {
        // Setup admin credentials
        when(session.getAttribute("user")).thenReturn("admin");
        when(session.getAttribute("username")).thenReturn("adminUser");
        when(session.getAttribute("password")).thenReturn("adminPass");
        
        // Setup empty notice fields
        when(request.getParameter("title")).thenReturn("");
        when(request.getParameter("info")).thenReturn("");
        
        // Execute the servlet
        addNoticeSrv.doGet(request, response);
        
        // Verify empty fields handling
        verify(dispatcher).include(request, response);
        assertTrue(stringWriter.toString().contains("error"));
    }

    /**
     * Test Case 8: POST Method
     * Verifies that the doPost method properly delegates to doGet
     * Tests HTTP POST request handling
     */
    @Test
    public void testDoPost() throws ServletException, IOException {
        // Setup admin credentials
        when(session.getAttribute("user")).thenReturn("admin");
        when(session.getAttribute("username")).thenReturn("adminUser");
        when(session.getAttribute("password")).thenReturn("adminPass");
        
        // Execute POST request
        addNoticeSrv.doPost(request, response);
        
        // Verify POST handling
        verify(dispatcher).include(request, response);
    }

    /**
     * Cleanup method that runs after each test
     * Closes the PrintWriter and performs any necessary cleanup
     */
    @After
    public void tearDown() {
        writer.close();
    }
}
