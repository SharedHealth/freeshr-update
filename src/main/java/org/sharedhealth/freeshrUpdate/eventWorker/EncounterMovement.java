package org.sharedhealth.freeshrUpdate.eventWorker;


public class EncounterMovement {

    private String fromPatientId;
    private String toPatientId;
    private String encounterId;
    private String reason;
    private int id;

    public EncounterMovement(int id, String fromPatientId, String toPatientId, String encounterId, String reason) {
        this.id = id;
        this.fromPatientId = fromPatientId;
        this.toPatientId = toPatientId;
        this.encounterId = encounterId;
        this.reason = reason;
    }


    public String getEncounterId() {
        return encounterId;
    }

    public int getId() {
        return id;
    }

    public String getFromPatientId() {
        return fromPatientId;
    }

    public String getToPatientId() {
        return toPatientId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncounterMovement)) return false;

        EncounterMovement that = (EncounterMovement) o;

        if (id != that.id) return false;
        if (!encounterId.equals(that.encounterId)) return false;
        if (!fromPatientId.equals(that.fromPatientId)) return false;
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) return false;
        if (!toPatientId.equals(that.toPatientId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fromPatientId.hashCode();
        result = 31 * result + toPatientId.hashCode();
        result = 31 * result + encounterId.hashCode();
        result = 31 * result + id;
        return result;
    }

    @Override
    public String toString() {
        return "EncounterMovement{" +
                "fromPatientId='" + fromPatientId + '\'' +
                ", toPatientId='" + toPatientId + '\'' +
                ", encounterId='" + encounterId + '\'' +
                ", reason='" + reason + '\'' +
                ", id=" + id +
                '}';
    }
}
