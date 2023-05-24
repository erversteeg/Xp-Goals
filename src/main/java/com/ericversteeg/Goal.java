package com.ericversteeg;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedList;
import java.util.List;

public class Goal {
    static final int resetHourly = 0;
    static final int resetDaily = 1;
    static final int resetWeekly = 2;
    static final int resetMonthly = 3;
    static final int resetYearly = 4;

    public Goal(int skillId)
    {
        this.skillId = skillId;
    }

    @SerializedName("skill_id")
    int skillId = 0;

    @SerializedName("reset_type")
    int resetType = resetDaily;

    @SerializedName("last_reset")
    long lastReset = 0L;

    @SerializedName("start_xp")
    int startXp = -1;

    @SerializedName("current_xp")
    int currentXp = -1;

    @SerializedName("previous_results")
    List<Boolean> previousResults = new LinkedList<>();

    int goalXp = 0;

    boolean enabled = false;
    boolean track = false;

    void reset()
    {
        previousResults.add(currentXp >= startXp + goalXp);

        startXp = -1;
        currentXp = -1;
    }
}
