package org.distributedlock;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DistributedLockData {

    private String id;

    @JsonProperty("org_id")
    private String orgId;

    @JsonProperty("locked_by")
    private String lockedBy;

    @JsonProperty("locked_at")
    private Number lockedAt;

    @JsonProperty("locked_till")
    private Number lockedTill;
}
