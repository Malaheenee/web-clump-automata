<%@ page language="java" %>

<!--%@ include file="/html/ClumpHeader.html" %-->

<% if (request.getAttribute("fStatus") != null) { %>

<%@ include file="/WEB-INF/jsp/ClumpResult.jsp" %>

<% } else { %>

<%@ include file="/html/ClumpForm.html" %>

<% } %>

<!--%@ include file="/html/ClumpFooter.html" %-->
