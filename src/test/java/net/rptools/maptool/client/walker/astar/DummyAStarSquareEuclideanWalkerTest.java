package net.rptools.maptool.client.walker.astar;

import net.rptools.maptool.client.walker.astar.DummyAStarSquareEuclideanWalker;

import net.rptools.maptool.client.walker.WalkerMetric;
import net.rptools.maptool.model.Zone;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DummyAStarSquareEuclideanWalkerTest {

    private static final int[] NORTH = {0, -1};
    private static final int[] WEST = {-1, 0};
    private static final int[] SOUTH = {0, 1};
    private static final int[] EAST = {1, 0};
    private static final int[] NORTH_EAST = {1, -1};
    private static final int[] SOUTH_EAST = {1, 1};
    private static final int[] NORTH_WEST = {-1, -1};
    private static final int[] SOUTH_WEST = {-1, 1};

    @Test
    void testShouldCreateDummyAStarSquareEuclideanWalker() {
        Zone mockZone = mock(Zone.class);
        try (DummyAStarSquareEuclideanWalker testWalker = new DummyAStarSquareEuclideanWalker(mockZone, WalkerMetric.NO_DIAGONALS)) {
            assertNotNull(testWalker);
        }
    }

    @Test
    void testSwitchWalkerMetricShouldReturnNoDiagonalsMetric() {
        WalkerMetric testMetric = WalkerMetric.NO_DIAGONALS;

        Zone mockZone = mock(Zone.class);
        try (DummyAStarSquareEuclideanWalker testWalker = new DummyAStarSquareEuclideanWalker(mockZone, testMetric);) {
            int[][] expectedMetric = {NORTH, EAST, SOUTH, WEST};
            int[][] actualMetric = testWalker.switchWalkerMetric(WalkerMetric.NO_DIAGONALS);
            assertArrayEquals(expectedMetric, actualMetric);

        }
    }

    @Test
    void testSwitchWalkerMetricShouldReturnOneTwoOneMetric() {
        WalkerMetric testMetric = WalkerMetric.ONE_TWO_ONE;
        int[][] expectedMetric = {NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};

        Zone mockZone = mock(Zone.class);
        try (DummyAStarSquareEuclideanWalker walker = new DummyAStarSquareEuclideanWalker(mockZone, testMetric);) {
            int[][] actualMetric = walker.switchWalkerMetric(WalkerMetric.ONE_TWO_ONE);
            assertArrayEquals(expectedMetric, actualMetric);
        }
    }

    @Test
    void testSwitchWalkerMetricShouldReturnManhattanMetric() {
        WalkerMetric testMetric = WalkerMetric.MANHATTAN;
        int[][] expectedMetric = {NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};

        Zone mockZone = mock(Zone.class);
        try (DummyAStarSquareEuclideanWalker walker = new DummyAStarSquareEuclideanWalker(mockZone, testMetric);) {
            int[][] actualMetric = walker.switchWalkerMetric(WalkerMetric.MANHATTAN);
            assertArrayEquals(expectedMetric, actualMetric);
        }
    }

    @Test
    void testSwitchWalkerMetricShouldReturnOneOneOneMetric() {
        WalkerMetric testMetric = WalkerMetric.ONE_ONE_ONE;
        int[][] expectedMetric = {NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};

        Zone mockZone = mock(Zone.class);
        try (DummyAStarSquareEuclideanWalker walker = new DummyAStarSquareEuclideanWalker(mockZone, testMetric);) {
            int[][] actualMetric = walker.switchWalkerMetric(WalkerMetric.ONE_ONE_ONE);
            assertArrayEquals(expectedMetric, actualMetric);
        }
    }

}
