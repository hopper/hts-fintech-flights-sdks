package com.hopper.fintech.flights.sdk.payments;

import java.util.Objects;

public final class CardDetails {
    private final String firstName;
    private final String lastName;
    private final String number;
    private final String expirationMonth;
    private final String expirationYear;
    private final String verificationValue;

    private CardDetails(Builder builder) {
        this.firstName = Objects.requireNonNull(builder.firstName, "firstName is required");
        this.lastName = Objects.requireNonNull(builder.lastName, "lastName is required");
        this.number = Objects.requireNonNull(builder.number, "number is required");
        this.expirationMonth = Objects.requireNonNull(builder.expirationMonth, "expirationMonth is required");
        this.expirationYear = Objects.requireNonNull(builder.expirationYear, "expirationYear is required");
        this.verificationValue = builder.verificationValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String number() {
        return number;
    }

    public String expirationMonth() {
        return expirationMonth;
    }

    public String expirationYear() {
        return expirationYear;
    }

    public String verificationValue() {
        return verificationValue;
    }

    public String lastFourDigits() {
        return number.substring(Math.max(0, number.length() - 4));
    }

    public static final class Builder {
        private String firstName;
        private String lastName;
        private String number;
        private String expirationMonth;
        private String expirationYear;
        private String verificationValue;

        private Builder() {
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder number(String number) {
            this.number = number;
            return this;
        }

        public Builder expirationMonth(String expirationMonth) {
            this.expirationMonth = expirationMonth;
            return this;
        }

        public Builder expirationYear(String expirationYear) {
            this.expirationYear = expirationYear;
            return this;
        }

        public Builder verificationValue(String verificationValue) {
            this.verificationValue = verificationValue;
            return this;
        }

        public CardDetails build() {
            return new CardDetails(this);
        }
    }
}
