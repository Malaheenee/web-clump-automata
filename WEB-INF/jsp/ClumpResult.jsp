<%@ page language="java" %>

<h2>Your parameters are:</h2>
<label>Pattern:</label> <%= request.getAttribute("fPattern") %>
<br>
<label>Probabilities:</label> <%= request.getAttribute("fProb") %>
<br>
<label>Polynomial truncation:</label> <%= request.getAttribute("fTruncate")%> and <label>A:</label> <%= request.getAttribute("fA")%>
<br>
<br>

<% if (request.getAttribute("fStatus").equals("Successful")) { %>
<label>AutoClump:</label> <%= request.getAttribute("fNumberOfStates")%>  states and <%= request.getAttribute("fNumberOfEdges")%> edges.
<label>DeepClump:</label> <%= request.getAttribute("fNumberOfStates2")%>  states and <%= request.getAttribute("fNumberOfEdges2")%> edges.

<br>
<br>

<form name="coeffcz" method="GET" action='<%= request.getAttribute("clumpPath") %>'>
<label>Coefficients of C(z) (<a href="javascript:void(0)" onClick="document.forms['coeffcz'].submit()">As file</a>):</label>
<br>
<textarea name="resData" rows=5 cols=40>
<%= request.getAttribute("aPoly")%>
</textarea>
<input type="hidden" name="resFile" value="coeff.txt" />
</form>

<label>Rho: the root of 1-z+C(z):</label> <%= request.getAttribute("fRho")%>
<br>
<label>1 - Cs(rho):</label> <%= request.getAttribute("fEval")%>
<br>
<label>Root of az(Cs(z)(1-z)+C(z))-C(z)(1-z+C(z))=0:</label> <%= request.getAttribute("fFund")%>


<% if (request.getAttribute("fOutGraphViz") != null) { %>
<br>
<br>

<label>GraphViz code for AutoClump:
<a href='<%= request.getAttribute("clumpPath") %>?resFile=autoclump.dot&resData=<%= request.getAttribute("aGraph") %>'>as dot</a>
<% if (request.getAttribute("dotPath") != null) { %>
 or <a href='<%= request.getAttribute("clumpPath") %>?resFile=autoclump.svg&resData=<%= request.getAttribute("aGraph") %>'>as svg</a>
<br>
<br>
<center><%= request.getAttribute("aGraphSVG") %></center>
<% } %>
</label>

<br>

<label>GraphViz code for DeepClump:
<a href='<%= request.getAttribute("clumpPath") %>?resFile=deepclump.dot&resData=<%= request.getAttribute("aGraph2") %>'>as dot</a>
<% if (request.getAttribute("dotPath") != null) { %>
 or <a href='<%= request.getAttribute("clumpPath") %>?resFile=deepclump.svg&resData=<%= request.getAttribute("aGraph2") %>'>or svg</a>
<br>
<br>
<center><%= request.getAttribute("aGraphSVG2") %></center>
<% } %>
</label>

<% } %>
<% } else { %>
<%= request.getAttribute("fStatus") %>
<% } %>

<br>
<br>
<center><input onclick="window.history.back();" type="button" value="Back To Previous Page"/></center>


