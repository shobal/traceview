<%--
  Created by IntelliJ IDEA.
  User: Administrator
  Date: 2016/8/19
  Time: 13:24
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>File Upload</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>
<body>
<form method="POST" action="analyze" enctype="multipart/form-data" >
  New Trace File 1:
  <input type="file" name="file1" id="file_1" /> <br/>
  Old Trace File 2:
  <input type="file" name="file2" id="file_2" /> <br/>
  </br>
  <input type="submit" value="Upload" name="upload" id="upload" />
</form>
</body>
</html>
