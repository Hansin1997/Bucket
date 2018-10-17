package test;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.services.http.HttpService;
import cn.dustlight.bucket.services.http.SimpleHttpService;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.Properties;

public class ScriptTest {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("python.console.encoding", "UTF-8");
        props.put("python.security.respectJavaAccessibility", "false");
        props.put("python.import.site", "false");

        Properties preprops = System.getProperties();
        PythonInterpreter.initialize(props, preprops, new String[] {});
        PythonInterpreter interpreter = new PythonInterpreter();

        interpreter.execfile("d:/test.py");

    }
}
