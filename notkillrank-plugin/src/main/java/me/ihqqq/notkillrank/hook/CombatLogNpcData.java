package me.ihqqq.notkillrank.hook;

import java.util.UUID;

public class CombatLogNpcData {

    private final UUID originalPlayerUUID;
    private final String originalPlayerName;
    private final UUID trackedAttackerUUID;
    private final int pendingEloGain;
    private final boolean attackerGains;
    private int taskId = -1;

    public CombatLogNpcData(UUID originalPlayerUUID, String originalPlayerName,
                            UUID trackedAttackerUUID, int pendingEloGain, boolean attackerGains) {
        this.originalPlayerUUID = originalPlayerUUID;
        this.originalPlayerName = originalPlayerName;
        this.trackedAttackerUUID = trackedAttackerUUID;
        this.pendingEloGain = pendingEloGain;
        this.attackerGains = attackerGains;
    }

    public UUID getOriginalPlayerUUID()  { return originalPlayerUUID;  }
    public String getOriginalPlayerName(){ return originalPlayerName;  }
    public UUID getTrackedAttackerUUID() { return trackedAttackerUUID; }
    public int getPendingEloGain()       { return pendingEloGain;       }
    public boolean isAttackerGains()     { return attackerGains;        }
    public int getTaskId()               { return taskId;               }
    public void setTaskId(int taskId)    { this.taskId = taskId;        }
}
