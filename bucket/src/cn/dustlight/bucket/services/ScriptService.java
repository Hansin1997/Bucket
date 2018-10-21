package cn.dustlight.bucket.services;

import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.CommonFuture;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.util.ArrayList;

public class ScriptService extends Service {

    private Invocable engine;
    private OutputStream out;
    private OutputStream err;

    @Override
    protected CommonFuture<ScriptService> doInit(ServiceConfig config) {
        try {
            String scriptType = config.getParam("name");
            ScriptEngine eng = new ScriptEngineManager().getEngineByName(scriptType);
            if (eng instanceof Invocable) {
                File dir = new File(config.root + File.separator + "out");
                dir.mkdir();
                out = new PrintStream(new FileOutputStream(config.root + File.separator + "out" + File.separator + "out.log"));
                err = new PrintStream(new FileOutputStream(config.root + File.separator + "out" + File.separator + "err.log"));
                eng.getContext().setWriter(new OutputStreamWriter(out));
                eng.getContext().setErrorWriter(new OutputStreamWriter(err));
                if (scriptType.toUpperCase().equals("JAVASCRIPT")){
                    eng.put("out",out);
                    eng.put("err",err);
                }
                Reader script = new BufferedReader(new FileReader(config.root + File.separator + config.path));
                eng.eval(script, eng.getContext());
                script.close();
                this.engine = (Invocable) eng;
            } else {
                throw new ServiceException(-601, "Script Engine not a invocable object");
            }

        } catch (ServiceException e) {
            e.printStackTrace();
            return new CommonFuture<ScriptService>() {
                @Override
                public void run() {
                    done(ScriptService.this,e);
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            ServiceException se = new ServiceException(-600, "ScriptService Init Error:" + e.toString());
            se.addSuppressed(e);
            return new CommonFuture<ScriptService>() {
                @Override
                public void run() {
                    done(ScriptService.this,se);
                }
            };
        }
        return new CommonFuture<ScriptService>() {
            @Override
            public void run() {
                try {
                    engine.invokeFunction("init", config);
                    done(ScriptService.this, null);
                } catch (Exception e) {
                    done(null, e);
                }
            }
        };
    }

    @Override
    protected CommonFuture<ScriptService> doStart(ServiceConfig config) {
        return new CommonFuture<ScriptService>() {
            @Override
            public void run() {
                try {
                    engine.invokeFunction("start", config);
                    done(ScriptService.this, null);
                } catch (Exception e) {
                    done(null, e);
                }
            }
        };
    }

    @Override
    protected CommonFuture<ScriptService> doStop() {
        return new CommonFuture<ScriptService>() {
            @Override
            public void run() {
                try {
                    engine.invokeFunction("stop");
                    done(ScriptService.this, null);
                } catch (Exception e) {
                    done(null, e);
                }
            }
        };
    }

    @Override
    public void resetConfig(ServiceConfig config) throws ServiceException {
        try {
            engine.invokeFunction("reset", config);
        } catch (Exception e) {
            ServiceException se = new ServiceException(-600, "ScriptService Reset Error:" + e.toString());
            se.addSuppressed(e);
            throw se;
        }
    }

    @Override
    public CommonFuture<Object> call(ServiceCalling calling) {
        return new CommonFuture<Object>() {
            @Override
            public void run() {
                try {
                    Object obj = engine.invokeFunction("call",calling);
                    done(obj, null);
                } catch (Exception e) {
                    done(null, e);
                }
            }
        };
    }
}
