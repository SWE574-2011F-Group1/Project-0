<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<html>

<head><title>Add Article</title></head>

<body>

<h1>Add Article</h1>

<c:url var="viewArticlesUrl" value="/articles.html" />

<a href="${viewArticlesUrl}">Show All Articles</a>

<br />

<br />

<c:url var="saveArticleUrl" value="/articles/save.html" />

<form:form modelAttribute="article" method="POST" action="${saveArticleUrl}">

<form:label path="articleName">Article Name:</form:label>

<form:input path="articleName" />

<br />

<form:label path="articleDesc">Article Desc:</form:label>

<form:textarea path="articleDesc" />

<br />

<input type="submit" value="Save Article" />

</form:form>

</body>

</html>