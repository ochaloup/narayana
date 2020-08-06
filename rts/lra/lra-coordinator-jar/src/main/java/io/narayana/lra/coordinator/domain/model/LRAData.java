package io.narayana.lra.coordinator.domain.model;

import org.eclipse.microprofile.lra.annotation.LRAStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LRAData {
    private final String lraId;
    private final String clientId;
    private final LRAStatus lraStatus;
    // TODO: is this needed?
    /*private final boolean isClosed;
    private final boolean isCancelled;
    private final boolean isRecovering;
    private final boolean isActive; */
    private final boolean isTopLevel;
    private final long startTime;
    private final long finishTime;
    // todo: isRecovering is needed?
    // todo: failedParticipants to be used?
    // todo: responseData
    // todo: httpStatus - vs. String status?
    // todo: timeNow is needed?

    public LRAData(String lraId, String clientId, LRAStatus lraStatus,
                           boolean isClosed, boolean isCancelled, boolean isRecovering,
                           boolean isActive, boolean isTopLevel,
                           long startTime, long finishTime) {
        this.lraId = lraId;
        this.clientId = clientId;
        this.lraStatus = lraStatus;

        this.isTopLevel = isTopLevel;
        this.startTime = startTime;
        this.finishTime = finishTime;
    }

    private LRAData(LRAData.Builder builder) {
        this.lraId = builder.lraId;
        this.clientId = builder.clientId;
        this.lraStatus = builder.lraStatus;
        this.isTopLevel = builder.isTopLevel;
        this.startTime = builder.startTime;
        this.finishTime = builder.finishTime;
    }

    public String getLraId() {
        return this.lraId;
    }

    public String getClientId() {
        return this.clientId;
    }

    public LRAStatus getLraStatus() {
        return this.lraStatus;
    }

    /*
    public boolean isClosed() {
        return this.isClosed;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public boolean isRecovering() {
        return this.isRecovering;
    }

    public boolean isActive() {
        return this.isActive;
    }
*/
    public boolean isTopLevel() {
        return this.isTopLevel;
    }


    public long getStartTime() {
        return startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public long getTimeNow() {
        return LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public ZoneOffset getZoneOffset() {
        return ZoneOffset.UTC;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof LRAData)) {
            return false;
        } else {
            LRAData lraData = (LRAData) o;
            return this.getLraId().equals(lraData.getLraId());
        }
    }

    public int hashCode() {
        return this.getLraId().hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "lraId='" + lraId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", status='" + lraStatus + '\'' +
                // TODO: is needed
                /* ", isClosed=" + isClosed +
                ", isCancelled=" + isCancelled +
                ", isRecovering=" + isRecovering +
                ", isActive=" + isActive + */
                ", isTopLevel=" + isTopLevel +
                ", startTime=" + startTime +
                ", finishTime=" + finishTime +
                '}';
    }

    public static class Builder implements BuilderWithLraId, BuilderWithClientId, BuilderWithStatus, BuilderWithTopLevel,
            BuilderWithStartTime, BuilderWithFinishTime, BuilderFinal {
        private String lraId;
        private String clientId;
        private LRAStatus lraStatus;
        private boolean isTopLevel;
        private long startTime;
        private long finishTime;

        private Builder() {
            // no public instantiation
        }

        public static BuilderWithLraId instanceOf() {
            return new Builder();
        }

        public BuilderWithClientId lraId(String lraId) {
            this.lraId = lraId;
            return this;
        }
        public BuilderWithStatus clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }
        public BuilderWithTopLevel status(LRAStatus lraStatus) {
            this.lraStatus = lraStatus;
            return this;
        }
        public BuilderWithStartTime topLevel(boolean isTopLevel) {
            this.isTopLevel = isTopLevel;
            return this;
        }
        public BuilderWithFinishTime startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        public BuilderFinal finishTime(long finishTime) {
            this.finishTime = finishTime;
            return this;
        }
        public LRAData build() {
            return new LRAData(this);
        }
    }

    public static interface BuilderWithLraId {
        BuilderWithClientId lraId(String lraId);
    }
    public static interface BuilderWithClientId {
        BuilderWithStatus clientId(String clientId);
    }
    public static interface BuilderWithStatus {
        BuilderWithTopLevel status(LRAStatus lraStatus);
    }
    public static interface BuilderWithTopLevel {
        BuilderWithStartTime topLevel(boolean isTopLevel);
    }
    public static interface BuilderWithStartTime {
        BuilderWithFinishTime startTime(long startTime);
    }
    public static interface BuilderWithFinishTime {
        BuilderFinal finishTime(long finishTime);
    }
    public static interface BuilderFinal {
        LRAData build();
    }
}
