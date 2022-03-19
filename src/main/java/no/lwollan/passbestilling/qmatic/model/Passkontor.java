package no.lwollan.passbestilling.qmatic.model;

/**
 * A Passkontor is a location where one can access a number of services. This model only keeps the
 * id of the branch, service id for passport and a friendly name.
 */
public class Passkontor {

    public final String branchId;
    public final String onlyPassId;
    public final String name;

    public Passkontor(String branchId, String onlyPassId, String name) {
        this.branchId = branchId;
        this.onlyPassId = onlyPassId;
        this.name = name;
    }
}