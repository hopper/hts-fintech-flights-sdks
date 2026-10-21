package com.hopper.fintech.flights.examples;

import com.hopper.fintech.flights.dafar.model.DafarContract;
import com.hopper.fintech.flights.dafar.model.DafarExerciseQuote;
import com.hopper.fintech.flights.dafar.model.ExerciseUrl;
import com.hopper.fintech.flights.dafar.model.RebookEligibilityResponse;
import com.hopper.fintech.flights.sdk.FlightsDg;

public final class ExerciseDgContract {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: ExerciseDgContract <contractId> [url|eligibility|refund]");
            System.exit(2);
        }
        String contractId = args[0];
        String mode = args.length > 1 ? args[1] : "url";
        FlightsDg dg = Env.htsClient().dg();

        DafarContract contract = dg.contracts().getContractsId(contractId).execute();
        System.out.printf("Contract %s is %s%n", contract.getId(), contract.getStatus());

        switch (mode) {
            case "url": {
                ExerciseUrl url = dg.htsSelfServiceExercise().postContractsIdExerciseUrl(contractId).execute();
                System.out.printf("Send the customer to %s%n", url.getExerciseUrl());
                break;
            }
            case "eligibility": {
                RebookEligibilityResponse eligibility = dg.partnerSelfServiceExercise().getContractsIdRebookEligibility(contractId).execute();
                System.out.println(eligibility.toJson());
                break;
            }
            case "refund": {
                DafarExerciseQuote quote = dg.partnerSelfServiceExercise().postContractsIdExerciseQuotes(contractId).execute();
                System.out.printf("Quote %s with %d payout(s)%n", quote.getId(), quote.getPayouts().size());
                dg.partnerSelfServiceExercise().putExerciseQuotesIdFulfill(quote.getId()).execute();
                System.out.printf("Exercised contract %s for a refund%n", contractId);
                break;
            }
            default:
                System.err.println("unknown mode " + mode);
                System.exit(2);
        }
    }
}
