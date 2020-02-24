import Data.Dataset;
import Writer.ReportWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates list of runnables from methods that are subscribed to data analysis.
 * The main idea is that multiple data analysis methods can be subscribed to
 * the same analyser which iterates through the data and gives it to methods.
 *
 * FAQ:
 * Q: why not give dataset to method directly? A: We would have to iterate multiple
 * times through data - each method would iterate over data independently. By using
 * Analyser we can (partially) avoid this - AnalyticalMethods subscribe callbacks for data.
 * Q: But what about parallelism? A: You win some you loose some :)
 *
 * @param <T> Data
 * @param <A> Analyser
 */
public class ActionManager <T, A extends Analyser<T, Dataset<T>>> {

    private final static Logger log = LoggerFactory.getLogger(ActionManager.class);

    private Dataset<T> dataset;
    private List<Runnable> analysisActions;
    private ReportWriter reportWriter;
    private Supplier<A> analyserSupplier;

    private A analyser;

    /**
     * Created action manager that uses and changes dataset.
     *
     * @param dataset Data for analysis.
     * @param analyserSupplier Analyser factory.
     * @param reportWriter Writes report.
     */
    public ActionManager(Dataset<T> dataset, Supplier<A> analyserSupplier, ReportWriter reportWriter) {
        this.dataset = dataset;
        analysisActions = new ArrayList<>();
        this.reportWriter = reportWriter;
        this.analyserSupplier = analyserSupplier;
        analyser = null;
    }

    public void addFilterMethod(DatasetFilter<T, Dataset<T>> datasetFilter) {
        log.debug("Adding filter " + datasetFilter.toString() + " to action manager.");
        analyser = null;

        analysisActions.add(new Runnable() {
            @Override
            public void run() {
                datasetFilter.filter(dataset);
            }
        });
    }

    public void addAnalyticalMethod(AnalyticalMethod<T, A> method) {
        log.debug("Adding analytical method " + method.toString() + " to action manager.");

        if (analyser == null) {
            analyser = analyserSupplier.get();
            analysisActions.add(new Runnable() {
                @Override
                public void run() {
                    analyser.analyse(dataset);
                }
            });
        }

        log.debug("Adding analytical method " + method.toString() + " to analyser " + analyser.toString());
        method.register(analyser);
        reportWriter.addReportable(method);
    }

    /**
     * Runs the whole analysis.
     */
    public void run() {
        log.debug("Analysis started.");
        for (Runnable runnable : analysisActions) {
            runnable.run();
        }

        log.debug("Analysis complete.");
        log.debug("Writing report.");
        reportWriter.writeReport();
    }
}
