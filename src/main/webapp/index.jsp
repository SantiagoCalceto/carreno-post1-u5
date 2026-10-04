<%@ page contentType="text/html;charset=UTF-8" %>
<%-- Página de bienvenida: redirige al controlador de tareas --%>
<% response.sendRedirect(request.getContextPath() + "/tareas"); %>
