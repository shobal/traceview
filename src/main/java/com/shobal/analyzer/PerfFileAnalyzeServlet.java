package com.shobal.analyzer;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/8/19.
 */
public class PerfFileAnalyzeServlet extends HttpServlet {
    private String javaScript = "<script language=\"javascript\" type=\"text/javascript\">\n" +
            "var headerTds = document.getElementById(\"analyze_result\").rows[0].cells;\n" +
            "var mousedown = false;\n" +
            "var resizeable = false;\n" +
            "var targetTd;\n" +
            "var screenXStart =0;\n" +
            "var tdWidth = 0;\n" +
            "var headerWidth = 0;\n" +
            "var tblObj = document.getElementById(\"analyze_result\");\n" +
            "for(var i = 0;i<headerTds.length;i++){\n" +
            "    addListener(headerTds[i],\"mousedown\",onmousedown);\n" +
            "    addListener(headerTds[i],\"mousemove\",onmousemove);\n" +
            "}\n" +
            "function onmousedown(event){\n" +
            "    if (resizeable == true){\n" +
            "        var evt =event||window.event;\n" +
            "        mousedown = true;\n" +
            "        screenXStart = evt.screenX;\n" +
            "        tdWidth = targetTd.offsetWidth;\n" +
            "        headerWidth = tblObj.offsetWidth;\n" +
            "    }\n" +
            "}\n" +
            "function onmousemove(event){\n" +
            "    var evt =event||window.event;\n" +
            "    var srcObj = getTarget(evt);\n" +
            "    var offsetX = evt.offsetX || (evt.clientX - srcObj.getBoundingClientRect().left);//这个比较关键，解决了Firefox无offsetX属性的问题\n" +
            "    if (mousedown == true){\n" +
            "        var width = (tdWidth + (evt.screenX - screenXStart)) + \"px\";//计算后的新的宽度\n" +
            "        targetTd.style.width = width;\n" +
            "        tblObj.style.width = (headerWidth + (evt.screenX - screenXStart)) + \"px\";\n" +
            "    }else{\n" +
            "        var trObj = tblObj.rows[0];\n" +
            "        if(srcObj.offsetWidth - offsetX <=4){//实际改变本单元格列宽\n" +
            "            targetTd=srcObj;\n" +
            "            resizeable = true;\n" +
            "            srcObj.style.cursor='col-resize';//修改光标样式\n" +
            "        }else if(offsetX <=4 && srcObj.cellIndex > 0){//实际改变前一单元格列宽，但是表格左边框线不可拖动\n" +
            "            targetTd=trObj.cells[srcObj.cellIndex - 1];\n" +
            "            resizeable = true;\n" +
            "            srcObj.style.cursor='col-resize';\n" +
            "        }else{\n" +
            "            resizeable = false;\n" +
            "            srcObj.style.cursor='default';\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "document.onmouseup = function(event){\n" +
            "    tartgetTd = null;\n" +
            "    resizeable = false;\n" +
            "    mousedown = false;\n" +
            "    document.body.style.cursor='default';\n" +
            "}\n" +
            "function getTarget(evt){\n" +
            "    return evt.target || evt.srcElement;\n" +
            "}\n" +
            "function addListener(element,type,listener,useCapture){\n" +
            "    element.addEventListener?element.addEventListener(type,listener,useCapture):element.attachEvent(\"on\" + type,listener);\n" +
            "}\n" +
            "</script>";
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter w = response.getWriter();
        w.println("<html><head><title>result</title>"+""+"</head><body>");
        w.println("start upload files.....");
        request.setCharacterEncoding("utf-8");
        //获得磁盘文件条目工厂
        DiskFileItemFactory factory = new DiskFileItemFactory();
        //获取文件需要上传到的路径
        String path =getServletContext().getRealPath("/")+"traces";
        w.println("<br/>upload files PAHT="+path);
        File dir = new File(path);
        if (dir.exists()){
            /*if (dir.isDirectory() && dir.listFiles().length > 0){
                File[] files = dir.listFiles();
                for (File f: files) {
                    f.delete();
                }
            }*/
        }else{
            dir.mkdirs();
        }
        factory.setRepository(dir);
        //设置 缓存的大小，当上传文件的容量超过该缓存时，直接放到 暂时存储室
        factory.setSizeThreshold(1024*1024) ;
        /*FileCleaningTracker fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(getServletContext());
        factory.setFileCleaningTracker(fileCleaningTracker);*/
        ServletFileUpload upload = new ServletFileUpload(factory);
        ArrayList<String> traceFiles = new ArrayList<String>();
        //可以上传多个文件
        OutputStream out = null;
        InputStream in = null;
        try {
                List<FileItem> list = upload.parseRequest(request);
                for(FileItem item : list) {
                    //获取表单的属性名字
                    String name = item.getFieldName();

                    //如果获取的 表单信息是普通的 文本 信息
                    if(item.isFormField())
                    {
                        //获取用户具体输入的字符串 ，名字起得挺好，因为表单提交过来的是 字符串类型的
                        String value = item.getString() ;

                        request.setAttribute(name, value);
                    }
                    //对传入的非 简单的字符串进行处理 ，比如说二进制的 图片，电影这些
                    else
                    {
                        /**
                         * 以下三步，主要获取 上传文件的名字
                         */
                        //获取路径名
                        String value = item.getName() ;
                        //索引到最后一个反斜杠
                        int start = value.lastIndexOf("\\");
                        //截取 上传文件的 字符串名字，加1是 去掉反斜杠，
                        String filename = value.substring(start+1);

                        request.setAttribute(name, filename);

                        //真正写到磁盘上
                        //它抛出的异常 用exception 捕捉

                        //item.write( new File(path,filename) );//第三方提供的

                        //手动写的
                        w.println("<br/>uploading file "+filename+".......");
                        traceFiles.add(path+"\\"+filename);
                        File curFile = new File(path,filename);
                        if (curFile.exists())
                            curFile.delete();
                        out = new FileOutputStream(curFile);
                        in = item.getInputStream() ;

                        int length = 0 ;
                        byte [] buf = new byte[1024] ;

                        // in.read(buf) 每次读到的数据存放在   buf 数组中
                        while( (length = in.read(buf) ) != -1)
                        {
                            //在   buf 数组中 取出数据 写到 （输出流）磁盘上
                            out.write(buf, 0, length);
                        }
                        in.close();
                        out.close();
                        in = null;
                        out = null;
                        w.println("<br/>upload size:"+item.getSize());
                    }
                }

            } catch (FileUploadException e) {
                e.printStackTrace();
            }finally {
                if (in !=null)
                    in.close();
                if (out != null)
                    out.close();
            }
            w.println("<br/>upload files over.......");
            w.println("<br/>--------------start analyze files-----------");
            TraceAnalyzer traceAnalyzer = new TraceAnalyzer(traceFiles,w);
            traceAnalyzer.startCompareAnalyzer2();
            w.println("<br/>--------------end analyze files-----------");
            w.println("</body></html>");
            w.flush();
            w.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter w = response.getWriter();
        w.println("dsfsdf--ertrt");
    }
}
