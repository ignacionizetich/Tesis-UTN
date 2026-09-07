package com.EDJ.ArCash.Service.result;

public final class QrDataResult {

    public enum Kind {
        OK,
        CUENTA_NO_ENCONTRADA,
        NO_ES_PROPIETARIO
    }

    private final Kind kind;
    private final QrPayload payload;

    private QrDataResult(Kind kind, QrPayload payload) {
        this.kind = kind;
        this.payload = payload;
    }

    public static QrDataResult ok(QrPayload payload) {
        return new QrDataResult(Kind.OK, payload);
    }

    public static QrDataResult cuentaNoEncontrada() {
        return new QrDataResult(Kind.CUENTA_NO_ENCONTRADA, null);
    }

    public static QrDataResult noEsPropietario() {
        return new QrDataResult(Kind.NO_ES_PROPIETARIO, null);
    }

    public Kind getKind() {
        return kind;
    }

    public QrPayload getPayload() {
        return payload;
    }

    /**
     * Contenido que el frontend codifica en el QR de cobro.
     *
     * <p>Se serializa directamente: los nombres de los componentes ya son las claves JSON que
     * espera el cliente, asi que copiarlos a un Map solo agregaba un lugar donde equivocarse.
     */
    public record QrPayload(
            String walletApp,
            Long accountId,
            String accountAlias,
            String receiverName,
            String dni,
            String email,
            String currency
    ) {
    }
}
