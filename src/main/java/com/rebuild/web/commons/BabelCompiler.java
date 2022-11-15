package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class BabelCompiler {

    @GetMapping("/babel/**")
    public void babel(HttpServletRequest request, HttpServletResponse response) {
        final String asset = request.getRequestURI().split("/babel/")[1];

        String es5;
        try {
            es5 = compiler(asset);
        } catch (Exception e) {
            log.error(null, e);
            es5 = "/* BABEL COMPILER ERROR */";
        }

        ServletUtils.setContentType(response, ServletUtils.CT_JS);
        ServletUtils.write(response, es5);
    }

    private static Map<String, String[]> ES65 = new HashMap<>();

    protected String compiler(String asset) throws Exception {
        ClassPathResource resource = new ClassPathResource("web/" + asset);
        String assetKey = asset + "." + resource.getFile().lastModified();

        if (ES65.containsKey(assetKey)) {
            return ES65.get(assetKey)[0];
        }

        String es6 = CommonsUtils.getStringOfRes("web/" + asset);
        log.info("Babel compiler : {}", assetKey);

        String[] es5WithMap = es5WithMap(es6);
        ES65.put(assetKey, es5WithMap);

        return es5WithMap[0];
    }

    // --

    private static ScriptEngine ENGINE;
    private static Bindings GLOBAL_BINDINGS;

    private static final String BABLE_TRANSFORM = "Babel.transform(__es6input, {" +
            "presets: ['es2015', 'react', 'stage-0']," +
            "minified: true," +
            "sourceMaps: true" +
            "})";

    /**
     * ES6 > ES5 转码
     *
     * @param es6
     * @return
     * @throws ScriptException
     */
    synchronized
    public static String es5(String es6) throws ScriptException {
        if (StringUtils.isBlank(es6)) return "/* No code */";

        try {
            initScriptEngine();
        } catch (Exception e) {
            log.error("init ScriptEngine error", e);
            return "/* No ScriptEngine provided */";
        }

        GLOBAL_BINDINGS.put("__es6input", es6);
        Object es5 = ENGINE.eval(BABLE_TRANSFORM + ".code", GLOBAL_BINDINGS);
        return (String) es5;
    }

    /**
     * @param es6
     * @return
     * @throws ScriptException
     */
    synchronized
    public static String[] es5WithMap(String es6) throws ScriptException {
        if (StringUtils.isBlank(es6)) return new String[] { "/* No code */", null };

        try {
            initScriptEngine();
        } catch (Exception e) {
            log.error("init ScriptEngine error", e);
            return new String[] { "/* No ScriptEngine provided */", null };
        }

        GLOBAL_BINDINGS.put("__es6input", es6);
        Object es5 = ENGINE.eval(BABLE_TRANSFORM, GLOBAL_BINDINGS);

        ScriptObjectMirror mirror = (ScriptObjectMirror) es5;
        String code = (String) mirror.get("code");
        ScriptObjectMirror map = (ScriptObjectMirror) mirror.get("map");

        return new String[] {code, map.toString()};
    }

    synchronized
    static void initScriptEngine() throws ScriptException {
        if (ENGINE != null) return;

        StopWatch sw = new StopWatch();

        sw.start("init ScriptEngine");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        sw.stop();
        log.info("Initializing ScriptEngine : {}", engine);

        String polyfillScript = CommonsUtils.getStringOfRes("web/assets/lib/react/polyfill.min.js");
        String babelScript = CommonsUtils.getStringOfRes("web/assets/lib/react/babel.min.js");

        GLOBAL_BINDINGS = engine.createBindings();
        sw.start("eval Polyfill");
        engine.eval(polyfillScript, GLOBAL_BINDINGS);
        sw.stop();
        sw.start("eval Babel");
        engine.eval(babelScript, GLOBAL_BINDINGS);
        sw.stop();

        ENGINE = engine;
        log.info("ScriptEngine ready \n{}", sw.prettyPrint());
    }
}
