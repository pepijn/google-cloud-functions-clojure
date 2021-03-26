package functions;

import nl.epij.gcp.gcf.RingHttpFunction;

public class JsonHttpEcho extends RingHttpFunction {
    @Override
    public String getHandler() {
        return "nl.epij.gcf-ring-adapter-example/app";
    }
}
