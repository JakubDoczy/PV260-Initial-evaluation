import Data.Dataset;

/**
 * Analyses dataset. Implementation can subscribe AnalyticalMethods for analysis
 *
 * @param <T>
 * @param <D>
 */
public interface Analyser<T, D extends Dataset<T>> {

    void analyse(D dataset);
}
