package com.quickprescription.authentication.exception;

/** Mismo mensaje si el correo no existe o si la contraseña es errónea: no revela qué correos existen. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(401, "Correo o contraseña incorrectos.");
    }
}
