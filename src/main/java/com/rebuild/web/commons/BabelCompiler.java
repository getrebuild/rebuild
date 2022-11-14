package com.rebuild.web.commons;

import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        String asset = request.getRequestURI().split("/babel/")[1];
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

    private static Map<String, String> ES65 = new HashMap<>();

    protected String compiler(String asset) throws Exception {
        ClassPathResource resource = new ClassPathResource("web/" + asset);
        String assetKey = asset + "." + resource.getFile().lastModified();

        if (ES65.containsKey(assetKey)) return ES65.get(assetKey);

        String es6 = CommonsUtils.getStringOfRes("web/" + asset);
        log.info("Babel compiler : {}", assetKey);

        String es5 = es5(es6);
        ES65.put(assetKey, es5);

        return es5;
    }

    // --

    private static ScriptEngine ENGINE;
    private static Bindings GLOBAL_BINDINGS;

    private static final String BABLE_TRANSFORM = "Babel.transform(__es6input, {" +
            "presets: ['es2015', 'react', 'stage-0']," +
            "minified: true," +
            "sourceMaps: true" +
            "}).code";

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
        Object es5 = ENGINE.eval(BABLE_TRANSFORM, GLOBAL_BINDINGS);
        return (String) es5;
    }

    // FIXME ScriptEngine 初始化太慢
    static void initScriptEngine() throws ScriptException {
        if (ENGINE != null) return;

        StopWatch sw = new StopWatch();

        sw.start("init ScriptEngine");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        sw.stop();
        log.info("Initializing ScriptEngine : {}", engine);

        String polyfillScript = CommonsUtils.getStringOfRes("web/assets/lib/react/polyfill.min.js");
        String babelScript = CommonsUtils.getStringOfRes("web/assets/lib/react/babel.min.js");

        GLOBAL_BINDINGS = engine.createBindings();
        sw.start("eval polyfill");
        engine.eval(polyfillScript, GLOBAL_BINDINGS);
        sw.stop();
        sw.start("eval babel");
        engine.eval(babelScript, GLOBAL_BINDINGS);
        sw.stop();

        ENGINE = engine;
        log.info("ScriptEngine ready \n{}", sw.prettyPrint());
    }
}
