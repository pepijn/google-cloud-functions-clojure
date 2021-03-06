package functions;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.io.IOException;

public class JsonHttpEcho implements HttpFunction {
    public void service(HttpRequest request, HttpResponse response)
            throws IOException {
        IFn require = Clojure.var("clojure.core", "require");

        String adapterNs = "nl.epij.google-cloud-function-ring-adapter.alpha";
        String handlerNs = "nl.epij.gcf-ring-adapter-example";

        require.invoke(Clojure.read(adapterNs));
        require.invoke(Clojure.read(handlerNs));

        IFn adapter = Clojure.var(adapterNs, "adapter");
        IFn handler = Clojure.var(handlerNs, "app");
        adapter.invoke(request, response, handler);
    }
}
