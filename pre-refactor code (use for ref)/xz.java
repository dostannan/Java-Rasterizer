import java.util.Comparator;

public class xz {
    private int x;
    private int y;
    private Float z;
    private Float u;
    private Float v;

    public xz(int xval, int yval, Float zval, Float uval, Float vval) {
        x = xval;
        y = yval;
        z = zval;
        u = uval;
        v = vval;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public Float getZ() {
        return z;
    }
    public Float[] getUVCoord() {
        return new Float[]{u, v};
    }
    public Float getU() {
        return u;
    }
    public Float getV() {
        return v;
    }

    public static Comparator<xz> xzCompare = new Comparator<xz>() {
        public int compare(xz one, xz two) {
            Integer x1 = one.getX();
            Integer x2 = two.getX();

            return x1.compareTo(x2);
        }
    };
}
