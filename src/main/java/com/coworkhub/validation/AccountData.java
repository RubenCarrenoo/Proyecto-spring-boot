package com.coworkhub.validation;

/** Contrato para DTOs que pueden traer datos opcionales de cuenta (usuario/password). */
public interface AccountData {

    String usuario();

    String password();
}
