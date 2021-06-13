package nl.epij.gcf;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.google.cloud.ServiceOptions;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * It's unfortunately not possible to compile this class from Clojure.
 * The static { } block added by Clojure runs during build time of the Cloud Function which doesn't have
 * clojure.core available in the class path.
 */
public abstract class RingHttpFunction implements HttpFunction {
    private static final String adapterNs = "nl.epij.gcf.ring";
    private static final String adapterFn = "adapter";

    /**
     * The fully qualified Clojure ring handler function to call for every incoming HTTP request.
     */
    public abstract String getHandler();
    public final String getAdapter() {
        return adapterNs + "/" + adapterFn;
    }

    private IFn require = null;
    private IFn adapter = null;
    private IFn handler = null;

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        String trace = request.getFirstHeader("X-Cloud-Trace-Context").orElse("").split("/")[0];
        String projectId = ServiceOptions.getDefaultProjectId();
        String traceId = String.format("projects/%s/traces/%s", projectId, trace);

        getAdapterFn(traceId).invoke(request, response, getHandlerFn(traceId));
    }

    private synchronized IFn getRequire() {
        if (this.require == null)
            this.require = Clojure.var("clojure.core", "require");

        return this.require;
    }

    private IFn getFn(String ns, String fn, String traceId) {
        logger.error(
                "Loading Clojure namespace '" + ns + "' function '" + fn + "'",
                kv("severity", "DEBUG"),
                kv("logging.googleapis.com/trace", traceId),
                kv("namespace", ns),
                kv("function", fn));

        getRequire().invoke(Clojure.read(ns));
        return Clojure.var(ns, fn);
    }

    private synchronized IFn getAdapterFn(String traceId) {
        if (this.adapter == null)
            this.adapter = getFn(adapterNs, adapterFn, traceId);

        return this.adapter;
    }

    private synchronized IFn getHandlerFn(String trace) {
        if (this.handler == null) {
            String[] handler = getHandler().split("/");
            String handlerNs = handler[0];
            String handlerFn = handler[1];

            this.handler = getFn(handlerNs, handlerFn, trace);
        }

        return this.handler;
    }

    private static final Logger logger = LoggerFactory.getLogger(RingHttpFunction.class.getName());
}
