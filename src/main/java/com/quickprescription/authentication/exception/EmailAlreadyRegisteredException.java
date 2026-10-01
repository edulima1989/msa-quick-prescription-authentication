package com.quickprescription.authentication.exception;

public class EmailAlreadyRegisteredException extends ApiException {

    public EmailAlreadyRegisteredException() {
        super(409, "Ya existe un consumidor registrado con ese correo.");
    }
}
