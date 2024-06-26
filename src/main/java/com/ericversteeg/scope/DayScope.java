package com.ericversteeg.scope;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DayScope extends Scope
{
    public DayScope(Scope parent, int value, long offsetBy)
    {
        super(parent, value, offsetBy);

        if (parent instanceof YearScope)
        {
            temporalField = ChronoField.DAY_OF_YEAR;
        }
        else if (parent instanceof MonthScope)
        {
            temporalField = ChronoField.DAY_OF_MONTH;
        }
        else if (parent instanceof WeekScope)
        {
            temporalField = ChronoField.DAY_OF_WEEK;
        }
    }

    public DayScope(Scope parent, int offset, int interval, long offsetBy)
    {
        super(parent, offset, interval, offsetBy);
    }

    public DayScope(Date estDate, int offset, int interval, long offsetBy)
    {
        super(estDate, offset, interval, offsetBy);
    }

    @Override
    boolean matchesInterval(LocalDateTime refLocalDate, LocalDate nowLocalDate, int offset, int interval)
    {
        LocalDateTime nowSoD = nowLocalDate.atStartOfDay();

        long daysSince = ChronoUnit.DAYS.between(refLocalDate, nowSoD);

        return (daysSince - offset) >= 0 && (daysSince - offset) % interval == 0;
    }
}
