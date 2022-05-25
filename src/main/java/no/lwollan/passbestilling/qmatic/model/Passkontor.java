package no.lwollan.passbestilling.qmatic.model;

/**
 * A Passkontor is a location where one can access a number of services. This model only keeps the
 * id of the branch, service id for passport and a friendly name.
 */
public class Passkontor {

    public final String branchId;
    public final String name;

    public Passkontor(String branchId, String name) {
        this.branchId = branchId;
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("[name=%s] [branchId=%s]", name, branchId);
    }
}
