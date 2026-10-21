package com.hopper.fintech.flights.examples;

import com.hopper.fintech.flights.cfar.model.CfarContract;
import com.hopper.fintech.flights.cfar.model.CfarExerciseQuote;
import com.hopper.fintech.flights.cfar.model.CreateCfarExerciseQuoteRequest;
import com.hopper.fintech.flights.cfar.model.ExerciseUrl;
import com.hopper.fintech.flights.cfar.model.FulfillExerciseQuoteRequest;
import com.hopper.fintech.flights.cfar.model.PayoutOption;
import com.hopper.fintech.flights.sdk.FlightsCfar;

import java.util.UUID;

public final class ExerciseCfarContract {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: ExerciseCfarContract <contractId> [self-service|url]");
            System.exit(2);
        }
        String contractId = args[0];
        String mode = args.length > 1 ? args[1] : "self-service";
        FlightsCfar cfar = Env.htsClient().cfar();

        CfarContract contract = cfar.contracts().getContractsId(contractId).execute();
        System.out.printf("Contract %s is %s%n", contract.getId(), contract.getStatus());

        if (mode.equals("url")) {
            ExerciseUrl url = cfar.htsSelfServiceExercise().postContractsIdExerciseUrl(contractId).execute();
            System.out.printf("Send the customer to %s%n", url.getExerciseUrl());
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        CfarExerciseQuote quote = cfar.partnerSelfServiceExercise().postContractsIdExerciseQuotes(
            contractId, new CreateCfarExerciseQuoteRequest().sessionId(sessionId)).execute();
        System.out.printf("Quote %s with %d payout option(s)%n", quote.getId(), quote.getPayoutOptions().size());

        PayoutOption chosen = quote.getPayoutOptions().get(0);
        cfar.partnerSelfServiceExercise().putExerciseQuotesIdFulfill(
            quote.getId(), new FulfillExerciseQuoteRequest().payoutOptionId(chosen.getId()).sessionId(sessionId)).execute();
        System.out.printf("Exercised contract %s using payout option %s%n", contractId, chosen.getId());
    }
}
