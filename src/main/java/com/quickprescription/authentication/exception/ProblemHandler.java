package com.quickprescription.authentication.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Todos los errores salen en application/problem+json (RFC 9457) con title, status, detail e instance.
 * Los detail son textos fijos: nunca se usa el mensaje de excepciones de Spring o Jackson,
 * porque puede incluir valores del cuerpo (correo o contraseña).
 */
@RestControllerAdvice
public class ProblemHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemHandler.class);
    private static final Map<Integer, String> TITLES = Map.of(
            400, "Solicitud inválida",
            401, "No autenticado",
            404, "Recurso no encontrado",
            405, "Método no permitido",
            406, "No aceptable",
            409, "Conflicto",
            415, "Tipo de contenido no soportado",
            500, "Error interno");
    static final String INTERNAL_DETAIL = "Ocurrió un error inesperado. Intente nuevamente más tarde.";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> api(ApiException e, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (e.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return respond(e, HttpStatusCode.valueOf(e.getStatus()), e.getMessage(), null, headers, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception e, WebRequest request) {
        // Solo clase y ubicación: el mensaje o las causas pueden contener datos de la solicitud.
        StackTraceElement[] stackTrace = e.getStackTrace();
        log.error("Error inesperado en {} {}: {} en {}", method(request), path(request), e.getClass().getName(),
                stackTrace.length > 0 ? stackTrace[0] : "?");
        return respond(e, HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_DETAIL, null, new HttpHeaders(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorCampo(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(e, status, "La solicitud no es válida.", errores, new HttpHeaders(), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = e.getCause() == null ? "El cuerpo de la solicitud es obligatorio."
                : "El cuerpo no es un JSON válido o algún campo tiene un tipo o formato inválido.";
        return respond(e, status, detail, null, new HttpHeaders(), request);
    }

    /** Punto de salida común (también para lo que resuelve la clase base): title, instance y Content-Type. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = body instanceof ProblemDetail pd ? pd : ProblemDetail.forStatus(status);
        problem.setTitle(TITLES.getOrDefault(status.value(), problem.getTitle()));
        problem.setInstance(URI.create(path(request)));
        HttpHeaders out = new HttpHeaders();
        out.putAll(headers);
        out.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problem, out, status);
    }

    private ResponseEntity<Object> respond(Exception e, HttpStatusCode status, String detail,
            List<ErrorCampo> errores, HttpHeaders headers, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        if (errores != null) {
            problem.setType(URI.create("/problems/validation-error"));
            problem.setProperty("errores", errores);
        }
        return handleExceptionInternal(e, problem, headers, status, request);
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest s ? s.getRequest().getRequestURI() : "/";
    }

    private static String method(WebRequest request) {
        return request instanceof ServletWebRequest s ? s.getRequest().getMethod() : "?";
    }

    record ErrorCampo(String campo, String mensaje) {
    }
}
