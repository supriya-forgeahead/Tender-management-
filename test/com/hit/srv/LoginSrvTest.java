package com.hit.srv;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.hit.utility.DBUtil;

public class LoginSrvTest {
    
    private LoginSrv loginSrv;
    private MockedStatic<DBUtil> mockedDBUtil;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private HttpSession session;
    
    @Mock
    private RequestDispatcher dispatcher;
    
    @Mock
    private Connection connection;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private ResultSet resultSet;
    
    private StringWriter stringWriter;
    private PrintWriter writer;

    @Before
    public void setUp() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        loginSrv = new LoginSrv();
        
        // Setup response writer
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        
        // Configure basic mock behaviors
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        
        // Initialize static mock for DBUtil
        mockedDBUtil = mockStatic(DBUtil.class);
        mockedDBUtil.when(DBUtil::provideConnection).thenReturn(connection);
        
        // Setup database mocks
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @After
    public void tearDown() {
        if (writer != null) {
            writer.close();
        }
        if (mockedDBUtil != null) {
            mockedDBUtil.close();
        }
    }

    /**
     * Test successful admin login
     */
    @Test
    public void testSuccessfulAdminLogin() throws ServletException, IOException {
        // Setup admin credentials
        when(request.getParameter("username")).thenReturn("Admin");
        when(request.getParameter("password")).thenReturn("Admin");
        when(request.getParameter("user")).thenReturn("Login as Admin");
        
        loginSrv.doPost(request, response);
        
        verify(session).setAttribute("user", "admin");
        verify(session).setAttribute("username", "Admin");
        verify(session).setAttribute("password", "Admin");
        verify(request).getRequestDispatcher("adminHome.jsp");
    }

    /**
     * Test failed admin login
     */
    @Test
    public void testFailedAdminLogin() throws ServletException, IOException {
        when(request.getParameter("username")).thenReturn("wrongAdmin");
        when(request.getParameter("password")).thenReturn("wrongPass");
        when(request.getParameter("user")).thenReturn("Login as Admin");
        
        loginSrv.doPost(request, response);
        
        verify(request).getRequestDispatcher("login.jsp");
        verify(dispatcher).include(request, response);
    }

    /**
     * Test successful vendor login with ID
     */
    @Test
    public void testSuccessfulVendorLoginWithId() throws Exception {
        // Setup vendor credentials
        String vendorId = "V001";
        when(request.getParameter("username")).thenReturn(vendorId);
        when(request.getParameter("password")).thenReturn("password");
        when(request.getParameter("user")).thenReturn("Login as Vendor");
        
        // Mock database response
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("vid")).thenReturn(vendorId);
        when(resultSet.getString("vname")).thenReturn("Test Vendor");
        when(resultSet.getString("vemail")).thenReturn("vendor@test.com");
        when(resultSet.getString("address")).thenReturn("Test Address");
        when(resultSet.getString("company")).thenReturn("Test Company");
        when(resultSet.getString("vmob")).thenReturn("1234567890");
        
        loginSrv.doPost(request, response);
        
        verify(session).setAttribute("user", "user");
        verify(session).setAttribute("username", vendorId);
        verify(request).getRequestDispatcher("vendorHome.jsp");
    }

    /**
     * Test successful vendor login with email
     */
    @Test
    public void testSuccessfulVendorLoginWithEmail() throws Exception {
        String email = "vendor@test.com";
        when(request.getParameter("username")).thenReturn(email);
        when(request.getParameter("password")).thenReturn("password");
        when(request.getParameter("user")).thenReturn("Login as Vendor");
        
        // First query returns false (ID check fails)
        // Second query returns true (email check succeeds)
        when(resultSet.next()).thenReturn(false).thenReturn(true);
        
        when(resultSet.getString("vid")).thenReturn("V001");
        when(resultSet.getString("vname")).thenReturn("Test Vendor");
        when(resultSet.getString("vemail")).thenReturn(email);
        when(resultSet.getString("address")).thenReturn("Test Address");
        when(resultSet.getString("company")).thenReturn("Test Company");
        when(resultSet.getString("vmob")).thenReturn("1234567890");
        
        loginSrv.doPost(request, response);
        
        verify(session).setAttribute("user", "user");
        verify(session).setAttribute("username", email);
        verify(request).getRequestDispatcher("vendorHome.jsp");
    }

    /**
     * Test failed vendor login
     */
    @Test
    public void testFailedVendorLogin() throws Exception {
        when(request.getParameter("username")).thenReturn("wrongVendor");
        when(request.getParameter("password")).thenReturn("wrongPass");
        when(request.getParameter("user")).thenReturn("Login as Vendor");
        
        when(resultSet.next()).thenReturn(false);
        
        loginSrv.doPost(request, response);
        
        verify(request).getRequestDispatcher("login.jsp");
        verify(dispatcher).include(request, response);
    }

    /**
     * Test GET method
     */
    @Test
    public void testDoGet() throws ServletException, IOException {
        loginSrv.doGet(request, response);
        verify(request, times(1)).getParameter("username");
    }
}
