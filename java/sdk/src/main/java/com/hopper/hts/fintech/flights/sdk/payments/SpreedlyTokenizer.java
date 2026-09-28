package com.hopper.hts.fintech.flights.sdk.payments;

import com.hopper.hts.fintech.spreedly.ApiClient;
import com.hopper.hts.fintech.spreedly.ApiException;
import com.hopper.hts.fintech.spreedly.api.PaymentApi;
import com.hopper.hts.fintech.spreedly.model.CreateCreditCard;
import com.hopper.hts.fintech.spreedly.model.CreatePaymentMethod;
import com.hopper.hts.fintech.spreedly.model.CreatePaymentMethodRequest;
import com.hopper.hts.fintech.spreedly.model.CreatePaymentMethodResponse;

import java.util.Objects;

public final class SpreedlyTokenizer {
    private static final String ENCRYPTED_FIELDS = "number,month,year,first_name,last_name";

    private final PaymentApi paymentApi;
    private final String encryptionCertificateToken;
    private final RsaEncryption encryption;

    private SpreedlyTokenizer(Builder builder) {
        ApiClient apiClient = new ApiClient();
        if (builder.baseUrl != null) {
            apiClient.setBasePath(builder.baseUrl);
        }
        apiClient.setUsername(Objects.requireNonNull(builder.environmentKey, "environmentKey is required"));
        apiClient.setPassword(Objects.requireNonNull(builder.accessSecret, "accessSecret is required"));
        this.paymentApi = new PaymentApi(apiClient);
        this.encryptionCertificateToken = builder.encryptionCertificateToken;
        this.encryption = builder.encryptionPublicKey == null ? null : new RsaEncryption(builder.encryptionPublicKey);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String tokenize(CardDetails card) throws ApiException {
        CreatePaymentMethodResponse response = paymentApi.postPaymentMethod(buildRequest(card));
        return response.getTransaction().getPaymentMethod().getToken();
    }

    CreatePaymentMethodRequest buildRequest(CardDetails card) {
        CreateCreditCard creditCard = new CreateCreditCard()
            .firstName(protect(card.firstName()))
            .lastName(protect(card.lastName()))
            .number(protect(card.number()))
            .month(protect(card.expirationMonth()))
            .year(protect(card.expirationYear()))
            .verificationValue(card.verificationValue());

        CreatePaymentMethod paymentMethod = new CreatePaymentMethod().creditCard(creditCard);
        if (encryption != null) {
            paymentMethod
                .encryptionCertificateToken(encryptionCertificateToken)
                .encryptedFields(ENCRYPTED_FIELDS);
        }
        return new CreatePaymentMethodRequest().paymentMethod(paymentMethod);
    }

    private String protect(String value) {
        return encryption == null ? value : encryption.encrypt(value);
    }

    public static final class Builder {
        private String baseUrl;
        private String environmentKey;
        private String accessSecret;
        private String encryptionCertificateToken;
        private String encryptionPublicKey;

        private Builder() {
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder environmentKey(String environmentKey) {
            this.environmentKey = environmentKey;
            return this;
        }

        public Builder accessSecret(String accessSecret) {
            this.accessSecret = accessSecret;
            return this;
        }

        public Builder encryption(String certificateToken, String base64PublicKey) {
            this.encryptionCertificateToken = Objects.requireNonNull(certificateToken, "certificateToken is required");
            this.encryptionPublicKey = Objects.requireNonNull(base64PublicKey, "base64PublicKey is required");
            return this;
        }

        public SpreedlyTokenizer build() {
            return new SpreedlyTokenizer(this);
        }
    }
}
