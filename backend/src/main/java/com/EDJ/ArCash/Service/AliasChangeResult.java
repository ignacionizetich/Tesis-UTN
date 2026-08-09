package com.EDJ.ArCash.Service;

/**
 * Desenlaces posibles de un cambio de alias de cuenta. Permite que el service
 * exprese que paso sin decidir como se traduce a HTTP: de eso se ocupa el controller.
 */
public enum AliasChangeResult {
    OK,
    FORMATO_INVALIDO,
    CUENTA_NO_ENCONTRADA,
    NO_ES_PROPIETARIO,
    ALIAS_EN_USO
}
