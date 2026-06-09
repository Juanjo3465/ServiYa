package com.parosurvivors.serviya.users.application.dto.command;

/**
 * Command interno del flujo compartido de creacion de usuario (UserCreationService.createUserAccount),
 * reutilizado por register (visitante) y createUserByAdmin (admin). No proviene directamente de un Form:
 * lo arma el orquestador combinando credenciales + datos de perfil + rol + consentimiento.
 * Sustituye al antiguo placeholder CreateUserData.
 * TODO: revisar campos contra documentacion-BD.docx.
 */
public record CreateUserAccountCommand(
        String email,
        String password,
        String fullName,
        String role,
        String documentType,
        String documentNumber,
        String phone,
        Boolean acceptedTerms) {
}
