package org.redukti.jfotopix.math;

public class Vector3 {

    private static final int N = 3;

    public static final Vector3 vector3_0 = new Vector3(0.0, 0.0, 0.0);
    public static final Vector3 vector3_1 = new Vector3(1.0, 1.0, 1.0);

    public static final Vector3 vector3_001 = new Vector3(0.0, 0.0, 1.0);
    public static final Vector3 vector3_010 = new Vector3(0.0, 1.0, 0.0);
    public static final Vector3 vector3_100 = new Vector3(1.0, 0.0, 0.0);

    final double[] values;

    public Vector3(double x, double y, double z) {
        this.values = new double[N];
        this.values[0] = x;
        this.values[1] = y;
        this.values[2] = z;
    }

    private Vector3(double[] values) {
        this.values = values;
    }

    public final double x() {
        return this.values[0];
    }

    public final double y() {
        return this.values[1];
    }

    public final double z() {
        return this.values[2];
    }

//    public Vector3 mul (Vector3 v)
//    {
//        double vec[] = new double[N];
//        for (int i = 0; i < N; i++)
//            vec[i] = this.values[i] * v.values[i];
//
//        return new Vector3(vec);
//    }

    public double dot (Vector3 v)
    {
        double r = 0;
        for (int i = 0; i < N; i++)
            r += values[i] * v.values[i];
        return r;
    }


    /**
     * The cross product a × b is defined as a vector c that is
     * perpendicular (orthogonal) to both a and b, with a direction given by the right-hand rule
     * and a magnitude equal to the area of the parallelogram that the vectors span.
     *
     * https://en.wikipedia.org/wiki/Cross_product
     */
    public Vector3 crossProduct(Vector3 b) {
        return new Vector3(y() * b.z() - z() * b.y(),
                z() * b.x() - x() * b.z(),
                x() * b.y() - y() * b.x());
    }


}
