package org.apache.flink.training.utils;

/**
 * Geographic utilities for NYC taxi data.
 */
public class GeoUtils {
    // NYC area bounding box
    private static final float LON_EAST = -73.7f;
    private static final float LON_WEST = -74.05f;
    private static final float LAT_NORTH = 41.0f;
    private static final float LAT_SOUTH = 40.5f;

    // NYC approximate center
    private static final float NYC_CENTER_LON = -73.9712f;
    private static final float NYC_CENTER_LAT = 40.7831f;

    /**
     * Checks if a location (lon, lat) is within NYC.
     *
     * @param lon longitude
     * @param lat latitude
     * @return true if the location is within NYC bounds
     */
    public static boolean isInNYC(float lon, float lat) {
        return lon > LON_WEST && lon < LON_EAST &&
               lat > LAT_SOUTH && lat < LAT_NORTH;
    }

    /**
     * Returns the Euclidean distance between two geographic points.
     * This is an approximation suitable for short distances.
     *
     * @param lon1 longitude of first point
     * @param lat1 latitude of first point
     * @param lon2 longitude of second point
     * @param lat2 latitude of second point
     * @return distance in degrees
     */
    public static double getEuclideanDistance(float lon1, float lat1, float lon2, float lat2) {
        double lonDiff = lon1 - lon2;
        double latDiff = lat1 - lat2;
        return Math.sqrt(lonDiff * lonDiff + latDiff * latDiff);
    }

    /**
     * Maps a geographic location to a grid cell ID.
     * Useful for spatial aggregation.
     *
     * @param lon longitude
     * @param lat latitude
     * @param gridSize number of cells per degree
     * @return grid cell ID
     */
    public static int mapToGridCell(float lon, float lat, int gridSize) {
        int xIndex = (int) Math.floor((lon - LON_WEST) * gridSize);
        int yIndex = (int) Math.floor((lat - LAT_SOUTH) * gridSize);
        return xIndex + yIndex * (int) Math.ceil((LON_EAST - LON_WEST) * gridSize);
    }
}
