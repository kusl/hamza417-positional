package app.simple.positional.model;

import androidx.annotation.NonNull;

public final class MeasurePoint {
    private final double latitude;
    private final double longitude;
    private final int order;

    public MeasurePoint(double latitude, double longitude, int order) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.order = order;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getOrder() {
        return order;
    }

    public String convertForDatabase() {
        return latitude + "," + longitude + "," + order;
    }

    public static MeasurePoint convertFromDatabase(String data) {
        String[] parts = data.split(",");
        return new MeasurePoint(
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]),
                Integer.parseInt(parts[2]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeasurePoint)) return false;
        MeasurePoint that = (MeasurePoint) o;
        return Double.compare(latitude, that.latitude) == 0
                && Double.compare(longitude, that.longitude) == 0
                && order == that.order;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);
        result = 31 * result + order;
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "MeasurePoint{" + "latitude=" + latitude + ", longitude=" + longitude + ", order=" + order + '}';
    }
}
