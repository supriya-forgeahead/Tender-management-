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

import com.hit.dao.BidderDao;
import com.hit.dao.BidderDaoImpl;

/**
 * Unit Test class for BidTenderSrv
 * Tests the tender bidding functionality including security aspects
 */
public class BidTenderSrvTest {
    
    @InjectMocks
    private BidTenderSrv bidTenderSrv;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private HttpSession session;
    
    @Mock
    private RequestDispatcher dispatcher;
    
    @Mock
    private BidderDao bidderDao;
    
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
     * Test 1: Successful Bid Submission
     * Verifies that a vendor can successfully submit a bid
     */
    @Test
    public void testSuccessfulBidSubmission() throws ServletException, IOException {
        // Setup session attributes for vendor
        when(session.getAttribute("username")).thenReturn("testVendor");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup bid parameters
        String tid = "T123";
        String vid = "V456";
        String bidAmount = "5000";
        String deadline = "2024-12-31";
        
        when(request.getParameter("tid")).thenReturn(tid);
        when(request.getParameter("vid")).thenReturn(vid);
        when(request.getParameter("bidamount")).thenReturn(bidAmount);
        when(request.getParameter("deadline")).thenReturn(deadline);
        
        // Mock DAO response
        when(bidderDao.bidTender(tid, vid, bidAmount, deadline))
            .thenReturn("Bid Submitted Successfully!");
        
        // Execute servlet
        bidTenderSrv.doPost(request, response);
        
        // Verify forward to success page
        verify(request).getRequestDispatcher("bidTender.jsp");
        verify(dispatcher).forward(request, response);
    }

    /**
     * Test 2: Unauthorized Access
     * Verifies that non-vendors cannot submit bids
     */
    @Test
    public void testUnauthorizedAccess() throws ServletException, IOException {
        // Setup session attributes for non-vendor
        when(session.getAttribute("username")).thenReturn("testAdmin");
        when(session.getAttribute("usertype")).thenReturn("admin");
        
        // Execute servlet
        bidTenderSrv.doPost(request, response);
        
        // Verify redirect to login failed page
        verify(response).sendRedirect("loginFailed.jsp");
    }

    /**
     * Test 3: Invalid Session
     * Verifies that requests without valid session are rejected
     */
    @Test
    public void testInvalidSession() throws ServletException, IOException {
        // Setup null session
        when(request.getSession(false)).thenReturn(null);
        
        // Execute servlet
        bidTenderSrv.doPost(request, response);
        
        // Verify redirect to login page
        verify(response).sendRedirect("loginFailed.jsp");
    }

    /**
     * Test 4: Missing Required Parameters
     * Verifies handling of missing bid parameters
     */
    @Test
    public void testMissingParameters() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testVendor");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup missing parameters
        when(request.getParameter("tid")).thenReturn(null);
        when(request.getParameter("bidamount")).thenReturn(null);
        
        // Execute servlet
        bidTenderSrv.doPost(request, response);
        
        // Verify error handling
        verify(request).getRequestDispatcher("bidTender.jsp");
        verify(dispatcher).forward(request, response);
    }

    /**
     * Test 5: Invalid Bid Amount
     * Verifies validation of bid amount
     */
    @Test
    public void testInvalidBidAmount() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testVendor");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup invalid bid amount
        String tid = "T123";
        String vid = "V456";
        String invalidBidAmount = "invalid";
        String deadline = "2024-12-31";
        
        when(request.getParameter("tid")).thenReturn(tid);
        when(request.getParameter("vid")).thenReturn(vid);
        when(request.getParameter("bidamount")).thenReturn(invalidBidAmount);
        when(request.getParameter("deadline")).thenReturn(deadline);
        
        when(bidderDao.bidTender(tid, vid, invalidBidAmount, deadline))
            .thenReturn("Invalid Bid Amount");
        
        // Execute servlet
        bidTenderSrv.doPost(request, response);
        
        // Verify error handling
        verify(request).getRequestDispatcher("bidTender.jsp");
        verify(dispatcher).forward(request, response);
    }

    /**
     * Test 6: Invalid Deadline Date
     * Verifies validation of deadline date
     */
    @Test
    public void testInvalidDeadline() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testVendor");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup invalid deadline
        String tid = "T123";
        String vid = "V456";
        String bidAmount = "5000";
        String invalidDeadline = "invalid-date";
        
        when(request.getParameter("tid")).thenReturn(tid);
        when(request.getParameter("vid")).thenReturn(vid);
        when(request.getParameter("bidamount")).thenReturn(bidAmount);
        when(request.getParameter("deadline")).thenReturn(invalidDeadline);
        
        when(bidderDao.bidTender(tid, vid, bidAmount, invalidDeadline))
            .thenReturn("Invalid Deadline Date");
        
        // Execute servlet
        bidTenderSrv.doPost(request, response);
        
        // Verify error handling
        verify(request).getRequestDispatcher("bidTender.jsp");
        verify(dispatcher).forward(request, response);
    }

    /**
     * Test 7: Duplicate Bid Check
     * Verifies that duplicate bids are handled properly
     */
    @Test
    public void testDuplicateBid() throws ServletException, IOException {
        // Setup valid session
        when(session.getAttribute("username")).thenReturn("testVendor");
        when(session.getAttribute("usertype")).thenReturn("vendor");
        
        // Setup bid parameters
        String tid = "T123";
        String vid = "V456";
        String bidAmount = "5000";
        String deadline = "2024-12-31";
        
        when(request.getParameter("tid")).thenReturn(tid);
        when(request.getParameter("vid")).thenReturn(vid);
        when(request.getParameter("bidamount")).thenReturn(bidAmount);
        when(request.getParameter("deadline")).thenReturn(deadline);
        
        // Mock DAO to simulate duplicate bid
        when(bidderDao.bidTender(tid, vid, bidAmount, deadline))
            .thenReturn("Duplicate Bid");
        
        // Execute servlet
        bidTenderSrv.doPost(request, response);
        
        // Verify duplicate handling
        verify(request).getRequestDispatcher("bidTender.jsp");
        verify(dispatcher).forward(request, response);
    }

    /**
     * Test 8: GET Method Not Allowed
     * Verifies that GET requests are properly handled
     */
    @Test
    public void testGetMethodNotAllowed() throws ServletException, IOException {
        // Execute GET request
        bidTenderSrv.doGet(request, response);
        
        // Verify method not allowed handling
        verify(request).getRequestDispatcher("bidTender.jsp");
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
