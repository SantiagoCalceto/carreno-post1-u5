package com.ejemplo.servlet;

import com.ejemplo.model.Tarea;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Controlador de la lista de tareas.
 * GET  /tareas -> lista con filtro combinado (texto + categoría + prioridad).
 * POST /tareas -> agregar, eliminar, completar o identificar usuario; siempre
 *                 termina en redirect (patrón Post/Redirect/Get).
 *
 * loadOnStartup = 1 hace que init() se ejecute al desplegar la aplicación y no
 * en la primera petición a /tareas. Así la lista ya está publicada en el
 * ServletContext aunque el primer acceso sea directo a /tareas/detalle?id=X.
 */
@WebServlet(name = "TareasServlet", urlPatterns = {"/tareas"}, loadOnStartup = 1)
public class TareasServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/tareas.jsp";
    private static final long UN_DIA_MS = 24L * 60 * 60 * 1000;

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
        Date hoy = new Date();
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Leer documentación de Servlets",
            "Estudio", "Alta", sumarDias(hoy, 2)));
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Implementar ciclo GET/POST",
            "Estudio", "Alta", sumarDias(hoy, 3)));
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Preparar sustentación del laboratorio",
            "Evaluación", "Media", sumarDias(hoy, 7)));
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Revisar JSTL y Expression Language",
            "Estudio", "Baja", sumarDias(hoy, 10)));

        // Se publica la MISMA lista (no una copia) en el ServletContext
        // (applicationScope) para que DetalleTareaServlet la lea sin duplicar estado.
        getServletContext().setAttribute("tareas", tareas);
    }

    private Date sumarDias(Date base, int dias) {
        return new Date(base.getTime() + dias * UN_DIA_MS);
    }

    /** GET /tareas — listar con filtro combinado. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        mostrarLista(req, resp);
    }

    /** POST /tareas — agregar, eliminar, completar o identificar usuario. */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");

        if ("agregar".equals(accion)) {
            // "titulo" es dato de ESTA petición: variable local, nunca de instancia
            String titulo = req.getParameter("titulo");
            if (titulo == null || titulo.isBlank()) {
                // Validación en el servidor. Se hace forward (no redirect) para que
                // el mensaje de error, que vive en el request, llegue a la vista.
                req.setAttribute("error", "El título no puede estar vacío");
                mostrarLista(req, resp);
                return;
            }
            // Valores por defecto: el formulario rápido de la Parte 1 solo pide título
            tareas.add(new Tarea(contadorId.getAndIncrement(), titulo.trim(),
                "General", "Media", sumarDias(new Date(), 5)));

        } else if ("eliminar".equals(accion)) {
            Integer id = parsearId(req.getParameter("id"));
            if (id != null) {
                tareas.removeIf(t -> t.getId() == id);
            }

        } else if ("completar".equals(accion)) {
            Integer id = parsearId(req.getParameter("id"));
            if (id != null) {
                tareas.stream()
                    .filter(t -> t.getId() == id)
                    .findFirst()
                    .ifPresent(t -> t.setCompletada(true));
            }

        } else if ("identificar".equals(accion)) {
            String nombre = req.getParameter("nombre");
            if (nombre != null && !nombre.isBlank()) {
                // En sesión y no en el request: el nombre debe sobrevivir a
                // todas las peticiones siguientes de este usuario.
                req.getSession().setAttribute("usuario", nombre.trim());
            }
        }

        // Patrón PRG: tras cualquier POST se redirige a un GET limpio, así
        // recargar la página (F5) no vuelve a enviar el formulario.
        resp.sendRedirect(req.getContextPath() + "/tareas");
    }

    /**
     * Calcula la lista filtrada y hace forward a la vista. Se usa tanto en el GET
     * normal como cuando la validación del POST falla, para que la vista reciba
     * siempre los mismos atributos (lista, categorías y filtro activo).
     */
    private void mostrarLista(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        boolean hayFiltroEnUrl = req.getParameter("q") != null
            || req.getParameter("cat") != null
            || req.getParameter("prioridad") != null;

        String filtroTexto;
        String filtroCategoria;
        String filtroPrioridad;

        if (hayFiltroEnUrl) {
            // Filtro nuevo: se aplica y se recuerda en sesión. Se guarda en sesión
            // (no en el request) porque debe sobrevivir a la navegación entre
            // /tareas y /tareas/detalle sin que el usuario lo repita.
            filtroTexto     = req.getParameter("q");
            filtroCategoria = req.getParameter("cat");
            filtroPrioridad = req.getParameter("prioridad");
            session.setAttribute("filtroTexto", filtroTexto);
            session.setAttribute("filtroCategoria", filtroCategoria);
            session.setAttribute("filtroPrioridad", filtroPrioridad);
        } else {
            // Sin parámetros (clic en "Volver", redirect tras un POST...):
            // se restaura el último filtro guardado en la sesión, si existe.
            filtroTexto     = (String) session.getAttribute("filtroTexto");
            filtroCategoria = (String) session.getAttribute("filtroCategoria");
            filtroPrioridad = (String) session.getAttribute("filtroPrioridad");
        }

        // Cada filtro vacío o ausente no restringe; los presentes se combinan con AND
        List<Tarea> resultado = tareas.stream()
            .filter(t -> estaVacio(filtroTexto)
                         || t.getTitulo().toLowerCase().contains(filtroTexto.trim().toLowerCase()))
            .filter(t -> estaVacio(filtroCategoria) || t.getCategoria().equals(filtroCategoria))
            .filter(t -> estaVacio(filtroPrioridad) || t.getPrioridad().equals(filtroPrioridad))
            .collect(Collectors.toList());

        List<String> categorias = tareas.stream()
            .map(Tarea::getCategoria).distinct().sorted()
            .collect(Collectors.toList());

        // La lista filtrada es solo de request: es el resultado de esta respuesta
        req.setAttribute("tareas", resultado);
        req.setAttribute("categorias", categorias);
        req.setAttribute("filtroTexto", filtroTexto);
        req.setAttribute("filtroCategoria", filtroCategoria);
        req.setAttribute("filtroPrioridad", filtroPrioridad);
        req.getRequestDispatcher(VISTA).forward(req, resp);
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
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
