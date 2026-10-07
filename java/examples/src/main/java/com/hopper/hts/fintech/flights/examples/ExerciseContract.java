package com.hopper.hts.fintech.flights.examples;

import com.hopper.hts.fintech.flights.cfar.model.CfarContract;
import com.hopper.hts.fintech.flights.cfar.model.CfarExerciseQuote;
import com.hopper.hts.fintech.flights.cfar.model.CreateCfarExerciseQuoteRequest;
import com.hopper.hts.fintech.flights.cfar.model.ExerciseUrl;
import com.hopper.hts.fintech.flights.cfar.model.FulfillExerciseQuoteRequest;
import com.hopper.hts.fintech.flights.cfar.model.PayoutOption;
import com.hopper.hts.fintech.flights.sdk.FlightsCfarClient;

import java.util.UUID;

public final class ExerciseContract {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: ExerciseContract <contractId> [self-service|url]");
            System.exit(2);
        }
        String contractId = args[0];
        String mode = args.length > 1 ? args[1] : "self-service";
        FlightsCfarClient hts = Env.htsClient();

        CfarContract contract = hts.contracts().getContractsId(contractId);
        System.out.printf("Contract %s is %s%n", contract.getId(), contract.getStatus());

        if (mode.equals("url")) {
            ExerciseUrl url = hts.htsSelfServiceExercise().postContractsIdExerciseUrl(contractId);
            System.out.printf("Send the customer to %s%n", url.getExerciseUrl());
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        CfarExerciseQuote quote = hts.partnerSelfServiceExercise().postContractsIdExerciseQuotes(
            contractId, new CreateCfarExerciseQuoteRequest().sessionId(sessionId));
        System.out.printf("Quote %s with %d payout option(s)%n", quote.getId(), quote.getPayoutOptions().size());

        PayoutOption chosen = quote.getPayoutOptions().get(0);
        hts.partnerSelfServiceExercise().putExerciseQuotesIdFulfill(
            quote.getId(), new FulfillExerciseQuoteRequest().payoutOptionId(chosen.getId()).sessionId(sessionId));
        System.out.printf("Exercised contract %s using payout option %s%n", contractId, chosen.getId());
    }
}
