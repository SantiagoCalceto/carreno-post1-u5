package com.ejemplo.servlet;

import com.ejemplo.model.Tarea;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controlador de la lista de tareas.
 * GET  /tareas -> muestra la lista (forward a tareas.jsp).
 * POST /tareas -> agrega o elimina una tarea y redirige (patrón Post/Redirect/Get).
 */
@WebServlet(name = "TareasServlet", urlPatterns = {"/tareas"})
public class TareasServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/tareas.jsp";

    /*
     * La lista es una variable de instancia a propósito: no guarda el dato de una
     * petición individual, sino el estado compartido de toda la aplicación (todas
     * las peticiones deben ver la misma lista). Como el contenedor usa una sola
     * instancia del Servlet atendida por varios hilos a la vez, se usan estructuras
     * seguras para concurrencia: CopyOnWriteArrayList (lecturas frecuentes,
     * escrituras escasas) y AtomicInteger para generar ids sin condiciones de carrera.
     */
    private final List<Tarea> tareas = new CopyOnWriteArrayList<>();
    private final AtomicInteger contadorId = new AtomicInteger(1);

    @Override
    public void init() throws ServletException {
        // Datos de ejemplo al iniciar
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Leer documentación de Servlets"));
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Implementar ciclo GET/POST"));
    }

    /** GET /tareas — mostrar lista. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("tareas", tareas);
        req.getRequestDispatcher(VISTA).forward(req, resp);
    }

    /** POST /tareas — agregar o eliminar tarea. */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");

        if ("agregar".equals(accion)) {
            // "titulo" es dato de ESTA petición: variable local, nunca de instancia
            String titulo = req.getParameter("titulo");
            if (titulo == null || titulo.isBlank()) {
                // Validación en el servidor: aunque el input HTML tenga "required",
                // una petición puede llegar sin pasar por el formulario.
                // Aquí se hace forward (no redirect) para conservar el mensaje de error.
                req.setAttribute("error", "El título no puede estar vacío");
                req.setAttribute("tareas", tareas);
                req.getRequestDispatcher(VISTA).forward(req, resp);
                return;
            }
            tareas.add(new Tarea(contadorId.getAndIncrement(), titulo.trim()));

        } else if ("eliminar".equals(accion)) {
            Integer id = parsearId(req.getParameter("id"));
            if (id != null) {
                tareas.removeIf(t -> t.getId() == id);
            }
        }

        // Patrón PRG: tras un POST se redirige a un GET limpio, así recargar
        // la página (F5) no vuelve a enviar el formulario.
        resp.sendRedirect(req.getContextPath() + "/tareas");
    }

    /** Convierte el parámetro id a entero; devuelve null si falta o no es numérico. */
    private Integer parsearId(String valor) {
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
