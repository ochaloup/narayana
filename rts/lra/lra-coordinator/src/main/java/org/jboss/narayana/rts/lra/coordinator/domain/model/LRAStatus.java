package org.jboss.narayana.rts.lra.coordinator.domain.model;

//@Data
//@AllArgsConstructor
//@ApiModel( value = "LRA", description = "A Long Running Action" )
public class LRAStatus {
//    @ApiModelProperty( value = "The unique id of the LRA", required = true )
    private String lraId;
//    @ApiModelProperty( value = "The client id associated with this LRA", required = false )
    private String clientId ;
//    @ApiModelProperty( value = "Indicates whether or not this LRA has completed", required = false )
    private boolean isComplete;
//    @ApiModelProperty( value = "Indicates whether or not this LRA has compensated", required = false )
    private boolean isCompensated;
//    @ApiModelProperty( value = "Indicates whether or not this LRA is recovering", required = false )
    private boolean isRecovering;
//    @ApiModelProperty( value = "Indicates whether or not this LRA has been asked to complete or compensate yet", required = false )
    private boolean isActive;
//    @ApiModelProperty( value = "Indicates whether or not this LRA is top level", required = false )
    private boolean isTopLevel;

    public LRAStatus(Transaction lra) {
        this.lraId = lra.getId().toString();
        this. clientId = lra.getClientId();
        this. isComplete = lra.isComplete();
        this. isCompensated = lra.isCompensated();
        this. isRecovering = lra.isRecovering();
        this. isActive = lra.isActive();
        this. isTopLevel = lra.isTopLevel();
    }

    public String getLraId() {
        return lraId;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public boolean isCompensated() {
        return isCompensated;
    }

    public boolean isRecovering() {
        return isRecovering;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isTopLevel() {
        return isTopLevel;
    }
}
