package cn.dustlight.bucket.services.http.handler;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.services.http.HttpService;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

public class SimpleHttpHandler implements HttpHandler {

    /**
     * 网页根目录
     */
    private String root;

    private List<String> defaultPage;

    private Boolean listFile;

    @Override
    public void init(HttpService httpService) {
        ServiceConfig config = httpService.getConfig();
        root = new StringBuilder()
                .append(config.root == null ? "" : config.root + File.separator)
                .append(config.path == null ? "" : config.path)
                .toString();
        defaultPage = config.getParam("default");
        if (defaultPage != null && defaultPage.size() == 0)
            defaultPage = null;
        listFile = config.getParam("list");
    }

    @Override
    public void dispose(HttpRequest q, ChannelHandlerContext ctx) {
        try {
            String path = getFilePath(q.uri());
            File file = new File(path);

            if (file.exists()) {
                if (file.isDirectory()) {
                    if (listFile != null && listFile == true) {
                        echoDirectory(path, q, ctx);
                        return;
                    }

                } else {
                    echoFile(path, q, ctx);
                    return;
                }
            }
            echoError(new FileNotFoundException(q.uri()), HttpResponseStatus.NOT_FOUND, ctx).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            echoError(e, HttpResponseStatus.INTERNAL_SERVER_ERROR, ctx).addListener(ChannelFutureListener.CLOSE);
        }
    }

    protected String getFilePath(String uri) throws URISyntaxException, FileNotFoundException {
        URI u = new URI(uri);
        String path = root + u.getPath();
        File file = new File(path);
        if (!file.exists())
            throw new FileNotFoundException(uri);

        if (file.isDirectory()) {
            if (defaultPage != null) {
                File f;
                for (String d : defaultPage) {
                    f = new File(file, d);
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                }
            }
        }
        return file.getAbsolutePath();
    }

    protected ChannelFuture echoError(Throwable throwable, HttpResponseStatus status, ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        if (throwable != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            throwable.printStackTrace(new PrintStream(outputStream));
            byte[] arr = outputStream.toByteArray();
            HttpUtil.setContentLength(response, arr.length);
            response.headers().set("Content-Type", "text/plain;charset=utf-8");
            response.content().writeBytes(arr);
        }

        ChannelFuture r = ctx.writeAndFlush(response);
        r.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                response.release();
            }
        });
        return r;
    }

    protected ChannelFuture echoFile(String filePath, HttpRequest q, ChannelHandlerContext ctx) throws IOException {
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        RandomAccessFile file = new RandomAccessFile(filePath, "r");
        ChunkedFile chunkFile = new ChunkedFile(file, 0, file.length(), 8192);
        HttpUtil.setContentLength(response, chunkFile.length());
        if (HttpUtil.isKeepAlive(q))
            HttpUtil.setKeepAlive(response, true);
        ctx.writeAndFlush(response);
        ctx.writeAndFlush(chunkFile, ctx.newProgressivePromise());
        ChannelFuture lastChannelFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                file.close();
                chunkFile.close();
            }
        });
        if (!HttpUtil.isKeepAlive(q))
            lastChannelFuture.addListener(ChannelFutureListener.CLOSE);

        return lastChannelFuture;
    }

    protected ChannelFuture echoDirectory(String filePath, HttpRequest q, ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("<title>")
                .append(q.uri())
                .append("</title>")
                .append("<meta charset='utf-8'>")
                .append("</head>")
                .append("<body>");
        File root = new File(filePath);
        File[] files = root.listFiles();
        builder.append("<ul>");
        for (File file : files) {
            String tmp = file.isDirectory() ? file.getName() + "/" : file.getName();
            builder.append("<li>")
                    .append("<a href='" + tmp + "'>" + tmp + "</a>")
                    .append("</li>");
        }
        builder.append("</ul>");
        builder.append("</body>")
                .append("</hmtl>");

        if (HttpUtil.isKeepAlive(q))
            HttpUtil.setKeepAlive(response, true);

        HttpUtil.setContentLength(response, builder.length());

        response.content().writeCharSequence(builder, Charset.forName("UTF-8"));
        ChannelFuture lastChannelFuture = ctx.writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(q))
            lastChannelFuture.addListener(ChannelFutureListener.CLOSE);
        return lastChannelFuture;
    }
}
