package bookmaprecorderdemo.utils;

public class DynamicAverage {
    private long counter = 0;
    private double cumulative = 0;

    public void update(double x) {
        cumulative += x;
        counter++;
    }

    public double getAverage() {
        return cumulative / counter;
    }
}
