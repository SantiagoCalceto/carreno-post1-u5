<%@ page contentType="text/html;charset=UTF-8" %>
<%-- URIs de Jakarta Tags 3.0 (equivalentes modernos de http://java.sun.com/jsp/jstl/...) --%>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Gestión de Tareas</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/estilos.css">
</head>
<body>
    <h1>Lista de Tareas</h1>

    <!-- Saludo de sesión: si el usuario ya se identificó, se le saluda -->
    <c:choose>
        <c:when test="${not empty sessionScope.usuario}">
            <p class="saludo">Hola, <c:out value="${sessionScope.usuario}"/></p>
        </c:when>
        <c:otherwise>
            <form method="post" action="${pageContext.request.contextPath}/tareas">
                <input type="hidden" name="accion" value="identificar">
                <input type="text" name="nombre" placeholder="¿Cómo te llamas?" required>
                <button type="submit">Identificarme</button>
            </form>
        </c:otherwise>
    </c:choose>

    <!-- Mensajes de error: validación del servidor o detalle no encontrado -->
    <c:if test="${not empty error}">
        <p class="error"><c:out value="${error}"/></p>
    </c:if>
    <c:if test="${param.error == 'no-encontrada'}">
        <p class="error">La tarea solicitada no existe.</p>
    </c:if>
    <c:if test="${param.error == 'id-invalido'}">
        <p class="error">El identificador de la tarea no es válido.</p>
    </c:if>

    <!-- Filtro combinado: texto + categoría + prioridad (GET: la URL es compartible) -->
    <form method="get" action="${pageContext.request.contextPath}/tareas" class="filtro">
        <%-- fn:escapeXml dentro del atributo: el texto lo escribe el usuario --%>
        <input type="text" name="q" value="${fn:escapeXml(filtroTexto)}" placeholder="Buscar por título...">
        <select name="cat">
            <option value="">Todas las categorías</option>
            <c:forEach var="cat" items="${categorias}">
                <option value="${fn:escapeXml(cat)}" ${cat == filtroCategoria ? "selected" : ""}><c:out value="${cat}"/></option>
            </c:forEach>
        </select>
        <select name="prioridad">
            <option value="">Cualquier prioridad</option>
            <option value="Alta"  ${filtroPrioridad == "Alta"  ? "selected" : ""}>Alta</option>
            <option value="Media" ${filtroPrioridad == "Media" ? "selected" : ""}>Media</option>
            <option value="Baja"  ${filtroPrioridad == "Baja"  ? "selected" : ""}>Baja</option>
        </select>
        <button type="submit">Filtrar</button>
        <a href="${pageContext.request.contextPath}/tareas?q=&amp;cat=&amp;prioridad=">Limpiar filtro</a>
    </form>
    <p>${fn:length(tareas)} tarea(s) encontrada(s)</p>

    <!-- Formulario agregar tarea -->
    <form method="post" action="${pageContext.request.contextPath}/tareas">
        <input type="hidden" name="accion" value="agregar">
        <input type="text"   name="titulo" placeholder="Nueva tarea..." required>
        <button type="submit">Agregar</button>
    </form>

    <!-- Tabla de tareas -->
    <table>
        <thead><tr>
            <th>#</th><th>Título</th><th>Categoría</th><th>Prioridad</th>
            <th>Estado</th><th>Detalle</th><th>Acción</th>
        </tr></thead>
        <tbody>
        <c:forEach var="t" items="${tareas}">
            <tr>
                <td>${t.id}</td>
                <%-- c:out escapa HTML: el título lo escribe el usuario (evita XSS) --%>
                <td class="${t.completada ? 'completada' : ''}"><c:out value="${t.titulo}"/></td>
                <td><c:out value="${t.categoria}"/></td>
                <td><c:out value="${t.prioridad}"/></td>
                <td>${t.completada ? "Completada" : "Pendiente"}</td>
                <td>
                    <a href="${pageContext.request.contextPath}/tareas/detalle?id=${t.id}">Ver</a>
                </td>
                <td>
                    <form method="post"
                          action="${pageContext.request.contextPath}/tareas">
                        <input type="hidden" name="accion" value="eliminar">
                        <input type="hidden" name="id"     value="${t.id}">
                        <button type="submit">Eliminar</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</body>
</html>
