/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.server.types3.mcompat.mfuncs;

import com.akiban.server.error.InvalidParameterValueException;
import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TOverload;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.mcompat.mtypes.MDatetimes;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.mcompat.mtypes.MString;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TOverloadBase;

public abstract class MExtractField extends TOverloadBase
{
    public static TOverload[] INSTANCES = new TOverload[]
    {
        new MExtractField("YEAR", MDatetimes.DATE)
        {
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                return (int) ymd[MDatetimes.YEAR_INDEX];
            }
        },
        new MExtractField("QUARTER", MDatetimes.DATE)
        {
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                int month = (int) ymd[MDatetimes.MONTH_INDEX];

                if (month < 4) return 1;
                else if (month < 7) return 2;
                else if (month < 10) return 3;
                else return 4;
            }
        },
        new MExtractField("MONTH", MDatetimes.DATE)
        {
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                return (int) ymd[MDatetimes.MONTH_INDEX];
            }
        },
        new MExtractField("LAST_DAY", MDatetimes.DATE)
        {
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                return (int) MDatetimes.getLastDay(ymd);
            }
        },
        new MExtractField("DAYOFYEAR", MDatetimes.DATE)
        {
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                return MDatetimes.toJodaDatetime(ymd, context.getCurrentTimezone()).getDayOfYear();
            }
        },
        new MExtractField("DAY", MDatetimes.DATE) // day of month
        {    
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                return (int) ymd[MDatetimes.DAY_INDEX];
            }
        },
        new MExtractField("HOUR", MDatetimes.TIME)
        {
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                return (int) ymd[MDatetimes.HOUR_INDEX];
            }
        },
        new MExtractField("MINUTE", MDatetimes.TIME)
        {
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                return (int) ymd[MDatetimes.MIN_INDEX];
            }
        },
        new MExtractField("SECOND", MDatetimes.TIME)
        {
            @Override
            protected int getField(long[] ymd, TExecutionContext context)
            {
                return (int) ymd[MDatetimes.SEC_INDEX];
            }
        },
        new TOverloadBase() // MONTH_NAME
        {
            @Override
            protected void buildInputSets(TInputSetBuilder builder)
            {
                builder.covers(MDatetimes.DATE, 0);
            }

            @Override
            protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output)
            {
                int numericMonth = (int) MDatetimes.decodeDate(inputs.get(0).getInt32())[MDatetimes.MONTH_INDEX];
                String month = MDatetimes.getMonthName(numericMonth, context.getCurrentLocale(), context);
                output.putString(month, null);
            }

            @Override
            public String displayName()
            {
                return "MONTHNAME";
            }

            @Override
            public TOverloadResult resultType()
            {
                // TODO
                // Could make this better by trying to evaluate the arg,
                // if it's literal to get the exact string length
                return TOverloadResult.fixed(MString.VARCHAR.instance(32));
            }
        }
    };

    protected abstract int getField(long ymd[], TExecutionContext context);
    private final String name;
    private final TClass inputType;
    MExtractField (String name, TClass inputType)
    {
        this.name = name;
        this.inputType = inputType;
    }

    @Override
    protected void buildInputSets(TInputSetBuilder builder)
    {
        builder.covers(inputType, 0);
    }

    @Override
    protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output)
    {
        int val = inputs.get(0).getInt32();
        long ymd[] = MDatetimes.decodeDate(val);
        if (!MDatetimes.isValidDatetime(ymd))
        {
            context.warnClient(new InvalidParameterValueException("Invalid DATETIME value: " + val));
            output.putNull();
        }
        else
            output.putInt32(getField(ymd, context));
    }

    @Override
    public String displayName()
    {
        return name;
    }

    @Override
    public TOverloadResult resultType()
    {
        return TOverloadResult.fixed(MNumeric.INT.instance());
    }
}
