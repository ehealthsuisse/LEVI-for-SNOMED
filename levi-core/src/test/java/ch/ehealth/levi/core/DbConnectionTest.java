package ch.ehealth.levi.core;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.ehealth.levi.core.export.ResultCollector;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DbConnection.
 *
 * DB-dependent methods (connect, searchTranslations, etc.) require a live
 * database and are covered by integration tests. This class focuses on the
 * static batch-query builders and the hasColumn() utility that can be tested
 * without a real connection.
 */
public class DbConnectionTest {

    private DbConnection dbConnection;

    @BeforeEach
    public void setUp() {
        dbConnection = new DbConnection(new ResultCollector(), new Conf());
    }

    // =========================================================
    // buildBatchedQueries()  – batch size 500
    // =========================================================

    @Test
    public void testBuildBatchedQueries_EmptySetReturnsEmptyList() {
        List<String> result = DbConnection.buildBatchedQueries(new HashSet<>(), "SELECT * WHERE id");
        assertTrue(result.isEmpty(), "Empty ID set should produce no queries");
    }

    @Test
    public void testBuildBatchedQueries_SingleIdProducesOneQuery() {
        Set<String> ids = Collections.singleton("123456");
        List<String> result = DbConnection.buildBatchedQueries(ids, "SELECT * WHERE id");
        assertEquals(1, result.size(), "One ID should produce exactly one query");
        assertTrue(result.get(0).contains("'123456'"), "Query should contain the quoted ID");
        assertTrue(result.get(0).contains("IN ("), "Query should contain IN clause");
    }

    @Test
    public void testBuildBatchedQueries_AllIdsInSingleBatchWhenBelow500() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 499; i++) {
            ids.add("id" + i);
        }
        List<String> result = DbConnection.buildBatchedQueries(ids, "SELECT * WHERE id");
        assertEquals(1, result.size(), "499 IDs should fit in a single batch");
    }

    @Test
    public void testBuildBatchedQueries_ExactlyOneBatchAt500Ids() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            ids.add("id" + i);
        }
        List<String> result = DbConnection.buildBatchedQueries(ids, "SELECT * WHERE id");
        assertEquals(1, result.size(), "Exactly 500 IDs should still be one batch");
    }

    @Test
    public void testBuildBatchedQueries_TwoBatchesFor501Ids() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 501; i++) {
            ids.add("id" + i);
        }
        List<String> result = DbConnection.buildBatchedQueries(ids, "SELECT * WHERE id");
        assertEquals(2, result.size(), "501 IDs should produce 2 batches");
    }

    @Test
    public void testBuildBatchedQueries_BaseQueryIsPrefixedToEachBatch() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1001; i++) {
            ids.add("id" + i);
        }
        String base = "SELECT * WHERE conceptId";
        List<String> result = DbConnection.buildBatchedQueries(ids, base);
        for (String query : result) {
            assertTrue(query.startsWith(base), "Each batch query must start with the base query");
        }
    }

    // =========================================================
    // buildBatchedQueries2()  – batch size 5000, /*IDS*/ placeholder
    // =========================================================

    @Test
    public void testBuildBatchedQueries2_EmptySetReturnsEmptyList() {
        List<String> result = DbConnection.buildBatchedQueries2(new HashSet<>(), "SELECT * WHERE id IN (/*IDS*/)");
        assertTrue(result.isEmpty(), "Empty ID set should produce no queries");
    }

    @Test
    public void testBuildBatchedQueries2_PlaceholderIsReplaced() {
        Set<String> ids = Collections.singleton("abc");
        String template = "SELECT * WHERE id IN (/*IDS*/)";
        List<String> result = DbConnection.buildBatchedQueries2(ids, template);
        assertEquals(1, result.size());
        assertFalse(result.get(0).contains("/*IDS*/"), "Placeholder should be replaced");
        assertTrue(result.get(0).contains("'abc'"), "ID should appear quoted in the query");
    }

    @Test
    public void testBuildBatchedQueries2_AllIdsInSingleBatchWhenBelow5000() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 4999; i++) {
            ids.add("id" + i);
        }
        List<String> result = DbConnection.buildBatchedQueries2(ids, "SELECT * WHERE id IN (/*IDS*/)");
        assertEquals(1, result.size(), "4999 IDs should fit in a single batch");
    }

    @Test
    public void testBuildBatchedQueries2_TwoBatchesFor5001Ids() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 5001; i++) {
            ids.add("id" + i);
        }
        List<String> result = DbConnection.buildBatchedQueries2(ids, "SELECT * WHERE id IN (/*IDS*/)");
        assertEquals(2, result.size(), "5001 IDs should produce 2 batches");
    }

    @Test
    public void testBuildBatchedQueries2_MultipleIdsAreSeparatedByCommas() {
        Set<String> ids = new LinkedHashSet<>(Arrays.asList("a1", "a2", "a3"));
        List<String> result = DbConnection.buildBatchedQueries2(ids, "SELECT * WHERE id IN (/*IDS*/)");
        assertEquals(1, result.size());
        String query = result.get(0);
        // Each ID should be individually quoted and comma-separated
        assertTrue(query.contains("'a1'"), "Query should contain 'a1'");
        assertTrue(query.contains("'a2'"), "Query should contain 'a2'");
        assertTrue(query.contains("'a3'"), "Query should contain 'a3'");
    }

    // =========================================================
    // buildBatchedTermConceptIdQueries()  – batch size 500
    // =========================================================

    @Test
    public void testBuildBatchedTermConceptIdQueries_EmptySetReturnsEmptyList() {
        List<String> result = DbConnection.buildBatchedTermConceptIdQueries(new HashSet<>(), "SELECT * WHERE ");
        assertTrue(result.isEmpty(), "Empty pair set should produce no queries");
    }

    @Test
    public void testBuildBatchedTermConceptIdQueries_SinglePairProducesOneQuery() {
        Set<Pair<String, String>> pairs = Collections.singleton(Pair.of("my term", "123456"));
        List<String> result = DbConnection.buildBatchedTermConceptIdQueries(pairs, "SELECT * WHERE ");
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("term = 'my term'"), "Query should contain term condition");
        assertTrue(result.get(0).contains("conceptId = '123456'"), "Query should contain conceptId condition");
    }

    @Test
    public void testBuildBatchedTermConceptIdQueries_MultiplePairsJoinedWithOR() {
        Set<Pair<String, String>> pairs = new LinkedHashSet<>(Arrays.asList(
            Pair.of("term one", "111"),
            Pair.of("term two", "222")
        ));
        List<String> result = DbConnection.buildBatchedTermConceptIdQueries(pairs, "SELECT * WHERE ");
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("OR"), "Multiple conditions should be joined with OR");
    }

    @Test
    public void testBuildBatchedTermConceptIdQueries_TwoBatchesFor501Pairs() {
        Set<Pair<String, String>> pairs = new HashSet<>();
        for (int i = 0; i < 501; i++) {
            pairs.add(Pair.of("term" + i, "concept" + i));
        }
        List<String> result = DbConnection.buildBatchedTermConceptIdQueries(pairs, "SELECT * WHERE ");
        assertEquals(2, result.size(), "501 pairs should produce 2 batches");
    }

    @Test
    public void testBuildBatchedTermConceptIdQueries_BaseQueryIsPrefixedToEachBatch() {
        Set<Pair<String, String>> pairs = new HashSet<>();
        for (int i = 0; i < 1001; i++) {
            pairs.add(Pair.of("term" + i, "concept" + i));
        }
        String base = "SELECT id FROM descriptions WHERE ";
        List<String> result = DbConnection.buildBatchedTermConceptIdQueries(pairs, base);
        for (String query : result) {
            assertTrue(query.startsWith(base), "Each batch query must start with the base query");
        }
    }

    // =========================================================
    // hasColumn()
    // =========================================================

    @Test
    public void testHasColumn_ReturnsTrueWhenColumnExists() throws SQLException {
        ResultSet mockRs = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockMeta = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockRs.getMetaData()).thenReturn(mockMeta);
        Mockito.when(mockMeta.getColumnCount()).thenReturn(2);
        Mockito.when(mockMeta.getColumnLabel(1)).thenReturn("conceptId");
        Mockito.when(mockMeta.getColumnLabel(2)).thenReturn("term");

        assertTrue(dbConnection.hasColumn(mockRs, "conceptId"), "Should find column 'conceptId'");
        assertTrue(dbConnection.hasColumn(mockRs, "term"), "Should find column 'term'");
    }

    @Test
    public void testHasColumn_ReturnsFalseWhenColumnAbsent() throws SQLException {
        ResultSet mockRs = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockMeta = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockRs.getMetaData()).thenReturn(mockMeta);
        Mockito.when(mockMeta.getColumnCount()).thenReturn(1);
        Mockito.when(mockMeta.getColumnLabel(1)).thenReturn("term");

        assertFalse(dbConnection.hasColumn(mockRs, "conceptId"), "Should not find missing column 'conceptId'");
    }

    @Test
    public void testHasColumn_IsCaseInsensitive() throws SQLException {
        ResultSet mockRs = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockMeta = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockRs.getMetaData()).thenReturn(mockMeta);
        Mockito.when(mockMeta.getColumnCount()).thenReturn(1);
        Mockito.when(mockMeta.getColumnLabel(1)).thenReturn("ConceptId");

        assertTrue(dbConnection.hasColumn(mockRs, "conceptid"), "Column lookup should be case-insensitive");
        assertTrue(dbConnection.hasColumn(mockRs, "CONCEPTID"), "Column lookup should be case-insensitive");
    }

    @Test
    public void testHasColumn_ReturnsFalseForEmptyResultSet() throws SQLException {
        ResultSet mockRs = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockMeta = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockRs.getMetaData()).thenReturn(mockMeta);
        Mockito.when(mockMeta.getColumnCount()).thenReturn(0);

        assertFalse(dbConnection.hasColumn(mockRs, "anyColumn"), "Should return false for ResultSet with no columns");
    }

    // =========================================================
    // disconnect() – safe to call when connection is null
    // =========================================================

    @Test
    public void testDisconnect_DoesNotThrowWhenConnectionIsNull() {
        // A freshly constructed DbConnection has a null connection field
        assertDoesNotThrow(() -> dbConnection.disconnect(),
            "disconnect() should not throw when no connection has been opened");
    }
}
