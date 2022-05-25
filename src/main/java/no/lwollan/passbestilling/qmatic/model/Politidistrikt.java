package no.lwollan.passbestilling.qmatic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class Politidistrikt {

    @JsonProperty
    private List<Integer> branchIds;
    @JsonProperty
    private List<Map<String, ?>> branches;
    @JsonProperty
    private String fullName;
    @JsonProperty
    private Integer id;
    @JsonProperty
    private String name;
    @JsonProperty
    private List<Integer> subGroupIds;

    public Politidistrikt() {
    }

    public List<Integer> getBranchIds() {
        return branchIds;
    }

    public void setBranchIds(List<Integer> branchIds) {
        this.branchIds = branchIds;
    }

    public List<Map<String, ?>> getBranches() {
        return branches;
    }

    public void setBranches(List<Map<String, ?>> branches) {
        this.branches = branches;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getSubGroupIds() {
        return subGroupIds;
    }

    public void setSubGroupIds(List<Integer> subGroupIds) {
        this.subGroupIds = subGroupIds;
    }
}
