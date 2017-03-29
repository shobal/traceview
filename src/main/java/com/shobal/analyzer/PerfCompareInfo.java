package com.shobal.analyzer;

import com.android.traceview.MethodData;

/**
 * Created by Administrator on 2016/8/22.
 */
public class PerfCompareInfo {
    public static long mNewTotalIncCpu;
    public static long mNewTotalIncRealCpu;
    public static long mOldTotalIncCpu;
    public static long mOldTotalIncRealCpu;
    public MethodData mNewMethodData;
    public MethodData mOldMethodData;
    public float mRiseCpu = 0;
    public float mNewIncCpuPer = 0;
    public float mOldIncCpuPer = 0;

    public PerfCompareInfo(MethodData mNewMethodData, MethodData mOldMethodData) {
        setNewMethodData(mNewMethodData);
        setOldMethodData(mOldMethodData);
    }

    public PerfCompareInfo() {
    }

    public MethodData getNewMethodData() {
        return mNewMethodData;
    }

    public void setNewMethodData(MethodData mNewMethodData) {
        if (mNewMethodData == null) {
            mNewIncCpuPer = 0;
            return;
        }
        this.mNewMethodData = mNewMethodData;
        this.mNewIncCpuPer = (float) mNewMethodData.getElapsedInclusiveCpuTime() / mNewTotalIncCpu;
    }

    public MethodData getOldMethodData() {
        return mOldMethodData;
    }

    public void setOldMethodData(MethodData mOldMethodData) {
        if (mOldMethodData == null) {
            mRiseCpu = mNewIncCpuPer;
            return;
        }
        this.mOldMethodData = mOldMethodData;
        this.mOldIncCpuPer = (float) mOldMethodData.getElapsedInclusiveCpuTime() / mOldTotalIncCpu;
        mRiseCpu = mNewIncCpuPer - mOldIncCpuPer;
    }
}
