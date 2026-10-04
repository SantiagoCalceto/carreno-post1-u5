package com.ejemplo.servlet;

import com.ejemplo.model.Tarea;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * GET /tareas/detalle?id=X — muestra el detalle de una tarea.
 * No mantiene una lista propia: lee la que TareasServlet publicó en el
 * ServletContext (applicationScope), así ambos Servlets ven el mismo estado.
 */
@WebServlet(name = "DetalleTareaServlet", urlPatterns = {"/tareas/detalle"})
public class DetalleTareaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int id;
        try {
            id = Integer.parseInt(req.getParameter("id"));
        } catch (NumberFormatException e) {
            // Cubre id ausente (null) o no numérico
            resp.sendRedirect(req.getContextPath() + "/tareas?error=id-invalido");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Tarea> tareas = (List<Tarea>) getServletContext().getAttribute("tareas");

        Tarea tarea = (tareas == null) ? null : tareas.stream()
            .filter(t -> t.getId() == id)
            .findFirst()
            .orElse(null);

        if (tarea == null) {
            resp.sendRedirect(req.getContextPath() + "/tareas?error=no-encontrada");
            return;
        }

        // Se usa forward (no redirect): el objeto Tarea viaja en el request
        // hacia detalle.jsp sin generar una nueva petición del navegador. Un
        // redirect perdería el atributo y cambiaría la URL sin necesidad; PRG
        // solo hace falta tras un POST, y esto es una lectura (GET).
        req.setAttribute("tarea", tarea);
        req.getRequestDispatcher("/WEB-INF/views/detalle.jsp")
           .forward(req, resp);
    }
}
