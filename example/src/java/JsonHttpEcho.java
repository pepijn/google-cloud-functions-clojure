import nl.epij.gcp.gcf.RingHttpFunction;

public class JsonHttpEcho extends RingHttpFunction {
    public String getHandler() {
        return "nl.epij.gcf.example/app";
    }
}
