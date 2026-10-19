package prink;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The window of recent cluster information losses that defines tau must start with mu values, not the default 10. */
public class TauWindowTest {

    @SuppressWarnings("unchecked")
    private static LinkedList<Float> window(CastleFunction f) throws ReflectiveOperationException {
        Field field = CastleFunction.class.getDeclaredField("recentInfoLoss");
        field.setAccessible(true);
        return (LinkedList<Float>) field.get(f);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100, 1000})
    void windowStartsWithMuZeros(int mu) throws ReflectiveOperationException {
        CastleFunction f = new CastleFunction(0, 5, 2, 125, 50000, 10000, mu, false, 2);
        LinkedList<Float> w = window(f);
        assertEquals(mu, w.size());
        assertEquals(0f, w.stream().reduce(0f, Float::sum));
    }
}
