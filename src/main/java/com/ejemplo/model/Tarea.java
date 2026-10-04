package com.ejemplo.model;

import java.util.Date;

/**
 * Modelo de una tarea de la lista. Es un JavaBean sencillo: los getters
 * permiten que las vistas JSP accedan a sus propiedades con EL (${t.titulo}).
 * Se usa java.util.Date para la fecha límite porque es el tipo que admite
 * directamente la etiqueta JSTL fmt:formatDate en detalle.jsp.
 */
public class Tarea {
    private int id;
    private String titulo;
    private boolean completada;
    private String categoria;
    private String prioridad;   // "Alta", "Media" o "Baja"
    private Date fechaLimite;

    public Tarea(int id, String titulo, String categoria,
                 String prioridad, Date fechaLimite) {
        this.id = id;
        this.titulo = titulo;
        this.completada = false;
        this.categoria = categoria;
        this.prioridad = prioridad;
        this.fechaLimite = fechaLimite;
    }

    // Getters y setters
    public int getId()            { return id; }
    public String getTitulo()     { return titulo; }
    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) {
        this.completada = completada;
    }
    public String getCategoria()  { return categoria; }
    public String getPrioridad()  { return prioridad; }
    public Date getFechaLimite()  { return fechaLimite; }
}
