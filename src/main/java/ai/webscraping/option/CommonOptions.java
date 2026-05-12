package ai.webscraping.option;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared options applicable to every API endpoint. Per-endpoint option
 * classes ({@link HtmlOptions}, {@link TextOptions}, …) extend this and add
 * the endpoint-specific fields.
 *
 * <p>All optional booleans/integers are nullable so "not set" can be
 * distinguished from a zero/false value — unset fields are dropped from the
 * wire and the server applies its own defaults.
 */
public abstract class CommonOptions {

    private final Map<String, String> headers;
    private final Integer timeout;
    private final Boolean js;
    private final Integer jsTimeout;
    private final String waitFor;
    private final String proxy;
    private final String country;
    private final String customProxy;
    private final String device;
    private final Boolean errorOn404;
    private final Boolean errorOnRedirect;
    private final String jsScript;

    protected CommonOptions(BaseBuilder<?, ?> b) {
        this.headers = b.headers;
        this.timeout = b.timeout;
        this.js = b.js;
        this.jsTimeout = b.jsTimeout;
        this.waitFor = b.waitFor;
        this.proxy = b.proxy;
        this.country = b.country;
        this.customProxy = b.customProxy;
        this.device = b.device;
        this.errorOn404 = b.errorOn404;
        this.errorOnRedirect = b.errorOnRedirect;
        this.jsScript = b.jsScript;
    }

    public Map<String, String> getHeaders() {
        return headers == null ? null : Collections.unmodifiableMap(headers);
    }

    public Integer getTimeout() {
        return timeout;
    }

    public Boolean getJs() {
        return js;
    }

    public Integer getJsTimeout() {
        return jsTimeout;
    }

    public String getWaitFor() {
        return waitFor;
    }

    public String getProxy() {
        return proxy;
    }

    public String getCountry() {
        return country;
    }

    public String getCustomProxy() {
        return customProxy;
    }

    public String getDevice() {
        return device;
    }

    public Boolean getErrorOn404() {
        return errorOn404;
    }

    public Boolean getErrorOnRedirect() {
        return errorOnRedirect;
    }

    public String getJsScript() {
        return jsScript;
    }

    /**
     * Curiously-recurring base builder. Each endpoint's own {@code Builder}
     * extends this with {@code &lt;O = OptionsType, B = OwnBuilderType&gt;} so
     * fluent calls return the concrete builder type.
     */
    public abstract static class BaseBuilder<O, B extends BaseBuilder<O, B>> {
        private Map<String, String> headers;
        private Integer timeout;
        private Boolean js;
        private Integer jsTimeout;
        private String waitFor;
        private String proxy;
        private String country;
        private String customProxy;
        private String device;
        private Boolean errorOn404;
        private Boolean errorOnRedirect;
        private String jsScript;

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B headers(Map<String, String> headers) {
            this.headers = headers == null ? null : new LinkedHashMap<>(headers);
            return self();
        }

        public B addHeader(String name, String value) {
            if (this.headers == null) {
                this.headers = new LinkedHashMap<>();
            }
            this.headers.put(name, value);
            return self();
        }

        public B timeout(int timeoutMs) {
            this.timeout = timeoutMs;
            return self();
        }

        public B js(boolean js) {
            this.js = js;
            return self();
        }

        public B jsTimeout(int jsTimeoutSeconds) {
            this.jsTimeout = jsTimeoutSeconds;
            return self();
        }

        public B waitFor(String selector) {
            this.waitFor = selector;
            return self();
        }

        public B proxy(String proxy) {
            this.proxy = proxy;
            return self();
        }

        public B country(String country) {
            this.country = country;
            return self();
        }

        public B customProxy(String customProxy) {
            this.customProxy = customProxy;
            return self();
        }

        public B device(String device) {
            this.device = device;
            return self();
        }

        public B errorOn404(boolean errorOn404) {
            this.errorOn404 = errorOn404;
            return self();
        }

        public B errorOnRedirect(boolean errorOnRedirect) {
            this.errorOnRedirect = errorOnRedirect;
            return self();
        }

        public B jsScript(String jsScript) {
            this.jsScript = jsScript;
            return self();
        }

        public abstract O build();
    }
}
