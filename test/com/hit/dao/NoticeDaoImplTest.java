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

import com.hit.beans.NoticeBean;
import com.hit.utility.DBUtil;

/**
 * Unit tests for NoticeDaoImpl
 * Tests all CRUD operations and error scenarios
 */
public class NoticeDaoImplTest {

    private NoticeDaoImpl noticeDao;
    private MockedStatic<DBUtil> mockedDBUtil;

    @Mock
    private Connection connection;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private ResultSet resultSet;

    /**
     * Setup method runs before each test
     * Initializes mocks and common test requirements
     */
    @Before
    public void setUp() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        noticeDao = new NoticeDaoImpl();
        
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
     * Test successful notice removal
     */
    @Test
    public void testRemoveNoticeSuccess() throws SQLException {
        // Setup
        int noticeId = 1;
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = noticeDao.removeNotice(noticeId);
        
        // Verify
        assertTrue(result.contains("has been Removed Successfully"));
        verify(preparedStatement).setInt(1, noticeId);
    }

    /**
     * Test notice removal with database error
     */
    @Test
    public void testRemoveNoticeFailure() throws SQLException {
        // Setup database error
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));
        
        // Execute test
        String result = noticeDao.removeNotice(1);
        
        // Verify error handling
        assertTrue(result.contains("Error"));
    }

    /**
     * Test successful notice addition
     */
    @Test
    public void testAddNoticeSuccess() throws SQLException {
        // Setup
        String title = "Test Notice";
        String desc = "Test Description";
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = noticeDao.addNotice(title, desc);
        
        // Verify
        assertEquals("Notice Added Successfully", result);
        verify(preparedStatement).setString(1, title);
        verify(preparedStatement).setString(2, desc);
    }

    /**
     * Test notice addition with SQL injection attempt
     */
    @Test
    public void testAddNoticeWithSQLInjection() throws SQLException {
        // Setup malicious input
        String maliciousTitle = "'; DROP TABLE notice; --";
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        noticeDao.addNotice(maliciousTitle, "description");
        
        // Verify PreparedStatement is used (preventing SQL injection)
        verify(preparedStatement).setString(1, maliciousTitle);
    }

    /**
     * Test viewing all notices
     */
    @Test
    public void testViewAllNotice() throws SQLException {
        // Setup mock result set
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false); // Two records
        when(resultSet.getInt("id")).thenReturn(1, 2);
        when(resultSet.getString("title")).thenReturn("Notice 1", "Notice 2");
        when(resultSet.getString("info")).thenReturn("Info 1", "Info 2");
        
        // Execute test
        List<NoticeBean> notices = noticeDao.viewAllNotice();
        
        // Verify
        assertEquals(2, notices.size());
        assertEquals("Notice 1", notices.get(0).getNoticeTitle());
        assertEquals("Notice 2", notices.get(1).getNoticeTitle());
    }

    /**
     * Test successful notice update
     */
    @Test
    public void testUpdateNoticeSuccess() throws SQLException {
        // Setup
        NoticeBean notice = new NoticeBean(1, "Updated Title", "Updated Info");
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = noticeDao.updateNotice(notice);
        
        // Verify
        assertEquals("Notice Updated Successfully!", result);
        verify(preparedStatement).setString(1, notice.getNoticeTitle());
        verify(preparedStatement).setString(2, notice.getNoticeInfo());
        verify(preparedStatement).setInt(3, notice.getNoticeId());
    }

    /**
     * Test notice update with database error
     */
    @Test
    public void testUpdateNoticeFailure() throws SQLException {
        // Setup
        NoticeBean notice = new NoticeBean(1, "Title", "Info");
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));
        
        // Execute test
        String result = noticeDao.updateNotice(notice);
        
        // Verify error handling
        assertTrue(result.contains("Error"));
    }

    /**
     * Test getting notice by ID - successful case
     */
    @Test
    public void testGetNoticeByIdSuccess() throws SQLException {
        // Setup
        int noticeId = 1;
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("title")).thenReturn("Test Title");
        when(resultSet.getString("info")).thenReturn("Test Info");
        
        // Execute test
        NoticeBean notice = noticeDao.getNoticeById(noticeId);
        
        // Verify
        assertNotNull(notice);
        assertEquals(noticeId, notice.getNoticeId());
        assertEquals("Test Title", notice.getNoticeTitle());
        assertEquals("Test Info", notice.getNoticeInfo());
    }

    /**
     * Test getting notice by ID - notice not found
     */
    @Test
    public void testGetNoticeByIdNotFound() throws SQLException {
        // Setup
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        
        // Execute test
        NoticeBean notice = noticeDao.getNoticeById(999);
        
        // Verify
        assertNull(notice);
    }

    /**
     * Test handling of null values in notice update
     */
    @Test
    public void testUpdateNoticeWithNullValues() throws SQLException {
        // Setup
        NoticeBean notice = new NoticeBean(1, null, null);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Execute test
        String result = noticeDao.updateNotice(notice);
        
        // Verify null values are handled
        assertEquals("Notice Updated Successfully!", result);
        verify(preparedStatement).setString(1, null);
        verify(preparedStatement).setString(2, null);
    }

    /**
     * Test viewing notices with empty result set
     */
    @Test
    public void testViewAllNoticeEmpty() throws SQLException {
        // Setup empty result set
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        
        // Execute test
        List<NoticeBean> notices = noticeDao.viewAllNotice();
        
        // Verify
        assertTrue(notices.isEmpty());
    }
}
